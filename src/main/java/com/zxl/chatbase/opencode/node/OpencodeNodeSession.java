package com.zxl.chatbase.opencode.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class OpencodeNodeSession {
    private final ObjectMapper objectMapper;
    private final Map<String, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
    private final Map<String, Consumer<String>> progressListeners = new ConcurrentHashMap<>();
    @Getter
    private final Long nodeId;
    @Getter
    private final WebSocketSession webSocketSession;

    public OpencodeNodeSession(Long nodeId, WebSocketSession webSocketSession, ObjectMapper objectMapper) {
        this.nodeId = nodeId;
        this.webSocketSession = webSocketSession;
        this.objectMapper = objectMapper;
    }

    public boolean isOpen() {
        return webSocketSession.isOpen();
    }

    public JsonNode invoke(String command, JsonNode params, int timeoutSeconds, Consumer<String> progressListener) {
        if (!isOpen()) throw new IllegalStateException("本机节点当前离线");
        timeoutSeconds = Math.max(1, Math.min(3600, timeoutSeconds));
        String id = UUID.randomUUID().toString();
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pending.put(id, future);
        if (progressListener != null) progressListeners.put(id, progressListener);
        ObjectNode frame = objectMapper.createObjectNode();
        frame.put("type", "invoke");
        frame.put("id", id);
        frame.put("command", command);
        frame.set("params", params == null ? objectMapper.createObjectNode() : params);
        frame.put("deadlineMs", System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds));
        try {
            send(frame);
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            sendCancel(id);
            throw new IllegalStateException("本机节点执行超时", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sendCancel(id);
            throw new IllegalStateException("等待本机节点响应时被中断", e);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            throw new IllegalStateException(cause == null ? "本机节点调用失败" : cause.getMessage(), cause);
        } catch (IOException e) {
            throw new IllegalStateException("无法发送请求到本机节点", e);
        } finally {
            pending.remove(id);
            progressListeners.remove(id);
        }
    }

    public void complete(String id, JsonNode frame) {
        CompletableFuture<JsonNode> future = pending.get(id);
        if (future == null) return;
        if ("error".equals(frame.path("type").asText()) || "error".equals(frame.path("status").asText())) {
            String code = frame.path("errorCode").asText("NODE_ERROR");
            String message = frame.path("message").asText("本机节点执行失败");
            future.completeExceptionally(new IllegalStateException(code + ": " + message));
        } else {
            future.complete(frame);
        }
    }

    public void handleProgress(JsonNode frame) {
        Consumer<String> listener = progressListeners.get(frame.path("id").asText());
        String message = frame.path("message").asText(null);
        if (listener != null && message != null && !message.isBlank()) {
            try { listener.accept(message); }
            catch (RuntimeException e) { log.debug("OpenCode 节点进度回调失败: nodeId={}", nodeId); }
        }
    }

    public void failAll(String reason) {
        pending.values().forEach(future -> future.completeExceptionally(new IllegalStateException(reason)));
        pending.clear();
        progressListeners.clear();
    }

    public void sendHeartbeat() throws IOException {
        ObjectNode frame = objectMapper.createObjectNode();
        frame.put("type", "ping");
        frame.put("sentAt", System.currentTimeMillis());
        send(frame);
    }

    public void close(int code, String reason) {
        if (!webSocketSession.isOpen()) return;
        try {
            webSocketSession.close(new CloseStatus(code, reason));
        } catch (IOException e) {
            log.debug("关闭 OpenCode 节点连接失败: nodeId={}", nodeId, e);
        }
    }

    private void sendCancel(String id) {
        ObjectNode cancel = objectMapper.createObjectNode();
        cancel.put("type", "cancel");
        cancel.put("id", id);
        try {
            send(cancel);
        } catch (IOException ignored) {
            // 连接可能已断开；pending Future 仍会在 disconnect 时完成。
        }
    }

    private void send(ObjectNode frame) throws IOException {
        synchronized (webSocketSession) {
            if (!webSocketSession.isOpen()) throw new IOException("WebSocket 已断开");
            webSocketSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(frame)));
        }
    }
}
