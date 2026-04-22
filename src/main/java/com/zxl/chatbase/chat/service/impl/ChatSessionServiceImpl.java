package com.zxl.chatbase.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxl.chatbase.chat.entity.ChatSession;
import com.zxl.chatbase.chat.mapper.ChatSessionMapper;
import com.zxl.chatbase.chat.service.ChatSessionService;
import com.zxl.chatbase.kb.entity.KbConversation;
import com.zxl.chatbase.kb.mapper.KbConversationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {

    private final KbConversationMapper conversationMapper;

    @Override
    @Transactional
    public ChatSession createSession(String userId, String channel) {
        ChatSession session = new ChatSession();
        session.setSessionId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        session.setUserId(userId);
        session.setChannel(channel != null ? channel : "web");
        session.setMessageCount(0);
        session.setStatus(true);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());

        save(session);
        log.info("创建新会话: sessionId={}, userId={}", session.getSessionId(), userId);
        return session;
    }

    @Override
    public ChatSession getSessionById(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getStatus, true);
        return getOne(wrapper);
    }

    @Override
    public Page<ChatSession> listUserSessions(String userId, String channel, Integer pageNum, Integer pageSize) {
        Page<ChatSession> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getStatus, true);
        if (channel != null && !channel.isEmpty()) {
            wrapper.eq(ChatSession::getChannel, channel);
        }
        wrapper.ge(ChatSession::getLastMessageTime, LocalDateTime.now().minusDays(30));
        wrapper.orderByDesc(ChatSession::getLastMessageTime);
        return page(page, wrapper);
    }

    @Override
    @Transactional
    public boolean deleteSession(String sessionId) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getSessionId, sessionId);
        ChatSession session = getOne(wrapper);
        if (session == null) {
            return false;
        }
        session.setStatus(false);
        session.setUpdateTime(LocalDateTime.now());
        return updateById(session);
    }

    @Override
    @Transactional
    public boolean updateSessionTitle(String sessionId, String title) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getSessionId, sessionId);
        ChatSession session = getOne(wrapper);
        if (session == null) {
            return false;
        }
        session.setTitle(title);
        session.setUpdateTime(LocalDateTime.now());
        return updateById(session);
    }

    @Override
    public List<KbConversation> getSessionMessages(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<KbConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbConversation::getSessionId, sessionId)
                .eq(KbConversation::getStatus, true)
                .orderByAsc(KbConversation::getCreateTime);
        return conversationMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void addMessageToSession(String sessionId, KbConversation message) {
        if (sessionId == null || sessionId.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getSessionId, sessionId);
        ChatSession session = getOne(wrapper);
        if (session == null) {
            return;
        }

        message.setSessionId(sessionId);
        conversationMapper.insert(message);

        session.setMessageCount(session.getMessageCount() != null ? session.getMessageCount() + 1 : 1);
        session.setLastMessageTime(LocalDateTime.now());

        if (session.getTitle() == null || session.getTitle().isEmpty()) {
            String title = message.getQuery();
            if (title != null && title.length() > 50) {
                title = title.substring(0, 50) + "...";
            }
            session.setTitle(title);
        }

        if (message.getDifyResponseId() != null && session.getDifyConversationId() == null) {
            session.setDifyConversationId(message.getConversationId());
        }

        session.setUpdateTime(LocalDateTime.now());
        updateById(session);
    }
}