package com.zxl.chatbase.im.service;

import com.zxl.chatbase.im.dto.ConversationSummaryVO;
import com.zxl.chatbase.im.entity.ImConversation;

import java.util.List;

public interface ImConversationService {

    ImConversation getOrCreateConversation(String platform, String userId, String userNickname, String createdBy);

    void updateLastMessage(String conversationId, String message, String userId, String platform);

    List<ConversationSummaryVO> listAccessibleConversations(String userId);

    void bindApp(Long id, Long appId, String appName, String userId);

    void unbindApp(Long id, String userId);

    Long getAppIdForConversation(String conversationId);
}
