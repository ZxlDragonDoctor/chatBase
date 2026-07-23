package com.zxl.chatbase.im.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zxl.chatbase.im.dto.ConversationSummaryVO;
import com.zxl.chatbase.im.entity.ImConversation;
import com.zxl.chatbase.im.mapper.ImConversationMapper;
import com.zxl.chatbase.im.service.ImConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImConversationServiceImpl implements ImConversationService {

    private final ImConversationMapper imConversationMapper;

    private static String buildConversationId(String platform, String userId) {
        return "single:" + platform + ":" + userId;
    }

    @Override
    public ImConversation getOrCreateConversation(String platform, String userId, String userNickname, String createdBy) {
        String conversationId = buildConversationId(platform, userId);
        LambdaQueryWrapper<ImConversation> wrapper = new LambdaQueryWrapper<ImConversation>()
                .eq(ImConversation::getPlatform, platform)
                .eq(ImConversation::getConversationId, conversationId);
        ImConversation conv = imConversationMapper.selectOne(wrapper);
        if (conv == null) {
            conv = new ImConversation();
            conv.setPlatform(platform);
            conv.setConversationId(conversationId);
            conv.setUserId(userId);
            conv.setUserNickname(userNickname);
            conv.setConversationType("single");
            conv.setTitle("与 " + (userNickname != null ? userNickname : userId) + " 的对话");
            conv.setMessageCount(0);
            conv.setCreatedBy(createdBy);
            conv.setStatus(true);
            conv.setCreateTime(LocalDateTime.now());
            imConversationMapper.insert(conv);
            log.info("创建单聊会话: platform={}, userId={}, conversationId={}", platform, userId, conversationId);
        }
        return conv;
    }

    @Override
    public void updateLastMessage(String conversationId, String message, String userId, String platform) {
        LambdaQueryWrapper<ImConversation> wrapper = new LambdaQueryWrapper<ImConversation>()
                .eq(ImConversation::getPlatform, platform)
                .eq(ImConversation::getConversationId, conversationId);
        ImConversation conv = imConversationMapper.selectOne(wrapper);
        if (conv != null) {
            conv.setLastMessage(message != null && message.length() > 200 ? message.substring(0, 200) : message);
            conv.setLastMessageTime(LocalDateTime.now());
            conv.setMessageCount(conv.getMessageCount() != null ? conv.getMessageCount() + 1 : 1);
            imConversationMapper.updateById(conv);
        }
    }

    @Override
    public List<ConversationSummaryVO> listAccessibleConversations(String userId) {
        if (userId == null) return List.of();
        return imConversationMapper.selectAccessibleConversations(userId);
    }
}
