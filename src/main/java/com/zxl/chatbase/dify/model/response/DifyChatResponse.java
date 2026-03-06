package com.zxl.chatbase.dify.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class DifyChatResponse {
    
    @JsonProperty("event")
    private String event;                           // 事件类型，一般为 message
    
    @JsonProperty("task_id")
    private String taskId;
    
    @JsonProperty("message_id")
    private String messageId;
    
    @JsonProperty("conversation_id")
    private String conversationId;
    
    @JsonProperty("mode")
    private String mode;                             // chat
    
    @JsonProperty("answer")
    private String answer;                           // 完整的回复内容
    
    @JsonProperty("metadata")
    private Metadata metadata;                        // 元数据
    
    @Data
    public static class Metadata {
        @JsonProperty("usage")
        private Usage usage;                          // token使用情况
        
        @JsonProperty("retriever_resources")
        private List<RetrieverResource> retrieverResources; // 引用来源
    }
    
    @Data
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        
        @JsonProperty("completion_tokens")
        private int completionTokens;
        
        @JsonProperty("total_tokens")
        private int totalTokens;
    }
    
    @Data
    public static class RetrieverResource {
        private int position;
        @JsonProperty("dataset_id")
        private String datasetId;
        @JsonProperty("dataset_name")
        private String datasetName;
        @JsonProperty("document_id")
        private String documentId;
        @JsonProperty("document_name")
        private String documentName;
        @JsonProperty("segment_id")
        private String segmentId;
        private double score;
        private String content;
    }
    
    @JsonProperty("created_at")
    private long createdAt;
}