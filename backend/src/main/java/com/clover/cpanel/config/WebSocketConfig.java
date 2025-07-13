package com.clover.cpanel.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.clover.cpanel.websocket.TerminalWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * WebSocket配置
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private TerminalWebSocketHandler terminalWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册终端WebSocket处理器，支持会话ID路径参数
        registry.addHandler(terminalWebSocketHandler, "/ws/terminal", "/ws/terminal/{sessionId}")
                .setAllowedOrigins("*"); // 生产环境中应该限制允许的源
    }
}
