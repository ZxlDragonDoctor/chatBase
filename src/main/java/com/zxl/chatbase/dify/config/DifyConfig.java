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

    // Getters
    public String getApiUrl() { return apiUrl; }
    public String getApiKey() { return apiKey; }
    public int getTimeout() { return timeout; }
}