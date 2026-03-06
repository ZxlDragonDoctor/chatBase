package com.zxl.chatbase.dify.server.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxl.chatbase.dify.config.DifyConfig;
import com.zxl.chatbase.dify.model.request.DifyChatRequest;
import com.zxl.chatbase.dify.model.response.DifyChatResponse;
import com.zxl.chatbase.dify.server.DifyService;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@Slf4j
@Service
public class DifyServiceImpl implements DifyService {
    
    @Autowired
    private DifyConfig difyConfig;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private final CloseableHttpClient httpClient;
    
    public DifyServiceImpl() {
        // 配置HttpClient
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(30000)
                .setSocketTimeout(30000)
                .build();
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build();
    }
    
    @Override
    public DifyChatResponse sendChatMessage(String query, String conversationId, String userId) {
        // 构造请求对象
        DifyChatRequest request = new DifyChatRequest();
        request.setQuery(query);
        request.setConversationId(conversationId);
        request.setUser(userId);
        request.setResponseMode("blocking");  // 第一阶段先用阻塞模式
        request.setInputs(new HashMap<>());   // 空变量
        
        return sendChatMessage(request);
    }
    
    @Override
    public DifyChatResponse sendChatMessage(DifyChatRequest request) {
        String url = difyConfig.getApiUrl() + "/chat-messages";
        
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Authorization", "Bearer " + difyConfig.getApiKey());
        httpPost.setHeader("Content-Type", "application/json");
        
        try {
            // 序列化请求体
            String jsonRequest = objectMapper.writeValueAsString(request);
            log.info("Dify请求: {}", jsonRequest);
            
            httpPost.setEntity(new StringEntity(jsonRequest, StandardCharsets.UTF_8));
            
            // 发送请求
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String jsonResponse = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                log.info("Dify响应: {}", jsonResponse);
                
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    // 解析响应
                    return objectMapper.readValue(jsonResponse, DifyChatResponse.class);
                } else {
                    log.error("Dify API错误: status={}, body={}", statusCode, jsonResponse);
                    DifyChatResponse errorResponse = new DifyChatResponse();
                    errorResponse.setAnswer("【系统错误】调用Dify API失败，状态码：" + statusCode);
                    return errorResponse;
                }
            }
            
        } catch (Exception e) {
            log.error("调用Dify API异常", e);
            DifyChatResponse errorResponse = new DifyChatResponse();
            errorResponse.setAnswer("【系统错误】" + e.getMessage());
            return errorResponse;
        }
    }
}