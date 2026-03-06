package com.zxl.chatbase.dify.controller;


import com.zxl.chatbase.dify.model.response.DifyChatResponse;
import com.zxl.chatbase.dify.server.DifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    @Autowired
    private DifyService difyService;
    
    /**
     * 简单问答接口
     * @param query 问题
     * @param conversationId 会话ID（可选）
     * @param userId 用户ID（可选）
     * @return Dify的回答
     */
    @GetMapping("/ask")
    public DifyChatResponse ask(
            @RequestParam String query,
            @RequestParam(required = false) String conversationId,
            @RequestParam(defaultValue = "web-user") String userId) {
        
        return difyService.sendChatMessage(query, conversationId, userId);
    }
    
    /**
     * POST接口，支持更复杂的参数
     */
    @PostMapping("/message")
    public DifyChatResponse sendMessage(@RequestBody MessageRequest request) {
        return difyService.sendChatMessage(
            request.getQuery(), 
            request.getConversationId(), 
            request.getUserId()
        );
    }
    
    // 内部请求类
    public static class MessageRequest {
        private String query;
        private String conversationId;
        private String userId;
        
        // getters and setters
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}