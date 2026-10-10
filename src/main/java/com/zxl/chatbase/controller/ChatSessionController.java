package com.zxl.chatbase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zxl.chatbase.chat.entity.ChatSession;
import com.zxl.chatbase.chat.service.ChatSessionService;
import com.zxl.chatbase.kb.entity.KbConversation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Web 聊天会话管理
 *
 * 会话按 userId 归属隔离：每个接口都要求携带 userId，并校验 session.userId 一致，
 * 防止遍历 sessionId 读取/删除他人会话。
 */
@RestController
@RequestMapping("/api/chat/session")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService sessionService;

    @PostMapping("/create")
    public ChatSession createSession(@RequestParam String userId, @RequestParam(defaultValue = "web") String channel) {
        return sessionService.createSession(userId, channel);
    }

    @GetMapping("/list")
    public Page<ChatSession> listSessions(
            @RequestParam String userId,
            @RequestParam(defaultValue = "web") String channel,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return sessionService.listUserSessions(userId, channel, pageNum, pageSize);
    }

    @GetMapping("/{sessionId}")
    public ChatSession getSession(@PathVariable String sessionId, @RequestParam String userId) {
        return sessionService.getSessionForUser(sessionId, userId);
    }

    @GetMapping("/{sessionId}/messages")
    public List<KbConversation> getSessionMessages(@PathVariable String sessionId, @RequestParam String userId) {
        return sessionService.getSessionMessagesForUser(sessionId, userId);
    }

    @DeleteMapping("/{sessionId}")
    public Map<String, Object> deleteSession(@PathVariable String sessionId, @RequestParam String userId) {
        boolean success = sessionService.deleteSessionForUser(sessionId, userId);
        return Map.of("success", success, "message", success ? "删除成功" : "删除失败或无权操作");
    }

    @PutMapping("/{sessionId}/title")
    public Map<String, Object> updateTitle(@PathVariable String sessionId,
                                          @RequestParam String title,
                                          @RequestParam String userId) {
        boolean success = sessionService.updateSessionTitleForUser(sessionId, title, userId);
        return Map.of("success", success, "message", success ? "更新成功" : "更新失败或无权操作");
    }
}
