package com.zxl.chatbase.opencode.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxl.chatbase.opencode.node.dto.NodeHelloRequest;
import com.zxl.chatbase.opencode.node.dto.NodePairRequest;
import com.zxl.chatbase.opencode.node.entity.OpencodeNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpencodeNodeWebSocketHandler extends TextWebSocketHandler {
    private static final String NODE_ID = "opencodeNodeId";
    private static final String NODE_SESSION = "opencodeNodeSession";

    private final ObjectMapper objectMapper;
    private final OpencodeNodeService nodeService;
    private final OpencodeNodeRegistry registry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS).execute(() -> {
            if (session.isOpen() && !session.getAttributes().containsKey(NODE_ID)) {
                close(session, 1008, "hello or auth required within 10 seconds");
            }
        });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode frame = objectMapper.readTree(message.getPayload());
            if (frame == null || !frame.isObject()) {
                close(session, 1007, "invalid JSON frame");
                return;
            }
            if (!session.getAttributes().containsKey(NODE_ID)) {
                handleHandshake(session, frame);
                return;
            }
            handleAuthenticatedFrame(session, frame);
        } catch (IllegalArgumentException e) {
            sendErrorAndClose(session, e.getMessage());
        } catch (Exception e) {
            log.warn("OpenCode 节点协议处理失败: exceptionType={}", e.getClass().getSimpleName());
            sendErrorAndClose(session, "节点协议处理失败");
        }
    }

    private void handleHandshake(WebSocketSession session, JsonNode frame) throws Exception {
        String type = frame.path("type").asText();
        if ("hello".equals(type)) {
            com.fasterxml.jackson.databind.node.ObjectNode helloPayload =
                    ((com.fasterxml.jackson.databind.node.ObjectNode) frame).deepCopy();
            helloPayload.remove("type");
            NodeHelloRequest hello = objectMapper.treeToValue(helloPayload, NodeHelloRequest.class);
            NodePairRequest paired = nodeService.enrollNode(hello);
            OpencodeNode node = nodeService.requireNode(paired.getNodeId());
            register(session, node);
            send(session, objectMapper.createObjectNode()
                    .put("type", "paired")
                    .put("protocolVersion", "1")
                    .put("nodeId", paired.getNodeId())
                    .put("nodeToken", paired.getNodeToken())
                    .put("approval", paired.getApproval()));
            return;
        }
        if ("auth".equals(type)) {
            Long nodeId = frame.path("nodeId").isIntegralNumber() ? frame.path("nodeId").asLong() : null;
            List<String> capabilities = stringList(frame.path("capabilities"));
            OpencodeNode node;
            try {
                node = nodeService.authenticate(nodeId, frame.path("nodeToken").asText(null), capabilities,
                        frame.path("name").asText(null), frame.path("platform").asText(null),
                        frame.path("agentVersion").asText(null));
            } catch (IllegalArgumentException e) {
                sendErrorAndClose(session, "NODE_AUTH_REJECTED", e.getMessage());
                return;
            }
            register(session, node);
            com.fasterxml.jackson.databind.node.ObjectNode accepted = objectMapper.createObjectNode()
                    .put("type", "authenticated")
                    .put("protocolVersion", "1")
                    .put("nodeId", node.getId())
                    .put("approval", node.getStatus());
            accepted.set("approvedCapabilities", objectMapper.valueToTree(node.getApprovedCapabilities()));
            send(session, accepted);
            return;
        }
        close(session, 1008, "first frame must be hello or auth");
    }

    private void register(WebSocketSession webSocketSession, OpencodeNode node) {
        OpencodeNodeSession nodeSession = registry.register(node.getId(), webSocketSession);
        nodeService.markSeen(node.getId());
        webSocketSession.getAttributes().put(NODE_ID, node.getId());
        webSocketSession.getAttributes().put(NODE_SESSION, nodeSession);
    }

    private void handleAuthenticatedFrame(WebSocketSession session, JsonNode frame) throws IOException {
        Long nodeId = (Long) session.getAttributes().get(NODE_ID);
        OpencodeNodeSession nodeSession = (OpencodeNodeSession) session.getAttributes().get(NODE_SESSION);
        String type = frame.path("type").asText();
        if ("pong".equals(type) || "heartbeat".equals(type)) {
            nodeService.markSeen(nodeId);
            if ("heartbeat".equals(type)) send(session, objectMapper.createObjectNode().put("type", "pong"));
            return;
        }
        if ("result".equals(type) || "error".equals(type)) {
            nodeSession.complete(frame.path("id").asText(), frame);
            return;
        }
        if ("progress".equals(type)) {
            nodeSession.handleProgress(frame);
            return;
        }
        close(session, 1008, "unsupported node frame");
    }

    private List<String> stringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode value : node) if (value.isTextual()) values.add(value.asText());
        }
        return values;
    }

    private void sendErrorAndClose(WebSocketSession session, String reason) {
        sendErrorAndClose(session, "HANDSHAKE_REJECTED", reason);
    }

    private void sendErrorAndClose(WebSocketSession session, String errorCode, String reason) {
        try {
            send(session, objectMapper.createObjectNode()
                    .put("type", "error")
                    .put("errorCode", errorCode)
                    .put("message", reason == null ? "节点连接被拒绝" : reason));
        } catch (Exception ignored) { }
        close(session, 1008, "node handshake rejected");
    }

    private void send(WebSocketSession session, JsonNode frame) throws IOException {
        synchronized (session) {
            if (session.isOpen()) session.sendMessage(new TextMessage(objectMapper.writeValueAsString(frame)));
        }
    }

    private void close(WebSocketSession session, int code, String reason) {
        if (!session.isOpen()) return;
        try { session.close(new CloseStatus(code, reason)); }
        catch (IOException ignored) { }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long nodeId = (Long) session.getAttributes().get(NODE_ID);
        OpencodeNodeSession nodeSession = (OpencodeNodeSession) session.getAttributes().get(NODE_SESSION);
        if (nodeId != null && nodeSession != null) registry.unregister(nodeId, nodeSession);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long nodeId = (Long) session.getAttributes().get(NODE_ID);
        OpencodeNodeSession nodeSession = (OpencodeNodeSession) session.getAttributes().get(NODE_SESSION);
        if (nodeId != null && nodeSession != null) registry.unregister(nodeId, nodeSession);
        close(session, 1011, "node transport error");
    }
}
