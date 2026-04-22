package com.zxl.chatbase.chat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zxl.chatbase.chat.entity.ChatSession;
import com.zxl.chatbase.kb.entity.KbConversation;

import java.util.List;

public interface ChatSessionService extends IService<ChatSession> {

    ChatSession createSession(String userId, String channel);

    ChatSession getSessionById(String sessionId);

    Page<ChatSession> listUserSessions(String userId, String channel, Integer pageNum, Integer pageSize);

    boolean deleteSession(String sessionId);

    boolean updateSessionTitle(String sessionId, String title);

    List<KbConversation> getSessionMessages(String sessionId);

    void addMessageToSession(String sessionId, KbConversation message);
}