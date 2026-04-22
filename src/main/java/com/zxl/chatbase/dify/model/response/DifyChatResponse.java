package com.zxl.chatbase.dify.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DifyChatResponse {

    private String event;
    
    private String taskId;
    
    private String id;
    
    @JsonProperty("message_id")
    private String messageId;
    
    @JsonProperty("conversation_id")
    private String conversationId;
    
    private String mode;
    
    private String answer;
    
    private Metadata metadata;
    
    private Usage usage;
    
    @JsonProperty("retriever_resources")
    private List<RetrieverResource> retrieverResources;
    
    @JsonProperty("created_at")
    private Long createdAt;
}