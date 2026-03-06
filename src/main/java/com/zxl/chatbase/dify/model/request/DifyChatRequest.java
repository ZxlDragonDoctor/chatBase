package com.zxl.chatbase.dify.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DifyChatRequest {
    
    @JsonProperty("inputs")
    private Map<String, Object> inputs;           // 变量值
    
    @JsonProperty("query")
    private String query;                          // 用户问题
    
    @JsonProperty("response_mode")
    private String responseMode;                   // streaming 或 blocking
    
    @JsonProperty("conversation_id")
    private String conversationId;                  // 会话ID，续聊时传入
    
    @JsonProperty("user")
    private String user;                            // 用户标识
    
    @JsonProperty("files")
    private List<FileInfo> files;                   // 文件列表（可选）
    
    @Data
    public static class FileInfo {
        private String type;                         // image, document 等
        @JsonProperty("transfer_method")
        private String transferMethod;                // remote_url 或 local_file
        private String url;                           // 文件URL
    }
}