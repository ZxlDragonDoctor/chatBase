package com.zxl.chatbase.dify.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * ChunkChatCompletionResponse - 流式响应
 * 当 response_mode 为 streaming 时返回
 */
@Data
public class DifyStreamChatResponse {
    
    /**
     * 事件类型，可能的值：message、message_end、message_file、ping等
     */
    private String event;
    
    /**
     * 任务 ID
     */
    private String taskId;
    
    /**
     * 消息唯一 ID
     */
    private String messageId;
    
    /**
     * 会话 ID
     */
    private String conversationId;
    
    /**
     * 回答内容片段
     */
    private String answer;
    
    /**
     * 创建时间戳
     */
    private Long createdAt;
    
    /**
     * 元数据（在message_end事件中可能包含）
     */
    private Metadata metadata;
}