package com.zxl.chatbase.qq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxl.chatbase.chat.ChatService;
import com.zxl.chatbase.config.ChatProperties;
import com.zxl.chatbase.im.entity.GroupMessage;
import com.zxl.chatbase.im.mapper.GroupMessageMapper;
import com.zxl.chatbase.dify.model.response.DifyChatResponse;
import com.zxl.chatbase.im.service.GroupMessageSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.HashMap;
import java.util.Map;

/**
 * QQ / OneBot WebSocket 事件处理
 *
 * 适配 NapCat / go-cqhttp 的 OneBot v11 事件：
 * - 收到群消息时，如果 @ 了机器人，则调用 ChatService，并通过 OneBot 的 send_group_msg 动作回复
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QqBotWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final QqBotProperties qqBotProperties;
    private final GroupMessageMapper groupMessageMapper;
    private final GroupMessageSyncService groupMessageSyncService;
    private final RestTemplate restTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ChatProperties chatProperties;
    @Qualifier("threadPool")
    private final ThreadPoolExecutor threadPool;

    private static final String RATE_KEY_PREFIX = "chat:rate:im:";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("QQ Bot WebSocket 已连接, id={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("收到 QQ Bot 消息: {}", payload);

        JsonNode root = objectMapper.readTree(payload);
        String postType = root.path("post_type").asText();

        // 只处理消息事件
        if (!"message".equals(postType)) {
            return;
        }

        String messageType = root.path("message_type").asText();
        if (!"group".equals(messageType)) {
            return;
        }

        String messageId = root.path("message_id").asText();
        long time = root.path("time").asLong(0L);
        String groupId = root.path("group_id").asText();
        String userId = root.path("user_id").asText();
        String rawMessage = root.path("raw_message").asText(root.path("message").asText(""));

        // 1. 无论是否 @ 机器人，先采集消息到数据库（异步写入，避免阻塞消息处理）
        CompletableFuture.runAsync(
                () -> groupMessageSyncService.saveGroupMessage(messageId, groupId, userId, rawMessage, messageType, time),
                threadPool
        );

        // 2. 仅当 @ 机器人时才触发问答
        if (!isAtSelf(rawMessage)) {
            return;
        }

        String query = stripAt(rawMessage);
        if (!StringUtils.hasText(query)) {
            return;
        }

        // 3. 限流：避免频繁 @ 机器人打爆 Dify
        if (isRateLimited(groupId, userId)) {
            sendGroupMessage(session, groupId, "请求太频繁，请稍后再试");
            return;
        }

        log.info("处理群聊消息: groupId={}, userId={}, query={}", groupId, userId, query);
        
        // 4. 异步回答，避免阻塞 WebSocket 消息线程
        CompletableFuture
                .supplyAsync(() -> chatService.chat(
                        "im",
                        String.valueOf(userId),
                        String.valueOf(groupId),
                        query
                ), threadPool)
                .orTimeout(160, TimeUnit.SECONDS)
                .exceptionally(e -> {
                    log.error("聊天任务执行失败，groupId={}, userId={}", groupId, userId, e);
                    DifyChatResponse fallback = new DifyChatResponse();
                    fallback.setAnswer("【系统繁忙】回答超时，请稍后再试");
                    return fallback;
                })
                .thenAccept(resp -> {
                    String answer = resp != null ? resp.getAnswer() : "【系统错误】暂时无法回答，请稍后再试";
                    sendGroupMessage(session, groupId, answer);
                });
    }


    /**
     * 向 OneBot 发送群消息
     *
     * 部分实现（例如 NapCat 的 OneBot 适配）对 action 名比较严格，
     * 这里采用兼容性更好的通用接口 send_msg，显式指定 message_type=group。
     */
    private void sendGroupMessage(WebSocketSession session, String groupId, String text) {
        try {
            String baseUrl = qqBotProperties.getHttpBaseUrl();
            if (!StringUtils.hasText(baseUrl)) {
                log.warn("未配置 qq.bot.http-base-url，无法通过 HTTP 向群发送消息");
                return;
            }
            String url = baseUrl.endsWith("/") ? baseUrl + "send_msg" : baseUrl + "/send_msg";
    
            Map<String, Object> body = new HashMap<>();
            body.put("message_type", "group");
            body.put("group_id", groupId);
            body.put("message", text);
    
            log.info("调用 OneBot HTTP 接口发送群消息, url={}, body={}", url, body);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) restTemplate.postForObject(url, body, Map.class);
            if (response != null && "failed".equals(String.valueOf(response.get("status")))) {
                // 简单重试一次
                log.warn("OneBot send_msg 失败，准备重试一次: {}", response);
                @SuppressWarnings("unchecked")
                Map<String, Object> retryResponse = (Map<String, Object>) restTemplate.postForObject(url, body, Map.class);
                response = retryResponse;
            }
            log.info("OneBot HTTP send_msg 响应: {}", response);
        } catch (Exception e) {
            log.error("发送群消息失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("QQ Bot WebSocket 已关闭, id={}, status={}", session.getId(), status);
    }

    private boolean isAtSelf(String msg) {
        if (!StringUtils.hasText(msg)) {
            return false;
        }
        // OneBot CQ 码格式: [CQ:at,qq=123456]
        String cqAt = "[CQ:at,qq=" + qqBotProperties.getSelfId() + "]";
        return msg.contains(cqAt);
    }

    private String stripAt(String msg) {
        if (!StringUtils.hasText(msg)) {
            return msg;
        }
        String cqAt = "[CQ:at,qq=" + qqBotProperties.getSelfId() + "]";
        String result = msg.replace(cqAt, "").trim();
        // 兼容直接 @昵称 文本
        return result.replaceAll("^@\\S+\\s*", "").trim();
    }

    private boolean isRateLimited(String groupId, String userId) {
        ChatProperties.RateLimit rl = chatProperties.getRateLimit();
        long windowSeconds = Math.max(1, rl.getWindowSeconds());
        long maxRequests = Math.max(1, rl.getMaxRequests());

        String key = RATE_KEY_PREFIX + groupId + ":" + userId;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }
        return count != null && count > maxRequests;
    }
}

