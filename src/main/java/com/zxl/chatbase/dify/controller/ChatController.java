package com.zxl.chatbase.dify.controller;


import com.zxl.chatbase.chat.ChatService;
import com.zxl.chatbase.dify.model.response.DifyChatResponse;
import com.zxl.chatbase.dify.model.response.DifyFileUploadResponse;
import com.zxl.chatbase.dify.server.DifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    @Autowired
    private DifyService difyService;

    @Autowired
    private ChatService chatService;
    
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
            @RequestParam(defaultValue = "abc-123") String userId) {
        
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

    /**
     * 上传文件
     *
     * @param file 要上传的文件
     * @param user 用户标识
     * @return 文件上传响应
     */
    @PostMapping(value = "/v1/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    DifyFileUploadResponse uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart("user") String user
    ){
        return difyService.uploadFile(file, user);
    }

    /**
     * 群聊/IM 机器人入口
     * 机器人收到消息后，将消息内容 POST 到这个接口
     */
    @PostMapping("/im")
    public DifyChatResponse imChat(@RequestBody ImChatRequest request) {
        return chatService.chat("im", request.getUserId(), request.getGroupId(), request.getText());
    }

    /**
     * Web 端聊天入口
     */
    @PostMapping("/web")
    public DifyChatResponse webChat(@RequestBody WebChatRequest request) {
        // Web 端目前不区分群组，groupId 传 null
        return chatService.chat("web", request.getUserId(), null, request.getText());
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

    /**
     * 群聊/IM 请求体
     */
    public static class ImChatRequest {
        private String text;
        private String userId;
        private String groupId;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }
    }

    /**
     * Web 端请求体
     */
    public static class WebChatRequest {
        private String text;
        private String userId;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
    }
}