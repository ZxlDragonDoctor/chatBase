package com.zxl.chatbase.opencode.bridge;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 注册 opencode 反向桥接 WebSocket 端点
 *
 * 本机 agent 连接地址（生产走 Nginx/WSS）：
 *   ws://<host>:8080/opencode/bridge?token=<OPENCODE_BRIDGE_TOKEN>
 *   wss://<domain>/opencode/bridge?token=<OPENCODE_BRIDGE_TOKEN>
 */
@Configuration
@EnableWebSocket
@EnableScheduling
@RequiredArgsConstructor
public class OpencodeBridgeWebSocketConfig implements WebSocketConfigurer {

    private final OpencodeBridgeWebSocketHandler handler;
    private final OpencodeBridgeService bridgeService;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/opencode/bridge")
                .setAllowedOrigins("*");
    }

    /**
     * 定期心跳，探测死连接并触发 fail-fast
     */
    @Scheduled(fixedDelayString = "${opencode.bridge.heartbeat-seconds:30}000")
    public void heartbeat() {
        if (bridgeService.isEnabled()) {
            bridgeService.sendHeartbeat();
        }
    }
}
