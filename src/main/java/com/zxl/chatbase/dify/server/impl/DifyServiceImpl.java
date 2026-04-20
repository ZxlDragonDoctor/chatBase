package com.zxl.chatbase.dify.server.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxl.chatbase.dify.config.DifyConfig;
import com.zxl.chatbase.dify.model.request.DifyChatRequest;
import com.zxl.chatbase.dify.model.response.BatchUploadResponse;
import com.zxl.chatbase.dify.model.response.DifyChatResponse;
import com.zxl.chatbase.dify.model.response.DifyDatasetResponse;
import com.zxl.chatbase.dify.model.response.DifyDocumentResponse;
import com.zxl.chatbase.dify.model.response.DifyFileUploadResponse;
import com.zxl.chatbase.kb.entity.KbFile;
import com.zxl.chatbase.kb.service.FileService;
import com.zxl.chatbase.dify.server.DifyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class DifyServiceImpl implements DifyService {

    @Autowired
    private DifyConfig difyConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FileService fileService;

    private CloseableHttpClient httpClient;

    /**
     * Spring Bean 的生命周期中：
     * 构造函数执行 → 此时 @Autowired 字段还未注入
     * 属性赋值 → @Autowired 注解的字段才被注入
     */
    @PostConstruct
    public void init() {
        int timeoutMs = Math.max(difyConfig.getTimeout(), 180) * 1000;
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeoutMs)
                .setSocketTimeout(timeoutMs)
                .setConnectionRequestTimeout(timeoutMs)
                .setCookieSpec(CookieSpecs.STANDARD)
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
        request.setUser((userId == null || userId.trim().isEmpty()) ? "abc-123" : userId);
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
            // Dify 要求 user 必填
            if (request.getUser() == null || request.getUser().trim().isEmpty()) {
                request.setUser("abc-123");
            }
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
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("timed out")) {
                errorResponse.setAnswer("【系统繁忙】大模型回答超时，请稍后再试");
            } else {
                errorResponse.setAnswer("【系统错误】" + e.getMessage());
            }
            return errorResponse;
        }
    }

    @Override
    public DifyFileUploadResponse uploadFile(MultipartFile file, String user) {


        log.info("开始上传文件到Dify: fileName={}, fileSize={}, user={}",
                file.getOriginalFilename(), file.getSize(), user);
        try {
            validateParams(file,user);

            DifyFileUploadResponse difyResp = uploadFileToDify(file, user);

            if (difyResp != null && difyResp.getId() != null) {
                saveToKbFile(file, user, difyResp.getId().toString());
                syncFileToDataset(file, user);
            }

            return difyResp;
        } catch (Exception e) {
            log.error("上传文件到 Dify 失败", e);
            return null;
        }
    }

    @Override
    public BatchUploadResponse batchUploadFiles(List<MultipartFile> files, String user, String datasetId) {
        BatchUploadResponse response = new BatchUploadResponse();
        List<BatchUploadResponse.FileUploadResult> results = new ArrayList<>();
        
        if (files == null || files.isEmpty()) {
            response.setTotalCount(0);
            response.setSuccessCount(0);
            response.setFailedCount(0);
            response.setResults(results);
            return response;
        }

        response.setTotalCount(files.size());
        int successCount = 0;
        int failedCount = 0;

        // 分批处理，每批最多5个文件
        int batchSize = 5;
        for (int i = 0; i < files.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, files.size());
            List<MultipartFile> batch = files.subList(i, endIndex);
            
            log.info("批量上传: 处理第 {} 到 {} 个文件", i + 1, endIndex);
            
            for (MultipartFile file : batch) {
                try {
                    BatchUploadResponse.FileUploadResult result = uploadSingleFileToDataset(file, user, datasetId);
                    results.add(result);
                    if (result.isSuccess()) {
                        successCount++;
                    } else {
                        failedCount++;
                    }
                } catch (Exception e) {
                    log.error("上传文件失败: {}", file.getOriginalFilename(), e);
                    results.add(BatchUploadResponse.FileUploadResult.fail(
                            file.getOriginalFilename(), 
                            "上传异常: " + e.getMessage()
                    ));
                    failedCount++;
                }
            }
        }

        response.setSuccessCount(successCount);
        response.setFailedCount(failedCount);
        response.setResults(results);
        
        log.info("批量上传完成: 总数={}, 成功={}, 失败={}", 
                files.size(), successCount, failedCount);
        
        return response;
    }

    private BatchUploadResponse.FileUploadResult uploadSingleFileToDataset(
            MultipartFile file, String user, String datasetId) {
        String fileName = file.getOriginalFilename();
        
        try {
            validateParams(file, user);
            
            // 1. 上传文件到Dify
            DifyFileUploadResponse difyResp = uploadFileToDify(file, user);
            if (difyResp == null || difyResp.getId() == null) {
                return BatchUploadResponse.FileUploadResult.fail(fileName, "上传到Dify失败");
            }
            
            String difyFileId = difyResp.getId().toString();
            
            // 2. 保存文件记录
            saveToKbFile(file, user, difyFileId);
            
            // 3. 同步到指定知识库
            if (datasetId != null && !datasetId.trim().isEmpty()) {
                boolean syncSuccess = syncFileToSpecificDataset(file, datasetId);
                if (!syncSuccess) {
                    return BatchUploadResponse.FileUploadResult.fail(fileName, "同步到知识库失败");
                }
            }
            
            return BatchUploadResponse.FileUploadResult.success(fileName, difyFileId);
            
        } catch (Exception e) {
            log.error("上传文件异常: {}", fileName, e);
            return BatchUploadResponse.FileUploadResult.fail(fileName, "上传异常: " + e.getMessage());
        }
    }

    private boolean syncFileToSpecificDataset(MultipartFile file, String datasetId) {
        try {
            String url = difyConfig.getApiUrl() + "/datasets/" + datasetId + "/document/create-by-file";
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Bearer " + difyConfig.getDatasetApiKey());

            String jsonData = "{\"indexing_technique\":\"high_quality\",\"process_rule\":{\"mode\":\"automatic\"}}";

            String fileName = file.getOriginalFilename();
            ContentType contentType = ContentType.parse(Objects.requireNonNull(file.getContentType()));
            contentType = contentType.withCharset(StandardCharsets.UTF_8);

            HttpEntity multipartEntity = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .setCharset(StandardCharsets.UTF_8)
                    .addBinaryBody("file", file.getBytes(), contentType, fileName)
                    .addTextBody("data", jsonData, ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8))
                    .build();

            httpPost.setEntity(multipartEntity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String resp = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (statusCode == 200 || statusCode == 201) {
                    String docId = objectMapper.readTree(resp).path("document").path("id").asText(null);
                    log.info("文件同步到知识库成功: fileName={}, datasetId={}, documentId={}", 
                            fileName, datasetId, docId);
                    return true;
                } else {
                    log.warn("同步文件到知识库失败: status={}, body={}", statusCode, resp);
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("同步文件到知识库异常: fileName={}", file.getOriginalFilename(), e);
            return false;
        }
    }

    private DifyFileUploadResponse uploadFileToDify(MultipartFile file, String user) throws Exception {
        HttpPost httpPost = new HttpPost(difyConfig.getApiUrl() + "/files/upload");
        httpPost.setHeader("Authorization", "Bearer " + difyConfig.getApiKey());

        HttpEntity multipartEntity = MultipartEntityBuilder.create()
                .setCharset(StandardCharsets.UTF_8)
                .addBinaryBody("file", file.getBytes(), ContentType.parse(Objects.requireNonNull(file.getContentType())), file.getOriginalFilename())
                .addTextBody("user", user, ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8))
                .build();

        httpPost.setEntity(multipartEntity);

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String jsonResponse = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (statusCode == 200 || statusCode == 201) {
                return objectMapper.readValue(jsonResponse, DifyFileUploadResponse.class);
            } else {
                log.error("Dify API 文件上传失败: status={}, body={}", statusCode, jsonResponse);
                return null;
            }
        }
    }

    private void saveToKbFile(MultipartFile file, String user, String difyFileId) {
        try {
            KbFile kbFile = new KbFile();
            kbFile.setFileName(file.getOriginalFilename());
            kbFile.setFilePath("dify://" + difyFileId);
            kbFile.setFileSize(file.getSize());
            kbFile.setFileType(file.getContentType());
            kbFile.setBucket("dify");
            kbFile.setSource("dify_upload");
            kbFile.setSourceId(difyFileId);
            kbFile.setUploadUserId(user);
            kbFile.setDifyFileId(difyFileId);
            kbFile.setStatus(true);
            kbFile.setCreateTime(java.time.LocalDateTime.now());
            fileService.saveKbFile(kbFile);
            log.info("文件记录保存成功: difyFileId={}, fileName={}", difyFileId, file.getOriginalFilename());
        } catch (Exception e) {
            log.warn("保存文件记录失败，不影响上传: difyFileId={}", difyFileId, e);
        }
    }

    private void syncFileToDataset(MultipartFile file, String user) {
        String datasetId = difyConfig.getDatasetId();
        if (datasetId == null || datasetId.trim().isEmpty()) {
            log.warn("未配置 datasetId，跳过自动同步到知识库");
            return;
        }

        try {
            String url = difyConfig.getApiUrl() + "/datasets/" + datasetId + "/document/create-by-file";
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Bearer " + difyConfig.getDatasetApiKey());

            String jsonData = "{\"indexing_technique\":\"high_quality\",\"process_rule\":{\"mode\":\"automatic\"}}";

            HttpEntity multipartEntity = MultipartEntityBuilder.create()
                    .setCharset(StandardCharsets.UTF_8)
                    .addBinaryBody("file", file.getBytes(), ContentType.parse(Objects.requireNonNull(file.getContentType())), file.getOriginalFilename())
                    .addTextBody("data", jsonData, ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8))
                    .build();

            httpPost.setEntity(multipartEntity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String resp = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (statusCode == 200 || statusCode == 201) {
                    String docId = objectMapper.readTree(resp).path("document").path("id").asText(null);
                    log.info("文件同步到知识库成功: fileName={}, datasetId={}, documentId={}", file.getOriginalFilename(), datasetId, docId);
                } else {
                    log.warn("同步文件到知识库失败: status={}, body={}", statusCode, resp);
                }
            }
        } catch (Exception e) {
            log.warn("同步文件到知识库异常: fileName={}", file.getOriginalFilename(), e);
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

    @Override
    public boolean updateDatasetDocument(String documentId, String name, String content) {
        String datasetId = difyConfig.getDatasetId();
        if (datasetId == null || datasetId.trim().isEmpty()) {
            log.warn("未配置 difyApp.datasetId，无法更新文档");
            return false;
        }
        if (documentId == null || documentId.trim().isEmpty()) {
            log.warn("文档ID为空，无法更新");
            return false;
        }

        // POST /datasets/{dataset_id}/documents/{document_id}/update-by-text
        String url = difyConfig.getApiUrl() + "/datasets/" + datasetId + "/documents/" + documentId + "/update-by-text";
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Authorization", "Bearer " + difyConfig.getDatasetApiKey());
        httpPost.setHeader("Content-Type", "application/json");

        try {
            HashMap<String, Object> body = new HashMap<>();
            body.put("name", name);
            body.put("text", content);

            String json = objectMapper.writeValueAsString(body);
            httpPost.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String resp = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                int statusCode = response.getStatusLine().getStatusCode();
                log.info("Dify 文档更新响应: status={}, body={}", statusCode, resp);
                if (statusCode == 200 || statusCode == 201) {
                    log.info("Dify 文档更新成功: documentId={}", documentId);
                    return true;
                } else {
                    log.error("更新 Dify 文档失败: documentId={}, status={}, body={}", documentId, statusCode, resp);
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("调用 Dify 更新文档接口异常: documentId={}", documentId, e);
            return false;
        }
    }

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
    public List<DifyDatasetResponse> listDatasets() {
        String url = difyConfig.getApiUrl() + "/datasets?page=1&limit=100";
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Authorization", "Bearer " + difyConfig.getDatasetApiKey());

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String jsonResponse = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            log.info("获取Dify知识库列表: status={}, body={}", statusCode, jsonResponse);

            if (statusCode == 200) {
                DifyDatasetResponse.DifyDatasetListResponse listResponse = objectMapper.readValue(
                        jsonResponse,
                        DifyDatasetResponse.DifyDatasetListResponse.class
                );
                return listResponse.getData() != null ? listResponse.getData() : Collections.emptyList();
            } else {
                log.error("获取Dify知识库列表失败: status={}, body={}", statusCode, jsonResponse);
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("获取Dify知识库列表异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<DifyDocumentResponse> listDatasetDocuments(String datasetId) {
        if (datasetId == null || datasetId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String url = difyConfig.getApiUrl() + "/datasets/" + datasetId + "/documents?page=1&limit=100";
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Authorization", "Bearer " + difyConfig.getDatasetApiKey());

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String jsonResponse = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            log.info("获取Dify知识库文档列表: datasetId={}, status={}", datasetId, statusCode);

            if (statusCode == 200) {
                DifyDocumentResponse.DifyDocumentListResponse listResponse = objectMapper.readValue(
                        jsonResponse,
                        DifyDocumentResponse.DifyDocumentListResponse.class
                );
                return listResponse.getData() != null ? listResponse.getData() : Collections.emptyList();
            } else {
                log.error("获取Dify知识库文档列表失败: datasetId={}, status={}, body={}", datasetId, statusCode, jsonResponse);
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("获取Dify知识库文档列表异常: datasetId={}", datasetId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean deleteDatasetDocument(String datasetId, String documentId) {
        if (datasetId == null || datasetId.trim().isEmpty() || documentId == null || documentId.trim().isEmpty()) {
            log.warn("删除Dify文档参数无效: datasetId={}, documentId={}", datasetId, documentId);
            return false;
        }

        String url = difyConfig.getApiUrl() + "/datasets/" + datasetId + "/documents/" + documentId;
        HttpDelete httpDelete = new HttpDelete(url);
        httpDelete.setHeader("Authorization", "Bearer " + difyConfig.getDatasetApiKey());

        try (CloseableHttpResponse response = httpClient.execute(httpDelete)) {
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String jsonResponse = "";
            if (entity != null) {
                jsonResponse = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            }
            EntityUtils.consumeQuietly(entity);
            
            log.info("删除Dify文档: datasetId={}, documentId={}, status={}", datasetId, documentId, statusCode);

            if (statusCode == 200 || statusCode == 204) {
                log.info("Dify文档删除成功: datasetId={}, documentId={}", datasetId, documentId);
                return true;
            } else {
                log.error("删除Dify文档失败: datasetId={}, documentId={}, status={}, body={}", datasetId, documentId, statusCode, jsonResponse);
                return false;
            }
        } catch (Exception e) {
            log.error("删除Dify文档异常: datasetId={}, documentId={}", datasetId, documentId, e);
            return false;
        }
    }
}