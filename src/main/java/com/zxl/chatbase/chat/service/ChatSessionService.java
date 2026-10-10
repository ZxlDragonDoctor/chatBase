package com.zxl.chatbase.chat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zxl.chatbase.chat.entity.ChatSession;
import com.zxl.chatbase.kb.entity.KbConversation;

import java.util.List;

public interface ChatSessionService extends IService<ChatSession> {

    ChatSession createSession(String userId, String channel);

    ChatSession getSessionById(String sessionId);

    /** 按用户归属获取会话，userId 不匹配返回 null */
    ChatSession getSessionForUser(String sessionId, String userId);

    Page<ChatSession> listUserSessions(String userId, String channel, Integer pageNum, Integer pageSize);

    boolean deleteSession(String sessionId);

    /** 按用户归属删除会话 */
    boolean deleteSessionForUser(String sessionId, String userId);

    boolean updateSessionTitle(String sessionId, String title);

    /** 按用户归属更新标题 */
    boolean updateSessionTitleForUser(String sessionId, String title, String userId);

    List<KbConversation> getSessionMessages(String sessionId);

    /** 按用户归属取消息列表 */
    List<KbConversation> getSessionMessagesForUser(String sessionId, String userId);

    void addMessageToSession(String sessionId, KbConversation message);
}