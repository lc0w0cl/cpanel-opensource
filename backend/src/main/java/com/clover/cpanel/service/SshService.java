package com.clover.cpanel.service;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSH连接服务
 */
@Slf4j
@Service
public class SshService {

    // 存储活跃的SSH会话
    private final Map<String, SshConnection> activeSessions = new ConcurrentHashMap<>();

    /**
     * SSH连接信息
     */
    public static class SshConnection {
        private Session session;
        private ChannelShell channel;
        private PrintWriter writer;
        private BufferedReader reader;
        private String sessionId;
        private boolean connected;
        private String lastCommand;
        private long lastCommandTime;
        private boolean isInitialConnection;

        public SshConnection(String sessionId) {
            this.sessionId = sessionId;
            this.connected = false;
            this.lastCommand = null;
            this.lastCommandTime = 0;
            this.isInitialConnection = true;
        }

        // Getters and Setters
        public Session getSession() { return session; }
        public void setSession(Session session) { this.session = session; }
        
        public ChannelShell getChannel() { return channel; }
        public void setChannel(ChannelShell channel) { this.channel = channel; }
        
        public PrintWriter getWriter() { return writer; }
        public void setWriter(PrintWriter writer) { this.writer = writer; }
        
        public BufferedReader getReader() { return reader; }
        public void setReader(BufferedReader reader) { this.reader = reader; }
        
        public String getSessionId() { return sessionId; }

        public boolean isConnected() { return connected; }
        public void setConnected(boolean connected) { this.connected = connected; }

        public String getLastCommand() { return lastCommand; }
        public void setLastCommand(String lastCommand) { this.lastCommand = lastCommand; }

        public long getLastCommandTime() { return lastCommandTime; }
        public void setLastCommandTime(long lastCommandTime) { this.lastCommandTime = lastCommandTime; }

        public boolean isInitialConnection() { return isInitialConnection; }
        public void setInitialConnection(boolean initialConnection) { this.isInitialConnection = initialConnection; }
    }

    /**
     * 创建SSH连接
     */
    public SshConnection createConnection(String sessionId, String host, int port, String username, 
                                        String authType, String password, String privateKey, String privateKeyPassword) {
        try {
            log.info("创建SSH连接: {}@{}:{}", username, host, port);
            
            JSch jsch = new JSch();
            
            // 如果是公钥认证，添加私钥
            if ("publickey".equals(authType) && privateKey != null && !privateKey.trim().isEmpty()) {
                try {
                    if (privateKeyPassword != null && !privateKeyPassword.trim().isEmpty()) {
                        jsch.addIdentity("key", privateKey.getBytes(), null, privateKeyPassword.getBytes());
                    } else {
                        jsch.addIdentity("key", privateKey.getBytes(), null, null);
                    }
                    log.info("已添加私钥认证");
                } catch (Exception e) {
                    log.error("添加私钥失败", e);
                    throw new RuntimeException("私钥格式错误或密码不正确");
                }
            }

            // 创建会话
            Session session = jsch.getSession(username, host, port);
            
            // 如果是密码认证，设置密码
            if ("password".equals(authType) && password != null && !password.trim().isEmpty()) {
                session.setPassword(password);
            }

            // 跳过主机密钥检查（生产环境中应该验证）
            session.setConfig("StrictHostKeyChecking", "no");
            
            // 设置连接超时
            session.setTimeout(30000); // 30秒
            
            // 连接
            session.connect();
            log.info("SSH会话连接成功");

            // 创建Shell通道
            ChannelShell channel = (ChannelShell) session.openChannel("shell");
            
            // 设置终端类型
            channel.setPtyType("xterm");
            channel.setPtySize(80, 24, 640, 480);
            
            // 设置输入输出流
            PipedInputStream in = new PipedInputStream();
            PipedOutputStream out = new PipedOutputStream(in);
            channel.setInputStream(in);
            
            PipedInputStream shellIn = new PipedInputStream();
            PipedOutputStream shellOut = new PipedOutputStream(shellIn);
            channel.setOutputStream(shellOut);
            channel.setExtOutputStream(shellOut);

            // 连接通道
            channel.connect();
            log.info("SSH Shell通道连接成功");

            // 创建连接对象
            SshConnection connection = new SshConnection(sessionId);
            connection.setSession(session);
            connection.setChannel(channel);
            connection.setWriter(new PrintWriter(out, true));
            connection.setReader(new BufferedReader(new InputStreamReader(shellIn)));
            connection.setConnected(true);

            // 存储连接
            activeSessions.put(sessionId, connection);
            
            log.info("SSH连接创建完成，会话ID: {}", sessionId);
            return connection;

        } catch (Exception e) {
            log.error("创建SSH连接失败: {}@{}:{}", username, host, port, e);
            throw new RuntimeException("SSH连接失败: " + e.getMessage());
        }
    }

    /**
     * 发送命令到SSH会话
     */
    public void sendCommand(String sessionId, String command) {
        SshConnection connection = activeSessions.get(sessionId);
        if (connection != null && connection.isConnected()) {
            try {
                // 所有输入都直接发送给SSH会话，不做任何处理
                // 这样可以确保服务器完全控制终端的行为
                connection.getWriter().print(command);
                connection.getWriter().flush();

                // 记录发送的内容用于调试
                if (command.length() == 1) {
                    char c = command.charAt(0);
                    if (c == '\r') {
                        log.info("发送回车到会话 {}", sessionId);
                    } else if (c == '\t') {
                        log.info("发送Tab到会话 {}", sessionId);
                    } else if (c == '\u007F') {
                        log.info("发送退格到会话 {}", sessionId);
                    } else if (c == '\u0003') {
                        log.info("发送Ctrl+C到会话 {}", sessionId);
                    } else if (c >= 32 && c <= 126) {
                        log.debug("发送字符到会话 {}: '{}'", sessionId, c);
                    } else {
                        log.info("发送控制字符到会话 {}, ASCII码: {}", sessionId, (int)c);
                    }
                } else {
                    log.debug("发送字符串到会话 {}, 长度: {}", sessionId, command.length());
                }

                // 标记不再是初始连接
                connection.setInitialConnection(false);

            } catch (Exception e) {
                log.error("发送命令失败", e);
                throw new RuntimeException("发送命令失败: " + e.getMessage());
            }
        } else {
            throw new RuntimeException("SSH会话不存在或已断开");
        }
    }

    /**
     * 读取SSH会话输出
     */
    public String readOutput(String sessionId) {
        SshConnection connection = activeSessions.get(sessionId);
        if (connection != null && connection.isConnected()) {
            try {
                StringBuilder output = new StringBuilder();
                BufferedReader reader = connection.getReader();

                // 非阻塞读取
                while (reader.ready()) {
                    char[] buffer = new char[1024];
                    int length = reader.read(buffer);
                    if (length > 0) {
                        output.append(buffer, 0, length);
                    }
                }

                String rawOutput = output.toString();

                // 过滤掉命令回显
                return filterCommandEcho(connection, rawOutput);
            } catch (Exception e) {
                log.error("读取输出失败", e);
                return "";
            }
        }
        return "";
    }

    /**
     * 过滤掉命令回显和多余的提示符
     */
    private String filterCommandEcho(SshConnection connection, String output) {
        if (output.isEmpty()) {
            return output;
        }

        // 如果是初始连接，不进行任何过滤，保留MOTD和初始提示符
        if (connection.isInitialConnection()) {
            log.debug("初始连接状态，不进行过滤，输出长度: {}", output.length());
            return output;
        }

        // 现在前端发送的是单个字符，不再需要复杂的命令回显过滤
        // 只需要简单的重复提示符过滤
        return filterDuplicatePrompts(output);
    }

    /**
     * 过滤重复的提示符
     */
    private String filterDuplicatePrompts(String output) {
        if (output == null || output.isEmpty()) {
            return output;
        }

        // 简单的重复提示符过滤
        // 如果输出只包含提示符且没有其他内容，可能是重复的
        String trimmed = output.trim();

        // 检查是否是单独的提示符行
        if (trimmed.matches(".*@.*[:~].*[$#]\\s*") && trimmed.length() < 100) {
            log.debug("检测到可能的重复提示符，长度: {}, 内容: {}", trimmed.length(), trimmed);
            // 暂时保留，让前端处理
        }

        return output;
    }

    /**
     * 过滤掉末尾的提示符
     */
    private String filterTrailingPrompt(String output) {
        if (output.isEmpty()) {
            return output;
        }

        String filtered = output;

        // 多种提示符模式，按优先级尝试
        String[] promptPatterns = {
            // 模式1: \r\n\ruser@host:path# \r\n\ruser@host:path# (重复的提示符)
            "\\r\\n\\r[^\\r\\n]*[@][^\\r\\n]*[#$][^\\r\\n]*\\r\\n\\r[^\\r\\n]*[@][^\\r\\n]*[#$][^\\r\\n]*$",
            // 模式2: \r\nuser@host:path# \r\n\ruser@host:path#
            "\\r\\n[^\\r\\n]*[@][^\\r\\n]*[#$][^\\r\\n]*\\r\\n\\r[^\\r\\n]*[@][^\\r\\n]*[#$][^\\r\\n]*$",
            // 模式3: \r\n\ruser@host:path#
            "\\r\\n\\r[^\\r\\n]*[@][^\\r\\n]*[#$][^\\r\\n]*$",
            // 模式4: \r\nuser@host:path#
            "\\r\\n[^\\r\\n]*[@][^\\r\\n]*[#$][^\\r\\n]*$"
        };

        // 尝试每个模式
        for (String pattern : promptPatterns) {
            String temp = filtered.replaceAll(pattern, "");
            if (!temp.equals(filtered)) {
                log.debug("使用模式过滤提示符: {}", pattern);
                filtered = temp;
                break;
            }
        }

        // 如果正则表达式没有匹配，使用字符串查找方式
        if (filtered.equals(output)) {
            filtered = filterPromptByStringSearch(output);
        }

        return filtered;
    }

    /**
     * 通过字符串搜索方式过滤提示符
     */
    private String filterPromptByStringSearch(String output) {
        // 查找最后一个包含 @ 和 # 或 $ 的行，这很可能是提示符
        String[] lines = output.split("\\r\\n");
        if (lines.length < 2) {
            return output;
        }

        // 从后往前查找提示符
        int promptLineIndex = -1;
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i];
            // 移除开头的 \r 字符
            if (line.startsWith("\r")) {
                line = line.substring(1);
            }

            // 检查是否是提示符格式：包含 @ 和 # 或 $
            if (line.contains("@") && (line.contains("#") || line.contains("$"))) {
                // 进一步验证：提示符通常以 # 或 $ 结尾，后面可能有空格
                if (line.trim().endsWith("#") || line.trim().endsWith("$")) {
                    promptLineIndex = i;
                    break;
                }
            }
        }

        // 如果找到了提示符行，移除它及其后面的所有内容
        if (promptLineIndex >= 0) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < promptLineIndex; i++) {
                if (i > 0) {
                    result.append("\r\n");
                }
                result.append(lines[i]);
            }

            String filtered = result.toString();
            log.debug("通过字符串搜索过滤掉提示符，从第{}行开始", promptLineIndex);
            return filtered;
        }

        return output;
    }

    /**
     * 断开SSH连接
     */
    public void disconnect(String sessionId) {
        SshConnection connection = activeSessions.get(sessionId);
        if (connection != null) {
            try {
                connection.setConnected(false);
                
                if (connection.getChannel() != null && connection.getChannel().isConnected()) {
                    connection.getChannel().disconnect();
                }
                
                if (connection.getSession() != null && connection.getSession().isConnected()) {
                    connection.getSession().disconnect();
                }
                
                // 关闭流
                if (connection.getWriter() != null) {
                    connection.getWriter().close();
                }
                if (connection.getReader() != null) {
                    connection.getReader().close();
                }
                
                activeSessions.remove(sessionId);
                log.info("SSH连接已断开，会话ID: {}", sessionId);
                
            } catch (Exception e) {
                log.error("断开SSH连接失败", e);
            }
        }
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected(String sessionId) {
        SshConnection connection = activeSessions.get(sessionId);
        return connection != null && connection.isConnected() && 
               connection.getSession() != null && connection.getSession().isConnected() &&
               connection.getChannel() != null && connection.getChannel().isConnected();
    }

    /**
     * 获取活跃会话数量
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * 清理所有连接
     */
    public void disconnectAll() {
        log.info("断开所有SSH连接，当前活跃会话数: {}", activeSessions.size());
        activeSessions.keySet().forEach(this::disconnect);
    }
}
