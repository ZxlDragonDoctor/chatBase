package com.zxl.chatbase.chat.impl;

import com.zxl.chatbase.chat.ChatService;
import com.zxl.chatbase.dify.model.response.DifyChatResponse;
import com.zxl.chatbase.dify.server.DifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    private static final String CONVERSATION_KEY_PREFIX = "chat:conversation:";

    @Override
    public DifyChatResponse chat(String channel, String userId, String groupId, String query) {
        String sessionKey = buildSessionKey(channel, userId, groupId);

        // 从 Redis 获取历史会话 ID
        String conversationId = stringRedisTemplate.opsForValue().get(sessionKey);

        log.info("开始对话, channel={}, userId={}, groupId={}, sessionKey={}, conversationId={}",
                channel, userId, groupId, sessionKey, conversationId);

        DifyChatResponse response = difyService.sendChatMessage(query, conversationId, userId);

        // 将新的会话 ID 回写到 Redis，便于后续连续对话
        if (response != null && StringUtils.hasText(response.getConversationId())) {
            stringRedisTemplate.opsForValue().set(sessionKey, response.getConversationId());
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
}

