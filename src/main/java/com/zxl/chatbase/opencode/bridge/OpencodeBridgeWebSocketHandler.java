package com.zxl.chatbase.opencode.bridge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * opencode 反向桥接 WebSocket 端点
 *
 * 本机 agent 主动外连 {@code /opencode/bridge?token=xxx}，
 * 连接建立后服务器可反向下发 HTTP 代理请求，无需 frp / 内网穿透。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpencodeBridgeWebSocketHandler extends TextWebSocketHandler {

    private final OpencodeBridgeService bridgeService;
    private final ObjectMapper objectMapper;

    /** wsSession.id → bridgeId */
    private final Map<String, String> sessionToBridge = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session);
        if (!bridgeService.authenticate(token)) {
            log.warn("opencode 桥接鉴权失败，关闭连接: remote={}", session.getRemoteAddress());
            session.close(CloseStatus.POLICY_VIOLATION.withReason("invalid token"));
            return;
        }
        // 等待 hello 消息完成注册；先占位以便鉴权后能识别
        sessionToBridge.put(session.getId(), "pending:" + session.getId());
        // 立即回一个 challenge-ok，让 agent 知道 token 通过
        session.sendMessage(new TextMessage("{\"type\":\"auth-ok\"}"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception e) {
            log.warn("opencode 桥接收到非法 JSON: {}", payload);
            return;
        }

        String type = root.path("type").asText("");
        switch (type) {
            case "hello": {
                String name = root.path("name").asText("unnamed");
                OpencodeBridgeSession bridge = bridgeService.register(session, name);
                sessionToBridge.put(session.getId(), bridge.getBridgeId());
                String reply = "{\"type\":\"hello-ok\",\"bridgeId\":\"" + bridge.getBridgeId() + "\"}";
                session.sendMessage(new TextMessage(reply));
                break;
            }
            case "res": {
                String bridgeId = sessionToBridge.get(session.getId());
                if (bridgeId == null || bridgeId.startsWith("pending:")) {
                    return;
                }
                String requestId = root.path("id").asText(null);
                int status = root.path("status").asInt(500);
                String body = root.path("body").isTextual() ? root.path("body").asText() : root.toString();
                bridgeService.handleResponse(bridgeId, requestId, status, body);
                break;
            }
            case "pong":
                // 心跳回包，忽略
                break;
            default:
                log.debug("opencode 桥接忽略消息类型: {}", type);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String bridgeId = sessionToBridge.remove(session.getId());
        if (bridgeId != null && !bridgeId.startsWith("pending:")) {
            bridgeService.unregister(bridgeId);
        }
        log.info("opencode 桥接连接关闭: sessionId={}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("opencode 桥接传输错误: sessionId={}", session.getId(), exception);
    }

    private String extractToken(WebSocketSession session) {
        // 1) query 参数 ?token=
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null) {
            for (String pair : query.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0 && "token".equals(pair.substring(0, eq))) {
                    return java.net.URLDecoder.decode(pair.substring(eq + 1), java.nio.charset.StandardCharsets.UTF_8);
                }
            }
        }
        // 2) Authorization: Bearer xxx
        String auth = session.getHandshakeHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }
}
