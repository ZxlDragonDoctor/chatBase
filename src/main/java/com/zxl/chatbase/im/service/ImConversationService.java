package com.zxl.chatbase.im.service;

import com.zxl.chatbase.im.dto.ConversationSummaryVO;
import com.zxl.chatbase.im.entity.ImConversation;

import java.util.List;

public interface ImConversationService {

    /**
     * 本地 opencode 特殊应用的 appId 哨兵值
     */
    Long OPENCODE_APP_ID = -1L;

    /**
     * 本地 opencode 应用名称
     */
    String OPENCODE_APP_NAME = "本地opencode";

    /**
     * 判断会话是否绑定了本地 opencode 应用
     */
    boolean isOpencodeBound(String conversationId);

    ImConversation getOrCreateConversation(String platform, String userId, String userNickname, String createdBy);

    void updateLastMessage(String conversationId, String message, String userId, String platform);

    List<ConversationSummaryVO> listAccessibleConversations(String userId);

    void bindApp(Long id, Long appId, String appName, String userId);

    void unbindApp(Long id, String userId);

    Long getAppIdForConversation(String conversationId);
}
