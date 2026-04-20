package com.zxl.chatbase.controller;

import com.zxl.chatbase.chat.service.ChatService;
import com.zxl.chatbase.common.RateLimitException;
import com.zxl.chatbase.common.service.RateLimitService;
import com.zxl.chatbase.dify.model.request.FileInfo;
import com.zxl.chatbase.dify.model.response.BatchUploadResponse;
import com.zxl.chatbase.dify.model.response.DifyChatResponse;
import com.zxl.chatbase.dify.model.response.DifyFileUploadResponse;
import com.zxl.chatbase.dify.server.DifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    @Autowired
    private DifyService difyService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private RateLimitService rateLimitService;
    
    @GetMapping("/ask")
    public DifyChatResponse ask(
            @RequestParam String query,
            @RequestParam(required = false) String conversationId,
            @RequestParam(defaultValue = "abc-123") String userId) {
        
        return difyService.sendChatMessage(query, conversationId, userId);
    }
    
    @PostMapping("/message")
    public DifyChatResponse sendMessage(@RequestBody MessageRequest request) {
        return difyService.sendChatMessage(
            request.getQuery(), 
            request.getConversationId(), 
            request.getUserId()
        );
    }

    @PostMapping(value = "/v1/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DifyFileUploadResponse uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart("user") String user
    ){
        return difyService.uploadFile(file, user);
    }

    @PostMapping(value = "/v1/files/batch-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BatchUploadResponse batchUploadFiles(
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("user") String user,
            @RequestPart(value = "datasetId", required = false) String datasetId
    ) {
        return difyService.batchUploadFiles(files, user, datasetId);
    }

    @PostMapping("/im")
    public DifyChatResponse imChat(@RequestBody ImChatRequest request) {
        try {
            rateLimitService.checkRateLimit("im", request.getUserId(), request.getGroupId());
            rateLimitService.recordRequest("im", request.getUserId(), request.getGroupId());
        } catch (RateLimitException e) {
            DifyChatResponse resp = new DifyChatResponse();
            resp.setAnswer(e.getMessage());
            return resp;
        }
        return chatService.chat("im", request.getUserId(), request.getGroupId(), request.getText());
    }

    @PostMapping("/web")
    public DifyChatResponse webChat(@RequestBody WebChatRequest request) {
        try {
            rateLimitService.checkRateLimit("web", request.getUserId(), null);
            rateLimitService.recordRequest("web", request.getUserId(), null);
        } catch (RateLimitException e) {
            DifyChatResponse resp = new DifyChatResponse();
            resp.setAnswer(e.getMessage());
            return resp;
        }
        return chatService.chat("web", request.getUserId(), null, request.getText(), request.getFiles());
    }

    @PostMapping("/web/session")
    public DifyChatResponse webChatWithSession(@RequestBody SessionChatRequest request) {
        try {
            rateLimitService.checkRateLimit("web", request.getUserId(), null);
            rateLimitService.recordRequest("web", request.getUserId(), null);
        } catch (RateLimitException e) {
            DifyChatResponse resp = new DifyChatResponse();
            resp.setAnswer(e.getMessage());
            return resp;
        }
        return chatService.chatWithSession(request.getSessionId(), "web", request.getUserId(), request.getText(), request.getFiles());
    }
    
    public static class MessageRequest {
        private String query;
        private String conversationId;
        private String userId;
        
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }

    public static class ImChatRequest {
        private String text;
        private String userId;
        private String groupId;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getGroupId() { return groupId; }
        public void setGroupId(String groupId) { this.groupId = groupId; }
    }

    public static class WebChatRequest {
        private String text;
        private String userId;
        private List<FileInfo> files;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public List<FileInfo> getFiles() { return files; }
        public void setFiles(List<FileInfo> files) { this.files = files; }
    }

    public static class SessionChatRequest {
        private String sessionId;
        private String text;
        private String userId;
        private List<FileInfo> files;

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public List<FileInfo> getFiles() { return files; }
        public void setFiles(List<FileInfo> files) { this.files = files; }
    }
}