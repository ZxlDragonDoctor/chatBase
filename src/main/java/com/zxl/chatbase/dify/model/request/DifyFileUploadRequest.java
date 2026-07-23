package com.zxl.chatbase.dify.model.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传请求DTO
 */
@Data
public class DifyFileUploadRequest {
    
    /**
     * 要上传的文件
     */
    private MultipartFile file;
    
    /**
     * 用户标识，用于定义终端用户的身份
     * 必须和发送消息接口传入 user 保持一致
     */
    private String user;
}