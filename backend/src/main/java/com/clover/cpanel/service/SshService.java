package com.clover.cpanel.service;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Shell;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;
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
        private SSHClient sshClient;
        private Session session;
        private Shell shell;
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
        public SSHClient getSshClient() { return sshClient; }
        public void setSshClient(SSHClient sshClient) { this.sshClient = sshClient; }

        public Session getSession() { return session; }
        public void setSession(Session session) { this.session = session; }

        public Shell getShell() { return shell; }
        public void setShell(Shell shell) { this.shell = shell; }
        
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

            SSHClient sshClient = new SSHClient();

            // 跳过主机密钥检查（生产环境中应该验证）
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());

            // 设置连接超时
            sshClient.setConnectTimeout(30000); // 30秒

            // 连接到服务器
            sshClient.connect(host, port);
            log.info("SSH客户端连接成功");

            // 根据认证类型进行认证
            if ("password".equals(authType) && password != null && !password.trim().isEmpty()) {
                sshClient.authPassword(username, password);
                log.info("密码认证成功");
            } else if ("publickey".equals(authType) && privateKey != null && !privateKey.trim().isEmpty()) {
                try {
                    KeyProvider keyProvider;
                    if (privateKeyPassword != null && !privateKeyPassword.trim().isEmpty()) {
                        // 使用密码保护的私钥
                        PasswordFinder passwordFinder = new PasswordFinder() {
                            @Override
                            public char[] reqPassword(Resource<?> resource) {
                                return privateKeyPassword.toCharArray();
                            }

                            @Override
                            public boolean shouldRetry(Resource<?> resource) {
                                return false;
                            }
                        };
                        keyProvider = sshClient.loadKeys(privateKey, null, passwordFinder);
                    } else {
                        // 无密码保护的私钥
                        keyProvider = sshClient.loadKeys(privateKey, null, null);
                    }
                    sshClient.authPublickey(username, keyProvider);
                    log.info("公钥认证成功");
                } catch (Exception e) {
                    log.error("公钥认证失败", e);
                    throw new RuntimeException("私钥格式错误或密码不正确: " + e.getMessage());
                }
            } else {
                throw new RuntimeException("不支持的认证类型或认证信息不完整");
            }

            // 创建会话
            Session session = sshClient.startSession();
            log.info("SSH会话创建成功");

            // 分配PTY并启动Shell
            session.allocateDefaultPTY();
            Shell shell = session.startShell();
            log.info("SSH Shell启动成功");

            // 创建连接对象
            SshConnection connection = new SshConnection(sessionId);
            connection.setSshClient(sshClient);
            connection.setSession(session);
            connection.setShell(shell);
            connection.setWriter(new PrintWriter(shell.getOutputStream(), true));
            connection.setReader(new BufferedReader(new InputStreamReader(shell.getInputStream())));
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

                // 直接返回原始输出，不做任何处理，交给前端xterm.js处理
                return rawOutput;
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

                // 关闭Shell
                if (connection.getShell() != null) {
                    try {
                        connection.getShell().close();
                    } catch (Exception e) {
                        log.warn("关闭Shell失败", e);
                    }
                }

                // 关闭会话
                if (connection.getSession() != null) {
                    try {
                        connection.getSession().close();
                    } catch (Exception e) {
                        log.warn("关闭会话失败", e);
                    }
                }

                // 关闭SSH客户端
                if (connection.getSshClient() != null && connection.getSshClient().isConnected()) {
                    try {
                        connection.getSshClient().disconnect();
                    } catch (Exception e) {
                        log.warn("断开SSH客户端失败", e);
                    }
                }

                // 关闭流
                if (connection.getWriter() != null) {
                    try {
                        connection.getWriter().close();
                    } catch (Exception e) {
                        log.warn("关闭Writer失败", e);
                    }
                }
                if (connection.getReader() != null) {
                    try {
                        connection.getReader().close();
                    } catch (Exception e) {
                        log.warn("关闭Reader失败", e);
                    }
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
               connection.getSshClient() != null && connection.getSshClient().isConnected() &&
               connection.getSession() != null && connection.getSession().isOpen();
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
