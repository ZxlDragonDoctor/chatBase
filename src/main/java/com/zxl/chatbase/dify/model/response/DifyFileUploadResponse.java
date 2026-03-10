package com.zxl.chatbase.dify.model.response;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 文件上传响应DTO
 */
@Data
public class DifyFileUploadResponse {
    
    /**
     * 文件ID
     */
    private UUID id;
    
    /**
     * 文件名
     */
    private String name;
    
    /**
     * 文件大小（字节）
     */
    private Integer size;
    
    /**
     * 文件后缀
     */
    private String extension;
    
    /**
     * 文件mime-type
     */
    @JsonProperty("mime_type")
    private String mimeType;
    
    /**
     * 上传人ID
     */
    @JsonProperty("created_by")
    private UUID createdBy;
    
    /**
     * 上传时间
     */
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}