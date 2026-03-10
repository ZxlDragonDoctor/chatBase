package com.zxl.chatbase.dify.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FileInfo {
    
    /**
     * 文件类型: image, document, audio, video 等
     */
    private String type;
    
    /**
     * 传输方式: remote_url 或 local_file
     */
    private String transferMethod;
    
    /**
     * 文件URL（当transferMethod为remote_url时）
     */
    private String url;
    
    /**
     * 上传文件ID（当transferMethod为local_file时）
     */
    private String uploadFileId;
}