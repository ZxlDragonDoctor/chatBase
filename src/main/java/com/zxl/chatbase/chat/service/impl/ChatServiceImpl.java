package com.zxl.chatbase.chat.service.impl;

import com.zxl.chatbase.chat.entity.ChatSession;
import com.zxl.chatbase.chat.service.ChatService;
import com.zxl.chatbase.chat.service.ChatSessionService;
import com.zxl.chatbase.config.ChatProperties;
import com.zxl.chatbase.dify.model.request.DifyChatRequest;
import com.zxl.chatbase.dify.model.request.FileInfo;
import com.zxl.chatbase.dify.model.response.DifyChatResponse;
import com.zxl.chatbase.dify.server.DifyService;
import com.zxl.chatbase.im.service.GroupMessageSyncService;
import com.zxl.chatbase.kb.entity.KbConversation;
import com.zxl.chatbase.kb.entity.KbFaq;
import com.zxl.chatbase.kb.entity.KbApp;
import com.zxl.chatbase.kb.service.IKbConversationService;
import com.zxl.chatbase.kb.service.IKbFaqService;
import com.zxl.chatbase.kb.service.IKbAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final DifyService difyService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ChatProperties chatProperties;
    private final GroupMessageSyncService groupMessageSyncService;
    private final IKbConversationService kbConversationService;
    private final ChatSessionService chatSessionService;
    private final IKbFaqService faqService;
    private final IKbAppService appService;
    private final ThreadPoolExecutor threadPool;

    private static final String CONVERSATION_KEY_PREFIX = "chat:conversation:";
    private static final String TURNS_KEY_PREFIX = "chat:turns:";

    @Override
    public DifyChatResponse chat(String channel, String userId, String groupId, String query) {
        return chat(null, channel, userId, groupId, query, null);
    }

    @Override
    public DifyChatResponse chat(String channel, String userId, String groupId, String query, List<FileInfo> files) {
        return chat(null, channel, userId, groupId, query, files);
    }

    @Override
    public DifyChatResponse chat(Long appId, String channel, String userId, String groupId, String query) {
        return chat(appId, channel, userId, groupId, query, null);
    }

    @Override
    public DifyChatResponse chat(Long appId, String channel, String userId, String groupId, String query, List<FileInfo> files) {
        String safeUserId = StringUtils.hasText(userId) ? userId : "abc-123";
        
        String apiKey = null;
        KbApp app = null;
        if (appId != null) {
            app = appService.getById(appId);
            if (app != null && app.getStatus()) {
                apiKey = app.getDifyApiKey();
            }
        }
        if (apiKey == null) {
            app = appService.getDefaultApp();
            if (app != null) {
                apiKey = app.getDifyApiKey();
                appId = app.getId();
            }
        }
        
        try {
            KbFaq faq = faqService.findSimilar(query);
            if (faq != null && faq.getStatus() && faq.getAnswer() != null) {
                log.info("从FAQ匹配到答案: question={}, faqId={}", query, faq.getId());
                
                DifyChatResponse faqResponse = new DifyChatResponse();
                faqResponse.setAnswer(faq.getAnswer());
                faqResponse.setId(java.util.UUID.randomUUID().toString());
                faqResponse.setConversationId(null);
                faqResponse.setCreatedAt(System.currentTimeMillis());
                
                return faqResponse;
            }
        } catch (Exception e) {
            log.warn("FAQ匹配失败: {}", e.getMessage());
        }

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

        String messageType;
        if(files!=null && !files.isEmpty()){
            messageType =  "Mixed";
        }else{
            messageType = "text";
        }

        long startTime = System.currentTimeMillis();
        DifyChatResponse response = apiKey != null 
            ? difyService.sendChatMessage(req, apiKey) 
            : difyService.sendChatMessage(req);
        int latencyMs = (int) (System.currentTimeMillis() - startTime);

        String convIdFromResponse = response != null ? response.getConversationId() : null;
        String responseId = response != null ? response.getId() : null;
        String finalConversationId = selectConversationId(convIdFromResponse, responseId, conversationId);

        boolean success = response != null && response.getAnswer() != null && !response.getAnswer().isEmpty();
        String answer = response != null ? response.getAnswer() : null;
        
        Integer promptTokens = null;
        Integer completionTokens = null;
        BigDecimal promptPrice = null;
        BigDecimal completionPrice = null;
        BigDecimal totalPrice = null;
        
        if (response != null && response.getUsage() != null) {
            promptTokens = response.getUsage().getPromptTokens();
            completionTokens = response.getUsage().getCompletionTokens();
            try {
                if (response.getUsage().getPromptPrice() != null) {
                    promptPrice = new BigDecimal(response.getUsage().getPromptPrice());
                }
                if (response.getUsage().getCompletionPrice() != null) {
                    completionPrice = new BigDecimal(response.getUsage().getCompletionPrice());
                }
                if (response.getUsage().getTotalPrice() != null) {
                    totalPrice = new BigDecimal(response.getUsage().getTotalPrice());
                }
            } catch (NumberFormatException e) {
                log.warn("解析价格失败: {}", e.getMessage());
            }
        }
        
        log.info("chat方法对话完成: appId={}, success={}, promptTokens={}, completionTokens={}, totalPrice={}, latencyMs={}", 
                appId, success, promptTokens, completionTokens, totalPrice, latencyMs);
        
        String errorMessage = null;
        if (!success && response != null && response.getAnswer() != null) {
            errorMessage = response.getAnswer();
        }

        kbConversationService.saveConversationWithCostAndApp(
                finalConversationId,
                safeUserId,
                channel,
                groupId,
                query,
                answer,
                appId,
                app != null ? app.getName() : null,
                promptTokens,
                completionTokens,
                promptPrice,
                completionPrice,
                totalPrice,
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

    @Override
    public DifyChatResponse chatWithSession(String sessionId, String channel, String userId, String query, List<FileInfo> files) {
        return chatWithSession(sessionId, null, channel, userId, query, files);
    }

    @Override
    public DifyChatResponse chatWithSession(String sessionId, Long appId, String channel, String userId, String query, List<FileInfo> files) {
        String safeUserId = StringUtils.hasText(userId) ? userId : "abc-123";
        
        String apiKey = null;
        KbApp app = null;
        if (appId != null) {
            app = appService.getById(appId);
            if (app != null && app.getStatus()) {
                apiKey = app.getDifyApiKey();
            }
        }
        if (apiKey == null) {
            app = appService.getDefaultApp();
            if (app != null) {
                apiKey = app.getDifyApiKey();
                appId = app.getId();
            }
        }
        
        try {
            KbFaq faq = faqService.findSimilar(query);
            if (faq != null && faq.getStatus() && faq.getAnswer() != null) {
                log.info("从FAQ匹配到答案: sessionId={}, question={}, faqId={}", sessionId, query, faq.getId());
                
                DifyChatResponse faqResponse = new DifyChatResponse();
                faqResponse.setAnswer(faq.getAnswer());
                faqResponse.setId(java.util.UUID.randomUUID().toString());
                faqResponse.setConversationId(null);
                faqResponse.setCreatedAt(System.currentTimeMillis());
                
                try {
                    KbConversation message = new KbConversation();
                    message.setSessionId(sessionId);
                    message.setConversationId(java.util.UUID.randomUUID().toString());
                    message.setUserId(safeUserId);
                    message.setChannel(channel);
                    message.setQuery(query);
                    message.setAnswer(faq.getAnswer());
                    message.setTokens(0);
                    message.setLatencyMs(0);
                    message.setStatus(true);
                    message.setErrorMessage("FAQ#" + faq.getId());
                    message.setCreateTime(LocalDateTime.now());
                    
                    chatSessionService.addMessageToSession(sessionId, message);
                } catch (Exception e) {
                    log.warn("保存FAQ消息记录失败: {}", e.getMessage());
                }
                
                return faqResponse;
            }
        } catch (Exception e) {
            log.warn("FAQ匹配失败: {}", e.getMessage());
        }
        
        ChatSession session = chatSessionService.getSessionById(sessionId);
        if (session == null) {
            log.warn("会话不存在，自动创建: sessionId={}", sessionId);
            session = chatSessionService.createSession(safeUserId, channel);
        }

        String difyConversationId = session.getDifyConversationId();

        log.info("会话对话: sessionId={}, userId={}, difyConversationId={}", sessionId, safeUserId, difyConversationId);

        DifyChatRequest req = new DifyChatRequest();
        req.setInputs(new HashMap<>());
        req.setQuery(query);
        req.setResponseMode("blocking");
        req.setConversationId(difyConversationId);
        req.setUser(safeUserId);
        req.setFiles(files);

        long startTime = System.currentTimeMillis();
        DifyChatResponse response = apiKey != null 
            ? difyService.sendChatMessage(req, apiKey) 
            : difyService.sendChatMessage(req);
        int latencyMs = (int) (System.currentTimeMillis() - startTime);

        boolean success = response != null && response.getAnswer() != null && !response.getAnswer().isEmpty();
        String answer = response != null ? response.getAnswer() : null;
        
        Integer promptTokens = null;
        Integer completionTokens = null;
        BigDecimal promptPrice = null;
        BigDecimal completionPrice = null;
        BigDecimal totalPrice = null;
        
        if (response != null && response.getUsage() != null) {
            promptTokens = response.getUsage().getPromptTokens();
            completionTokens = response.getUsage().getCompletionTokens();
            try {
                if (response.getUsage().getPromptPrice() != null) {
                    promptPrice = new BigDecimal(response.getUsage().getPromptPrice());
                }
                if (response.getUsage().getCompletionPrice() != null) {
                    completionPrice = new BigDecimal(response.getUsage().getCompletionPrice());
                }
                if (response.getUsage().getTotalPrice() != null) {
                    totalPrice = new BigDecimal(response.getUsage().getTotalPrice());
                }
            } catch (NumberFormatException e) {
                log.warn("解析价格失败: {}", e.getMessage());
            }
        }
        
        log.info("chatWithSession对话完成: sessionId={}, appId={}, success={}, promptTokens={}, completionTokens={}, totalPrice={}, latencyMs={}", 
                sessionId, appId, success, promptTokens, completionTokens, totalPrice, latencyMs);
        
        String errorMessage = null;
        if (!success && response != null && response.getAnswer() != null) {
            errorMessage = response.getAnswer();
        }

        String finalConversationId = response != null && StringUtils.hasText(response.getConversationId()) 
                ? response.getConversationId() 
                : java.util.UUID.randomUUID().toString();

        KbConversation message = new KbConversation();
        message.setSessionId(sessionId);
        message.setConversationId(finalConversationId);
        message.setUserId(safeUserId);
        message.setChannel(channel);
        message.setQuery(query);
        message.setAnswer(answer);
        message.setAppId(appId);
        message.setAppName(app != null ? app.getName() : null);
        message.setPromptTokens(promptTokens);
        message.setCompletionTokens(completionTokens);
        message.setPromptPrice(promptPrice);
        message.setCompletionPrice(completionPrice);
        message.setTotalPrice(totalPrice);
        message.setLatencyMs(latencyMs);
        message.setStatus(success);
        message.setErrorMessage(errorMessage);
        message.setCreateTime(LocalDateTime.now());

        chatSessionService.addMessageToSession(sessionId, message);

        if (response != null && StringUtils.hasText(response.getConversationId()) && session.getDifyConversationId() == null) {
            session.setDifyConversationId(response.getConversationId());
            session.setUpdateTime(LocalDateTime.now());
            chatSessionService.updateById(session);
        }

        return response;
    }
}

