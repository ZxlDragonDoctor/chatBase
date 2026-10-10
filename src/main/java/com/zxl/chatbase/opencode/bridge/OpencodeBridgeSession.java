package com.zxl.chatbase.opencode.bridge;

import lombok.Getter;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 一条已连接的本机 opencode 桥接会话
 */
@Getter
public class OpencodeBridgeSession {

    private final String bridgeId;
    private final WebSocketSession wsSession;
    private final String name;
    private final long connectedAt;

    /** 未完成的请求：requestId → 响应 Future */
    private final Map<String, CompletableFuture<OpencodeBridgeResponse>> pending = new ConcurrentHashMap<>();

    public OpencodeBridgeSession(String bridgeId, WebSocketSession wsSession, String name) {
        this.bridgeId = bridgeId;
        this.wsSession = wsSession;
        this.name = name;
        this.connectedAt = System.currentTimeMillis();
    }

    public boolean isOpen() {
        return wsSession != null && wsSession.isOpen();
    }

    /**
     * 通过桥接发起一次 HTTP 代理请求，阻塞等待本机 agent 回包
     */
    public OpencodeBridgeResponse call(String method, String path, String body, long timeoutSeconds)
            throws IOException, TimeoutException, InterruptedException {
        String requestId = java.util.UUID.randomUUID().toString();
        CompletableFuture<OpencodeBridgeResponse> future = new CompletableFuture<>();
        pending.put(requestId, future);

        String payload = String.format(
                "{\"type\":\"req\",\"id\":\"%s\",\"method\":\"%s\",\"path\":\"%s\",\"body\":%s}",
                requestId,
                escape(method),
                escape(path),
                body == null ? "null" : ("\"" + escape(body) + "\"")
        );
        try {
            synchronized (wsSession) {
                wsSession.sendMessage(new TextMessage(payload));
            }
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IOException("桥接请求执行失败: " + e.getCause().getMessage(), e);
        } catch (java.util.concurrent.TimeoutException e) {
            pending.remove(requestId);
            throw new TimeoutException("桥接请求超时（" + timeoutSeconds + "s）: " + path);
        } finally {
            pending.remove(requestId);
        }
    }

    /**
     * 由 WebSocket 消息处理器回调：完成一个待定请求
     */
    public void complete(String requestId, OpencodeBridgeResponse response) {
        CompletableFuture<OpencodeBridgeResponse> future = pending.remove(requestId);
        if (future != null) {
            future.complete(response);
        }
    }

    /**
     * 连接关闭时失败所有待定请求
     */
    public void failAll(String reason) {
        pending.values().forEach(f -> f.completeExceptionally(new IOException(reason)));
        pending.clear();
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 桥接响应载体
     */
    public static class OpencodeBridgeResponse {
        private final int status;
        private final String body;

        public OpencodeBridgeResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }

        public int getStatus() {
            return status;
        }

        public String getBody() {
            return body;
        }

        public boolean isSuccess() {
            return status >= 200 && status < 300;
        }
    }
}
