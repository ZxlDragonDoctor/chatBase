package com.zxl.chatbase.dify.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * ChatCompletionResponse - 完整的App结果响应
 * 当 response_mode 为 blocking 时返回
 */
@Data
public class DifyChatResponse {
    
    /**
     * 事件类型，固定为 message
     */
    private String event;
    
    /**
     * 任务 ID，用于请求跟踪和停止响应接口
     */
    private String taskId;
    
    /**
     * 唯一ID
     */
    private String id;
    
    /**
     * 消息唯一 ID
     */
    private String messageId;
    
    /**
     * 会话 ID
     */
    private String conversationId;
    
    /**
     * App 模式，固定为 chat
     */
    private String mode;
    
    /**
     * 完整回复内容
     */
    private String answer;
    
    /**
     * 元数据
     */
    private Metadata metadata;
    
    /**
     * 模型用量信息
     */
    private Usage usage;
    
    /**
     * 引用和归属分段列表
     */
    private List<RetrieverResource> retrieverResources;
    
    /**
     * 消息创建时间戳，如：1705395332
     */
    private Long createdAt;
}