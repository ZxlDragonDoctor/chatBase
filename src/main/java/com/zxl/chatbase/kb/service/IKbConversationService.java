package com.zxl.chatbase.kb.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zxl.chatbase.kb.entity.KbConversation;

public interface IKbConversationService extends IService<KbConversation> {
    
    void saveConversation(String conversationId, String userId, String channel, String groupId,
                          String query, String answer, Long tokens, Integer latencyMs, boolean success);
    
    void saveConversation(String conversationId, String userId, String channel, String groupId,
                          String query, String answer, Long tokens, Integer latencyMs, boolean success, String errorMessage);
    
    Page<KbConversation> pageList(String userId, String channel, Integer pageNum, Integer pageSize);
    
    boolean addFeedback(Long conversationId, Integer rating, String feedbackType, String content);
}
