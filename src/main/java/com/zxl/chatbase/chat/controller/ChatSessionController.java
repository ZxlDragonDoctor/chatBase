package com.zxl.chatbase.chat.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zxl.chatbase.chat.entity.ChatSession;
import com.zxl.chatbase.chat.service.ChatSessionService;
import com.zxl.chatbase.kb.entity.KbConversation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public ChatSession getSession(@PathVariable String sessionId) {
        return sessionService.getSessionById(sessionId);
    }

    @GetMapping("/{sessionId}/messages")
    public List<KbConversation> getSessionMessages(@PathVariable String sessionId) {
        return sessionService.getSessionMessages(sessionId);
    }

    @DeleteMapping("/{sessionId}")
    public Map<String, Object> deleteSession(@PathVariable String sessionId) {
        boolean success = sessionService.deleteSession(sessionId);
        return Map.of("success", success, "message", success ? "删除成功" : "删除失败");
    }

    @PutMapping("/{sessionId}/title")
    public Map<String, Object> updateTitle(@PathVariable String sessionId, @RequestParam String title) {
        boolean success = sessionService.updateSessionTitle(sessionId, title);
        return Map.of("success", success, "message", success ? "更新成功" : "更新失败");
    }
}