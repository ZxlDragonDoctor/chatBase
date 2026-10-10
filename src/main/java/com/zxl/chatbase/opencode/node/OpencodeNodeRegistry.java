package com.zxl.chatbase.opencode.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpencodeNodeRegistry {
    private final ObjectMapper objectMapper;
    private final Map<Long, OpencodeNodeSession> sessions = new ConcurrentHashMap<>();

    public OpencodeNodeSession register(Long nodeId, WebSocketSession webSocketSession) {
        OpencodeNodeSession session = new OpencodeNodeSession(nodeId, webSocketSession, objectMapper);
        OpencodeNodeSession previous = sessions.put(nodeId, session);
        if (previous != null) {
            previous.failAll("本机节点已在其他连接重新上线");
            previous.close(4001, "replaced by a newer connection");
        }
        log.info("OpenCode 节点已连接: nodeId={}", nodeId);
        return session;
    }

    public void unregister(Long nodeId, OpencodeNodeSession session) {
        if (sessions.remove(nodeId, session)) {
            session.failAll("本机节点连接已断开");
            log.info("OpenCode 节点已断开: nodeId={}", nodeId);
        }
    }

    public OpencodeNodeSession getOnlineSession(Long nodeId) {
        OpencodeNodeSession session = sessions.get(nodeId);
        return session != null && session.isOpen() ? session : null;
    }

    public boolean isOnline(Long nodeId) {
        return getOnlineSession(nodeId) != null;
    }

    public void disconnect(Long nodeId, int closeCode, String reason) {
        OpencodeNodeSession session = sessions.remove(nodeId);
        if (session != null) {
            session.failAll(reason);
            session.close(closeCode, reason);
        }
    }

    public Map<Long, OpencodeNodeSession> snapshot() {
        return Map.copyOf(sessions);
    }
}
