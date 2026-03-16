package com.zxl.chatbase.chat.impl;

import com.zxl.chatbase.chat.ChatService;
import com.zxl.chatbase.config.ChatProperties;
import com.zxl.chatbase.dify.model.response.DifyChatResponse;
import com.zxl.chatbase.dify.server.DifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 使用 Redis 维护会话 ID 的统一聊天服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final DifyService difyService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ChatProperties chatProperties;

    private static final String CONVERSATION_KEY_PREFIX = "chat:conversation:";
    private static final String TURNS_KEY_PREFIX = "chat:turns:";

    @Override
    public DifyChatResponse chat(String channel, String userId, String groupId, String query) {
        // Dify 要求 user 必填，这里统一兜底
        String safeUserId = StringUtils.hasText(userId) ? userId : "abc-123";

        String sessionKey = buildSessionKey(channel, userId, groupId);
        String turnsKey = buildTurnsKey(channel, userId, groupId);

        // 从 Redis 获取历史会话 ID
        String conversationId = stringRedisTemplate.opsForValue().get(sessionKey);
        Long turns = getTurns(turnsKey);

        // 超过阈值，自动开启新会话（避免单会话过长导致 Dify 变慢/超时）
        if (turns != null && turns >= chatProperties.getMaxTurnsPerSession()) {
            log.info("会话轮数达到上限，自动重置会话: turns={}, sessionKey={}", turns, sessionKey);
            conversationId = null;
            stringRedisTemplate.delete(sessionKey);
            stringRedisTemplate.delete(turnsKey);
        }

        log.info("开始对话, channel={}, userId={}, groupId={}, sessionKey={}, conversationId={}",
                channel, safeUserId, groupId, sessionKey, conversationId);

        DifyChatResponse response = difyService.sendChatMessage(query, conversationId, safeUserId);

        // 将新的会话 ID / 轮数 回写到 Redis，便于后续连续对话
        if (response != null && StringUtils.hasText(response.getConversationId())) {
            stringRedisTemplate.opsForValue().set(sessionKey, response.getConversationId());
            stringRedisTemplate.expire(sessionKey, Duration.ofDays(chatProperties.getSessionTtlDays()));
            stringRedisTemplate.opsForValue().increment(turnsKey);
            stringRedisTemplate.expire(turnsKey, Duration.ofDays(chatProperties.getSessionTtlDays()));
            log.info("会话Id写入redis成功,time={}", LocalDateTime.now());
        }

        return response;
    }

    private String buildSessionKey(String channel, String userId, String groupId) {
        StringBuilder sb = new StringBuilder(CONVERSATION_KEY_PREFIX);
        sb.append(channel == null ? "unknown" : channel.toLowerCase());
        sb.append(":");
        sb.append(userId == null ? "anonymous" : userId);
        if (StringUtils.hasText(groupId)) {
            sb.append(":").append(groupId);
        }
        return sb.toString();
    }

    private String buildTurnsKey(String channel, String userId, String groupId) {
        StringBuilder sb = new StringBuilder(TURNS_KEY_PREFIX);
        sb.append(channel == null ? "unknown" : channel.toLowerCase());
        sb.append(":");
        sb.append(userId == null ? "anonymous" : userId);
        if (StringUtils.hasText(groupId)) {
            sb.append(":").append(groupId);
        }
        return sb.toString();
    }

    private Long getTurns(String turnsKey) {
        try {
            String val = stringRedisTemplate.opsForValue().get(turnsKey);
            if (!StringUtils.hasText(val)) {
                return 0L;
            }
            return Long.parseLong(val);
        } catch (Exception e) {
            log.warn("读取 turns 失败，默认当作 0: key={}", turnsKey, e);
            return 0L;
        }
    }
}

