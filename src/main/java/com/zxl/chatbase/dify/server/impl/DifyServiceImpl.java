package com.zxl.chatbase.dify.server.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxl.chatbase.dify.config.DifyConfig;
import com.zxl.chatbase.dify.model.request.DifyChatRequest;
import com.zxl.chatbase.dify.model.response.DifyChatResponse;
import com.zxl.chatbase.dify.model.response.DifyFileUploadResponse;
import com.zxl.chatbase.dify.server.DifyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;

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

    @Override
    public DifyFileUploadResponse uploadFile(MultipartFile file, String user) {


        log.info("开始上传文件到Dify: fileName={}, fileSize={}, user={}",
                file.getOriginalFilename(), file.getSize(), user);
        try {
            validateParams(file,user);
            // 创建HttpPost请求
            HttpPost httpPost = new HttpPost(difyConfig.getApiUrl() + "/files/upload");

            // 设置请求头 - 使用multipart/form-data，由HttpClient根据实体自动设置boundary
            httpPost.setHeader("Authorization", "Bearer " + difyConfig.getApiKey());

            // 构建multipart实体
            HttpEntity multipartEntity = MultipartEntityBuilder.create()
                    .setCharset(StandardCharsets.UTF_8)
                    // 添加文件部分
                    .addBinaryBody(
                            "file",
                            file.getBytes(),
                            ContentType.parse(Objects.requireNonNull(file.getContentType())),
                            file.getOriginalFilename()
                    )
                    // 添加user字段
                    .addTextBody("user", user, ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8))
                    .build();

            httpPost.setEntity(multipartEntity);
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                // 获取响应
                int statusCode = response.getStatusLine().getStatusCode();
                String jsonResponse = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                // 处理响应
                if (statusCode == 200) {

                    return objectMapper.readValue(jsonResponse, DifyFileUploadResponse.class);
                } else {
                    log.error("Dify API 文件上传失败: status={}, body={}", statusCode, jsonResponse);
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("上传文件到 Dify 失败", e);
            return null;
        }
    }

    /**
     * 参数文件校验
     */
    private void validateParams(MultipartFile file, String user) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        if (user == null || user.trim().isEmpty()) {
            throw new IllegalArgumentException("用户标识不能为空");
        }
        if (difyConfig.getApiKey() == null || difyConfig.getApiKey().trim().isEmpty()) {
            throw new IllegalArgumentException("API密钥未配置");
        }
    }

    @Override
    public String createDataset(String name, String description) {
        String url = difyConfig.getApiUrl() + "/datasets";
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Authorization", "Bearer " + difyConfig.getDatasetApiKey());
        httpPost.setHeader("Content-Type", "application/json");

        try {
            HashMap<String, Object> body = new HashMap<>();
            body.put("name", name);
            if (description != null && !description.trim().isEmpty()) {
                body.put("description", description);
            }
            // 使用高质量索引，权限仅自己可见
            body.put("indexing_technique", "high_quality");
            body.put("permission", "only_me");

            String json = objectMapper.writeValueAsString(body);
            httpPost.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String resp = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                int statusCode = response.getStatusLine().getStatusCode();
                log.info("Dify 创建知识库响应: status={}, body={}", statusCode, resp);
                if (statusCode == 200 || statusCode == 201) {
                    return objectMapper.readTree(resp).path("id").asText(null);
                } else {
                    log.error("创建 Dify 知识库失败: status={}, body={}", statusCode, resp);
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("调用 Dify 创建知识库接口异常", e);
            return null;
        }
    }

    @Override
    public String createDatasetDocument(String title, String content) {
        String datasetId = difyConfig.getDatasetId();
        if (datasetId == null || datasetId.trim().isEmpty()) {
            log.warn("未配置 difyApp.datasetId，跳过同步到知识库");
            return null;
        }

        // 按 Dify 知识库接口文档，纯文本创建文档的路径为：
        // POST /datasets/{dataset_id}/document/create-by-text
        String url = difyConfig.getApiUrl() + "/datasets/" + datasetId + "/document/create-by-text";
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Authorization", "Bearer " + difyConfig.getDatasetApiKey());
        httpPost.setHeader("Content-Type", "application/json");

        try {
            // 参考 Dify 数据集文档接口结构，构造一个简单的手动文本文档
            HashMap<String, Object> body = new HashMap<>();
            body.put("name", title);
            body.put("indexing_technique", "high_quality");
            body.put("text", content);

            String json = objectMapper.writeValueAsString(body);
            httpPost.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String resp = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                int statusCode = response.getStatusLine().getStatusCode();
                log.info("Dify 数据集文档创建响应: status={}, body={}", statusCode, resp);
                if (statusCode == 200 || statusCode == 201) {
                    // 文档创建成功，解析出 id
                    return objectMapper.readTree(resp).path("document").path("id").asText(null);
                } else {
                    log.error("创建 Dify 知识库文档失败: status={}, body={}", statusCode, resp);
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("调用 Dify 知识库文档接口异常", e);
            return null;
        }
    }
}