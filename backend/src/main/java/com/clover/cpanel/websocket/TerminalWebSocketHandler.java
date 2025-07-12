package com.clover.cpanel.websocket;

import com.clover.cpanel.service.SshService;
import com.clover.cpanel.util.AnsiProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 终端WebSocket处理器
 */
@Slf4j
@Component
public class TerminalWebSocketHandler implements WebSocketHandler {

    @Autowired
    private SshService sshService;

    @Autowired
    private AnsiProcessor ansiProcessor;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        log.info("WebSocket连接建立: {}", sessionId);
        
        // 发送连接成功消息
        sendMessage(session, "connected", "WebSocket连接已建立");
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload().toString();

        log.debug("收到WebSocket消息: {}", payload);

        try {
            JsonNode jsonNode = objectMapper.readTree(payload);

            if (!jsonNode.has("type") || jsonNode.get("type").isNull()) {
                sendMessage(session, "error", "消息缺少type字段");
                return;
            }

            String type = jsonNode.get("type").asText();
            JsonNode data = jsonNode.get("data");

            switch (type) {
                case "connect":
                    handleConnect(session, data);
                    break;
                case "command":
                    handleCommand(session, data);
                    break;
                case "disconnect":
                    handleDisconnect(session);
                    break;
                case "resize":
                    handleResize(session, data);
                    break;
                default:
                    log.warn("未知消息类型: {}", type);
                    sendMessage(session, "error", "未知消息类型: " + type);
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败: {}", payload, e);
            sendMessage(session, "error", "处理消息失败: " + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: {}", session.getId(), exception);
        cleanupSession(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = session.getId();
        log.info("WebSocket连接关闭: {}, 状态: {}", sessionId, closeStatus);
        cleanupSession(sessionId);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 处理SSH连接请求
     */
    private void handleConnect(WebSocketSession session, JsonNode data) {
        String sessionId = session.getId();

        try {
            // 验证必需字段
            if (!data.has("host") || data.get("host").isNull()) {
                sendMessage(session, "error", "缺少主机地址");
                return;
            }
            if (!data.has("username") || data.get("username").isNull()) {
                sendMessage(session, "error", "缺少用户名");
                return;
            }
            if (!data.has("authType") || data.get("authType").isNull()) {
                sendMessage(session, "error", "缺少认证类型");
                return;
            }

            String host = data.get("host").asText();
            int port = data.has("port") && !data.get("port").isNull() ? data.get("port").asInt() : 22;
            String username = data.get("username").asText();
            String authType = data.get("authType").asText();
            String password = data.has("password") && !data.get("password").isNull() ? data.get("password").asText() : null;
            String privateKey = data.has("privateKey") && !data.get("privateKey").isNull() ? data.get("privateKey").asText() : null;
            String privateKeyPassword = data.has("privateKeyPassword") && !data.get("privateKeyPassword").isNull() ? data.get("privateKeyPassword").asText() : null;

            // 验证必需的认证信息
            if ("password".equals(authType) && (password == null || password.trim().isEmpty())) {
                sendMessage(session, "error", "密码认证时密码不能为空");
                return;
            }
            if ("publickey".equals(authType) && (privateKey == null || privateKey.trim().isEmpty())) {
                sendMessage(session, "error", "公钥认证时私钥不能为空");
                return;
            }

            log.info("尝试SSH连接: {}@{}:{}", username, host, port);
            
            // 创建SSH连接
            SshService.SshConnection sshConnection = sshService.createConnection(
                sessionId, host, port, username, authType, password, privateKey, privateKeyPassword
            );

            if (sshConnection.isConnected()) {
                sendMessage(session, "connected", "SSH连接成功");
                
                // 启动输出读取任务
                startOutputReader(session, sessionId);
            } else {
                sendMessage(session, "error", "SSH连接失败");
            }

        } catch (Exception e) {
            log.error("SSH连接失败", e);
            sendMessage(session, "error", "SSH连接失败: " + e.getMessage());
        }
    }

    /**
     * 处理命令发送
     */
    private void handleCommand(WebSocketSession session, JsonNode data) {
        String sessionId = session.getId();
        String command = data.get("command").asText();
        
        try {
            sshService.sendCommand(sessionId, command);
            log.debug("发送命令到SSH会话 {}: {}", sessionId, command);
        } catch (Exception e) {
            log.error("发送命令失败", e);
            sendMessage(session, "error", "发送命令失败: " + e.getMessage());
        }
    }

    /**
     * 处理断开连接
     */
    private void handleDisconnect(WebSocketSession session) {
        String sessionId = session.getId();
        
        try {
            sshService.disconnect(sessionId);
            sendMessage(session, "disconnected", "SSH连接已断开");
            log.info("SSH连接已断开: {}", sessionId);
        } catch (Exception e) {
            log.error("断开SSH连接失败", e);
        }
    }

    /**
     * 处理终端大小调整
     */
    private void handleResize(WebSocketSession session, JsonNode data) {
        String sessionId = session.getId();
        int cols = data.get("cols").asInt();
        int rows = data.get("rows").asInt();
        
        // TODO: 实现终端大小调整
        log.debug("终端大小调整: {}x{}", cols, rows);
    }

    /**
     * 启动输出读取任务
     */
    private void startOutputReader(WebSocketSession session, String sessionId) {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                if (!sshService.isConnected(sessionId) || !session.isOpen()) {
                    return;
                }

                String output = sshService.readOutput(sessionId);
                if (!output.isEmpty()) {
                    // 处理ANSI转义序列
                    String processedOutput = processTerminalOutput(output);
                    // 只发送非空的处理后输出
                    if (!processedOutput.isEmpty()) {
                        sendMessage(session, "output", processedOutput);
                    }
                }
            } catch (Exception e) {
                log.error("读取SSH输出失败", e);
                try {
                    sendMessage(session, "error", "读取输出失败: " + e.getMessage());
                } catch (Exception ex) {
                    log.error("发送错误消息失败", ex);
                }
            }
        }, 100, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * 处理终端输出，转换ANSI转义序列为前端可识别的格式
     */
    private String processTerminalOutput(String output) {
        try {
            // 检查是否包含ANSI转义序列
            if (ansiProcessor.containsAnsi(output)) {
                log.debug("检测到ANSI转义序列，进行处理");

                // 选择处理方式：保留颜色信息转换为HTML格式
                String processedOutput = ansiProcessor.ansiToHtml(output);

                // 如果转换后的输出为空或只包含空白字符，可能是纯控制序列
                String textOnly = ansiProcessor.stripAnsi(output);
                if (textOnly.trim().isEmpty() && !output.trim().isEmpty()) {
                    log.debug("输出主要包含控制序列，跳过发送");
                    return ""; // 返回空字符串，调用方会跳过发送
                }

                log.debug("原始输出长度: {}, 处理后长度: {}", output.length(), processedOutput.length());
                return processedOutput;
            }

            return output;
        } catch (Exception e) {
            log.warn("处理终端输出失败: {}", e.getMessage());
            // 失败时返回纯文本
            return ansiProcessor.stripAnsi(output);
        }
    }

    /**
     * 发送消息到WebSocket客户端
     */
    private void sendMessage(WebSocketSession session, String type, String data) {
        try {
            if (session.isOpen()) {
                // 使用Jackson来正确序列化JSON，避免手动字符串拼接
                Map<String, String> messageMap = new HashMap<>();
                messageMap.put("type", type);
                messageMap.put("data", data);

                String message = objectMapper.writeValueAsString(messageMap);
                session.sendMessage(new TextMessage(message));

                log.debug("发送WebSocket消息: {}", message);
            }
        } catch (Exception e) {
            log.error("发送WebSocket消息失败", e);
        }
    }

    /**
     * 清理会话
     */
    private void cleanupSession(String sessionId) {
        sessions.remove(sessionId);
        sshService.disconnect(sessionId);
    }
}
