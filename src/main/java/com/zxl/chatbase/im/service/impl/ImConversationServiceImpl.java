package com.zxl.chatbase.im.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zxl.chatbase.im.dto.ConversationSummaryVO;
import com.zxl.chatbase.im.entity.ImConversation;
import com.zxl.chatbase.im.mapper.ImConversationMapper;
import com.zxl.chatbase.im.service.ImConversationService;
import com.zxl.chatbase.kb.entity.KbApp;
import com.zxl.chatbase.kb.entity.SysUser;
import com.zxl.chatbase.kb.mapper.KbAppMapper;
import com.zxl.chatbase.kb.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImConversationServiceImpl implements ImConversationService {

    private final ImConversationMapper imConversationMapper;
    private final KbAppMapper kbAppMapper;
    private final SysUserMapper sysUserMapper;

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

    @Override
    public boolean isOpencodeBound(String conversationId) {
        if (!StringUtils.hasText(conversationId)) return false;
        try {
            ImConversation conv = imConversationMapper.selectOne(
                    new LambdaQueryWrapper<ImConversation>()
                            .eq(ImConversation::getConversationId, conversationId)
                            .last("LIMIT 1")
            );
            return conv != null && OPENCODE_APP_ID.equals(conv.getAppId());
        } catch (Exception e) {
            log.error("判断opencode绑定失败: conversationId={}", conversationId, e);
            return false;
        }
    }

    @Override
    public void bindApp(Long id, Long appId, String appName, String userId) {
        ImConversation conv = imConversationMapper.selectById(id);
        if (conv == null) {
            throw new RuntimeException("会话不存在");
        }
        if (conv.getCreatedBy() != null && !conv.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权绑定应用：非会话归属用户");
        }

        // 绑定本地 opencode：仅管理员可用
        if (OPENCODE_APP_ID.equals(appId)) {
            if (!isAdmin(userId)) {
                throw new RuntimeException("无权绑定本地opencode：仅管理员可用");
            }
            conv.setCreatedBy(userId);
            conv.setAppId(OPENCODE_APP_ID);
            conv.setAppName(OPENCODE_APP_NAME);
            conv.setUpdateTime(LocalDateTime.now());
            imConversationMapper.updateById(conv);
            log.info("会话绑定本地opencode: conversationId={}, userId={}", conv.getConversationId(), userId);
            return;
        }

        conv.setCreatedBy(userId);
        conv.setAppId(appId);
        conv.setAppName(appName);
        conv.setUpdateTime(LocalDateTime.now());
        imConversationMapper.updateById(conv);
        log.info("会话绑定应用: conversationId={}, appId={}, appName={}, userId={}", conv.getConversationId(), appId, appName, userId);
    }

    @Override
    public void unbindApp(Long id, String userId) {
        ImConversation conv = imConversationMapper.selectById(id);
        if (conv == null) {
            throw new RuntimeException("会话不存在");
        }
        if (conv.getCreatedBy() != null && !conv.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权解绑应用：非会话归属用户");
        }
        conv.setCreatedBy(null);
        conv.setAppId(null);
        conv.setAppName(null);
        conv.setUpdateTime(LocalDateTime.now());
        imConversationMapper.updateById(conv);
        log.info("会话解除应用绑定: conversationId={}, userId={}", conv.getConversationId(), userId);
    }

    @Override
    public Long getAppIdForConversation(String conversationId) {
        if (!StringUtils.hasText(conversationId)) return null;
        try {
            ImConversation conv = imConversationMapper.selectOne(
                    new LambdaQueryWrapper<ImConversation>()
                            .eq(ImConversation::getConversationId, conversationId)
                            .last("LIMIT 1")
            );
            if (conv != null && conv.getAppId() != null) {
                // 本地 opencode 特殊绑定，不属于 Dify 应用，直接返回 null（由调用方走 opencode 通道）
                if (OPENCODE_APP_ID.equals(conv.getAppId())) {
                    return null;
                }
                KbApp app = kbAppMapper.selectById(conv.getAppId());
                if (app != null && Boolean.TRUE.equals(app.getStatus())) {
                    return app.getId();
                }
            }
            KbApp defaultApp = kbAppMapper.selectOne(
                    new LambdaQueryWrapper<KbApp>()
                            .eq(KbApp::getStatus, true)
                            .eq(KbApp::getIsDefault, true)
                            .last("LIMIT 1")
            );
            return defaultApp != null ? defaultApp.getId() : null;
        } catch (Exception e) {
            log.error("获取会话应用失败: conversationId={}", conversationId, e);
            return null;
        }
    }

    private boolean isAdmin(String userId) {
        if (!StringUtils.hasText(userId)) return false;
        try {
            SysUser user = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getUsername, userId)
                            .eq(SysUser::getStatus, true)
                            .last("LIMIT 1")
            );
            return user != null && "admin".equals(user.getRole());
        } catch (Exception e) {
            log.error("查询管理员状态失败: userId={}", userId, e);
            return false;
        }
    }
}
