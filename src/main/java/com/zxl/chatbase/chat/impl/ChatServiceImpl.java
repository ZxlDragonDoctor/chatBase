package com.zxl.chatbase.chat.impl;

import com.zxl.chatbase.chat.ChatService;
import com.zxl.chatbase.config.ChatProperties;
import com.zxl.chatbase.dify.model.request.DifyChatRequest;
import com.zxl.chatbase.dify.model.request.FileInfo;
import com.zxl.chatbase.dify.model.response.DifyChatResponse;
import com.zxl.chatbase.dify.server.DifyService;
import com.zxl.chatbase.im.service.GroupMessageSyncService;
import com.zxl.chatbase.kb.service.IKbConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

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
    private final GroupMessageSyncService groupMessageSyncService;
    private final IKbConversationService kbConversationService;
    private final ThreadPoolExecutor threadPool;

    private static final String CONVERSATION_KEY_PREFIX = "chat:conversation:";
    private static final String TURNS_KEY_PREFIX = "chat:turns:";

    @Override
    public DifyChatResponse chat(String channel, String userId, String groupId, String query) {
        return chat(channel, userId, groupId, query, null);
    }

    @Override
    public DifyChatResponse chat(String channel, String userId, String groupId, String query, List<FileInfo> files) {
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

        DifyChatRequest req = new DifyChatRequest();
        req.setInputs(new HashMap<>());
        req.setQuery(query);
        req.setResponseMode("blocking");
        req.setConversationId(conversationId);
        req.setUser(safeUserId);
        req.setFiles(files);

        //判断消息类型
        //一般消息要么是文本，要么是混合消息
        //前端拦截空消息
        String messageType;
        if(files!=null && !files.isEmpty()){
            messageType =  "Mixed";
        }else{
            messageType = "text";
        }

        // 保存web消息到数据库
        if(groupId==null && channel.equals("web")){
            CompletableFuture.runAsync(()
                            -> groupMessageSyncService.saveGroupMessage(userId, query, messageType, System.currentTimeMillis() / 1000),
                    threadPool);
        }

        long startTime = System.currentTimeMillis();
        DifyChatResponse response = difyService.sendChatMessage(req);
        int latencyMs = (int) (System.currentTimeMillis() - startTime);

        // 保存会话记录到 kb_conversation
        // 优先使用 conversationId，其次使用 response.getId()，最后生成 UUID
        String convIdFromResponse = response != null ? response.getConversationId() : null;
        String responseId = response != null ? response.getId() : null;
        String finalConversationId = selectConversationId(convIdFromResponse, responseId, conversationId);

        boolean success = response != null && response.getAnswer() != null && !response.getAnswer().isEmpty();
        String answer = response != null ? response.getAnswer() : null;
        Long tokens = response != null && response.getUsage() != null ? response.getUsage().getCompletionTokens().longValue() : null;
        String errorMessage = null;
        if (!success && response != null && response.getAnswer() != null) {
            errorMessage = response.getAnswer();
        }

        kbConversationService.saveConversation(
                finalConversationId,
                safeUserId,
                channel,
                groupId,
                query,
                answer,
                tokens,
                latencyMs,
                success,
                errorMessage
        );

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

    /**
     * 选择会话ID：优先使用 conversationId，其次 response.getId()，最后使用传入的 conversationId，否则生成 UUID
     */
    private String selectConversationId(String conversationId, String responseId, String inputConversationId) {
        // 有效的 UUID 格式：8-4-4-4-12
        if (isValidUuid(conversationId)) {
            return conversationId;
        }
        if (isValidUuid(responseId)) {
            return responseId;
        }
        if (isValidUuid(inputConversationId)) {
            return inputConversationId;
        }
        return java.util.UUID.randomUUID().toString();
    }

    private boolean isValidUuid(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        // 简单的 UUID 格式校验：32位字符+4个横杠
        return str.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }
}

