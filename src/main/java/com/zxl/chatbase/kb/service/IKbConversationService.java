package com.zxl.chatbase.kb.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zxl.chatbase.kb.entity.KbConversation;

import java.math.BigDecimal;

public interface IKbConversationService extends IService<KbConversation> {
    
    void saveConversation(String conversationId, String userId, String channel, String groupId,
                          String query, String answer, Long tokens, Integer latencyMs, boolean success);
    
    void saveConversation(String conversationId, String userId, String channel, String groupId,
                          String query, String answer, Long tokens, Integer latencyMs, boolean success, String errorMessage);

    void saveConversationWithCost(String conversationId, String userId, String channel, String groupId,
                                   String query, String answer, Integer promptTokens, Integer completionTokens,
                                   BigDecimal promptPrice, BigDecimal completionPrice, BigDecimal totalPrice,
                                   Integer latencyMs, boolean success, String errorMessage);

    void saveConversationWithCostAndApp(String conversationId, String userId, String channel, String groupId,
                                   String query, String answer, Long appId, String appName,
                                   Integer promptTokens, Integer completionTokens,
                                   BigDecimal promptPrice, BigDecimal completionPrice, BigDecimal totalPrice,
                                   Integer latencyMs, boolean success, String errorMessage);
    
    Page<KbConversation> pageList(String userId, String channel, Integer pageNum, Integer pageSize);
    
    boolean addFeedback(Long conversationId, Integer rating, String feedbackType, String content);
    
    boolean addFeedbackBySession(String sessionId, Integer messageIndex, Integer rating, String feedbackType, String content);
}
