package com.zxl.chatbase.qq;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 注册 QQ Bot WebSocket 端点
 *
 * NapCat / go-cqhttp 可以配置反向 WebSocket，连接到：
 * ws://your-host:8080/qq/ws
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class QqWebSocketConfig implements WebSocketConfigurer {

    private final QqBotWebSocketHandler qqBotWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(qqBotWebSocketHandler, "/qq/ws")
                .setAllowedOrigins("*");
    }
}

