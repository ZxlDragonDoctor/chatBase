package com.zxl.chatbase.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxl.chatbase.kb.entity.KbConversation;
import com.zxl.chatbase.kb.entity.KbFeedback;
import com.zxl.chatbase.kb.mapper.KbConversationMapper;
import com.zxl.chatbase.kb.mapper.KbFeedbackMapper;
import com.zxl.chatbase.kb.service.IKbConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class KbConversationServiceImpl extends ServiceImpl<KbConversationMapper, KbConversation> implements IKbConversationService {

    private final KbFeedbackMapper feedbackMapper;

    @Override
    public void saveConversation(String conversationId, String userId, String channel, String groupId,
                                  String query, String answer, Long tokens, Integer latencyMs, boolean success) {
        saveConversation(conversationId, userId, channel, groupId, query, answer, tokens, latencyMs, success, null);
    }

    @Override
    public void saveConversation(String conversationId, String userId, String channel, String groupId,
                                  String query, String answer, Long tokens, Integer latencyMs, boolean success, String errorMessage) {
        try {
            KbConversation conversation = new KbConversation();
            conversation.setConversationId(conversationId);
            conversation.setUserId(userId);
            conversation.setChannel(channel);
            conversation.setGroupId(groupId);
            conversation.setQuery(query);
            conversation.setAnswer(answer);
            conversation.setTokens(tokens != null ? tokens.intValue() : 0);
            conversation.setLatencyMs(latencyMs != null ? latencyMs : 0);
            conversation.setStatus(success);
            conversation.setErrorMessage(errorMessage);
            conversation.setCreateTime(LocalDateTime.now());
            
            save(conversation);
            log.info("会话记录保存成功: conversationId={}, userId={}, channel={}", conversationId, userId, channel);
        } catch (Exception e) {
            log.error("保存会话记录失败", e);
        }
    }

    @Override
    public void saveConversationWithCost(String conversationId, String userId, String channel, String groupId,
                                           String query, String answer, Integer promptTokens, Integer completionTokens,
                                           BigDecimal promptPrice, BigDecimal completionPrice, BigDecimal totalPrice,
                                           Integer latencyMs, boolean success, String errorMessage) {
        try {
            KbConversation conversation = new KbConversation();
            conversation.setConversationId(conversationId);
            conversation.setUserId(userId);
            conversation.setChannel(channel);
            conversation.setGroupId(groupId);
            conversation.setQuery(query);
            conversation.setAnswer(answer);
            conversation.setPromptTokens(promptTokens != null ? promptTokens : 0);
            conversation.setCompletionTokens(completionTokens != null ? completionTokens : 0);
            conversation.setTokens((promptTokens != null ? promptTokens : 0) + (completionTokens != null ? completionTokens : 0));
            conversation.setPromptPrice(promptPrice != null ? promptPrice : BigDecimal.ZERO);
            conversation.setCompletionPrice(completionPrice != null ? completionPrice : BigDecimal.ZERO);
            conversation.setTotalPrice(totalPrice != null ? totalPrice : BigDecimal.ZERO);
            conversation.setLatencyMs(latencyMs != null ? latencyMs : 0);
            conversation.setStatus(success);
            conversation.setErrorMessage(errorMessage);
            conversation.setCreateTime(LocalDateTime.now());
            
            save(conversation);
            log.info("会话记录保存成功(含费用): conversationId={}, tokens={}, cost={}", 
                    conversationId, conversation.getTokens(), conversation.getTotalPrice());
        } catch (Exception e) {
            log.error("保存会话记录失败", e);
        }
    }

    @Override
    public Page<KbConversation> pageList(String userId, String channel, Integer pageNum, Integer pageSize) {
        Page<KbConversation> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<KbConversation> wrapper = new LambdaQueryWrapper<>();
        if (userId != null && !userId.isBlank()) {
            wrapper.eq(KbConversation::getUserId, userId);
        }
        if (channel != null && !channel.isBlank() && !"all".equals(channel)) {
            wrapper.eq(KbConversation::getChannel, channel);
        }
        wrapper.orderByDesc(KbConversation::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    @Transactional
    public boolean addFeedback(Long conversationId, Integer rating, String feedbackType, String content) {
        KbConversation conversation = getById(conversationId);
        if (conversation == null) {
            log.warn("会话不存在: {}", conversationId);
            return false;
        }
        
        KbFeedback feedback = new KbFeedback();
        feedback.setConversationId(conversationId);
        feedback.setUserId(conversation.getUserId());
        feedback.setRating(rating);
        feedback.setFeedbackType(feedbackType);
        feedback.setFeedbackContent(content);
        feedback.setCreateTime(LocalDateTime.now());
        feedback.setStatus(false);
        
        return feedbackMapper.insert(feedback) > 0;
    }

    @Override
    @Transactional
    public boolean addFeedbackBySession(String sessionId, Integer messageIndex, Integer rating, String feedbackType, String content) {
        LambdaQueryWrapper<KbConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbConversation::getSessionId, sessionId)
                .eq(KbConversation::getStatus, true)
                .orderByAsc(KbConversation::getCreateTime);
        
        var conversations = list(wrapper);
        if (conversations == null || conversations.isEmpty()) {
            log.warn("会话中没有消息: sessionId={}", sessionId);
            return false;
        }
        
        int actualIndex = messageIndex / 2;
        if (actualIndex < 0 || actualIndex >= conversations.size()) {
            log.warn("消息索引超出范围: sessionId={}, index={}, size={}", sessionId, actualIndex, conversations.size());
            return false;
        }
        
        KbConversation conversation = conversations.get(actualIndex);
        
        KbFeedback feedback = new KbFeedback();
        feedback.setConversationId(conversation.getId());
        feedback.setUserId(conversation.getUserId());
        feedback.setRating(rating);
        feedback.setFeedbackType(feedbackType);
        feedback.setFeedbackContent(content);
        feedback.setCreateTime(LocalDateTime.now());
        feedback.setStatus(false);
        
        boolean success = feedbackMapper.insert(feedback) > 0;
        log.info("反馈保存: sessionId={}, conversationId={}, rating={}, success={}", sessionId, conversation.getId(), rating, success);
        return success;
    }
}
