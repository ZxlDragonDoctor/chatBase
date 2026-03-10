package com.zxl.chatbase.dify.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DifyChatRequest {
    
    /**
     * 输入参数
     * 必填，{}
     */
    private Map<String, Object> inputs;
    
    /**
     * 用户查询内容
     */
    private String query;

    /**
     * 响应模式: streaming 或 blocking
     */
    private String responseMode;
    
    /**
     * 会话ID，空字符串表示新会话
     */
    private String conversationId;
    
    /**
     * 用户标识
     */
    private String user;
    
    /**
     * 文件列表
     */
    private List<FileInfo> files;
    
    /**
     * 是否自动生成标题
     */
    private Boolean autoGenerateName = true; //默认为TRUE,必须赋值
}