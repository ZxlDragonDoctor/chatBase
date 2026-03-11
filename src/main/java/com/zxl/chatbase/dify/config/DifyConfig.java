package com.zxl.chatbase.dify.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DifyConfig {
    
    @Value("${difyApp.url}")
    private String apiUrl;
    
    @Value("${difyApp.apiKey}")
    private String apiKey;
    
    @Value("${difyApp.timeOut}")
    private int timeout;

    @Value("${difyApp.datasetApiKey}")
    private String datasetApiKey;

    /**
     * 可选：Dify 知识库 ID（dataset_id），用于存储群聊消息
     */
    @Value("${difyApp.datasetId:}")
    private String datasetId;

    // Getters
    public String getApiUrl() { return apiUrl; }
    public String getApiKey() { return apiKey; }
    public int getTimeout() { return timeout; }
    public String getDatasetId() { return datasetId; }

    public String getDatasetApiKey() {
        return datasetApiKey;
    }
}