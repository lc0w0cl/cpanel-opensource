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

        public SshConnection(String sessionId) {
            this.sessionId = sessionId;
            this.connected = false;
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
                connection.getWriter().println(command);
                connection.getWriter().flush();
                log.debug("发送命令到会话 {}: {}", sessionId, command);
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
                
                return output.toString();
            } catch (Exception e) {
                log.error("读取输出失败", e);
                return "";
            }
        }
        return "";
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
