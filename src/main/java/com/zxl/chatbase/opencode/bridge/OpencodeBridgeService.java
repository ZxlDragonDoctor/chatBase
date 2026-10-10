package com.zxl.chatbase.opencode.bridge;

import com.zxl.chatbase.opencode.bridge.OpencodeBridgeSession.OpencodeBridgeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * opencode 反向桥接服务：管理本机 agent 连接，路由 HTTP 代理请求
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpencodeBridgeService {

    private final OpencodeBridgeProperties properties;

    /** bridgeId → 会话 */
    private final Map<String, OpencodeBridgeSession> sessions = new ConcurrentHashMap<>();

    /** 当前活跃桥接（同一时间只用最新一条） */
    private volatile String activeBridgeId;

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public String getTransport() {
        return properties.getTransport() == null ? "auto" : properties.getTransport().trim().toLowerCase();
    }

    public boolean isBridgeOnly() {
        return "bridge".equals(getTransport());
    }

    public boolean isDirectOnly() {
        return "direct".equals(getTransport());
    }

    /**
     * 校验桥接 token（为空或不匹配则拒绝）
     */
    public boolean authenticate(String token) {
        if (!StringUtils.hasText(properties.getToken())) {
            log.warn("opencode 桥接 token 未配置，拒绝连接");
            return false;
        }
        return properties.getToken().equals(token);
    }

    public OpencodeBridgeSession register(WebSocketSession wsSession, String name) {
        String bridgeId = "br_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        OpencodeBridgeSession session = new OpencodeBridgeSession(bridgeId, wsSession, name);
        sessions.put(bridgeId, session);
        activeBridgeId = bridgeId;
        log.info("opencode 桥接已连接: bridgeId={}, name={}, remote={}",
                bridgeId, name, wsSession.getRemoteAddress());
        return session;
    }

    public void unregister(String bridgeId) {
        OpencodeBridgeSession session = sessions.remove(bridgeId);
        if (session != null) {
            session.failAll("桥接连接已断开");
            if (bridgeId.equals(activeBridgeId)) {
                activeBridgeId = sessions.isEmpty() ? null : sessions.keySet().iterator().next();
            }
            log.info("opencode 桥接已断开: bridgeId={}", bridgeId);
        }
    }

    public OpencodeBridgeSession getActiveSession() {
        String id = activeBridgeId;
        if (id == null) {
            return null;
        }
        OpencodeBridgeSession session = sessions.get(id);
        if (session != null && session.isOpen()) {
            return session;
        }
        return null;
    }

    public boolean hasActiveBridge() {
        return getActiveSession() != null;
    }

    /**
     * 是否应走桥接：transport=bridge 强制；transport=auto 且有活跃连接时
     */
    public boolean shouldUseBridge() {
        if (isDirectOnly()) {
            return false;
        }
        if (isBridgeOnly()) {
            return true;
        }
        return hasActiveBridge();
    }

    /**
     * 通过桥接代理一次 HTTP 调用
     */
    public OpencodeBridgeResponse proxy(String method, String path, String body) throws Exception {
        OpencodeBridgeSession session = getActiveSession();
        if (session == null) {
            throw new IOException("opencode 桥接未连接（请在开发者本机运行 bridge/opencode-bridge.mjs）");
        }
        return session.call(method, path, body, properties.getRequestTimeoutSeconds());
    }

    /**
     * 处理 agent 回包
     */
    public void handleResponse(String bridgeId, String requestId, int status, String body) {
        OpencodeBridgeSession session = sessions.get(bridgeId);
        if (session != null) {
            session.complete(requestId, new OpencodeBridgeResponse(status, body));
        }
    }

    /**
     * 向 agent 发送心跳 ping
     */
    public void sendHeartbeat() {
        OpencodeBridgeSession session = getActiveSession();
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            synchronized (session.getWsSession()) {
                session.getWsSession().sendMessage(new TextMessage("{\"type\":\"ping\"}"));
            }
        } catch (IOException e) {
            log.warn("opencode 桥接心跳发送失败: bridgeId={}", session.getBridgeId(), e);
        }
    }

    public List<Map<String, Object>> listSessions() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (OpencodeBridgeSession s : sessions.values()) {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("bridgeId", s.getBridgeId());
            item.put("name", s.getName());
            item.put("connected", s.isOpen());
            item.put("connectedAt", s.getConnectedAt());
            item.put("active", s.getBridgeId().equals(activeBridgeId));
            list.add(item);
        }
        return list;
    }
}
