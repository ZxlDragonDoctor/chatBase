package com.zxl.chatbase.opencode.node;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.io.IOException;

@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class OpencodeNodeWebSocketConfig implements WebSocketConfigurer {
    private final OpencodeNodeWebSocketHandler handler;
    private final OpencodeNodeRegistry nodeRegistry;
    private final OpencodeNodeProperties properties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/opencode/node").setAllowedOrigins("*");
    }

    @Scheduled(fixedDelayString = "${opencode.node.heartbeat-seconds:30}000")
    public void sendHeartbeats() {
        if (!properties.isEnabled()) return;
        for (OpencodeNodeSession session : nodeRegistry.snapshot().values()) {
            WebSocketSession webSocketSession = session.getWebSocketSession();
            try {
                session.sendHeartbeat();
            } catch (IOException e) {
                nodeRegistry.unregister(session.getNodeId(), session);
                try { if (webSocketSession.isOpen()) webSocketSession.close(); }
                catch (IOException ignored) { }
                log.debug("OpenCode 节点心跳发送失败: nodeId={}", session.getNodeId());
            }
        }
    }
}
