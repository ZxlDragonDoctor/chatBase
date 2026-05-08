package com.zxl.chatbase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxl.chatbase.dify.config.DifyConfig;
import com.zxl.chatbase.dify.model.response.BatchUploadResponse;
import com.zxl.chatbase.dify.server.DifyService;
import com.zxl.chatbase.kb.entity.KbCategory;
import com.zxl.chatbase.kb.entity.KbDocument;
import com.zxl.chatbase.kb.entity.KbKnowledgeBase;
import com.zxl.chatbase.kb.entity.KbUserCategoryMapping;
import com.zxl.chatbase.kb.service.IKbCategoryService;
import com.zxl.chatbase.kb.service.IKbDocumentService;
import com.zxl.chatbase.kb.service.IKbKnowledgeBaseService;
import com.zxl.chatbase.kb.service.IKbUserCategoryMappingService;
import com.zxl.chatbase.upload.service.UploadProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/kb")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final IKbKnowledgeBaseService knowledgeBaseService;
    private final IKbDocumentService documentService;
    private final IKbCategoryService categoryService;
    private final IKbUserCategoryMappingService categoryMappingService;
    private final DifyService difyService;
    private final UploadProgressService progressService;
    private final DifyConfig difyConfig;
    private final ObjectMapper objectMapper;
    private final ExecutorService uploadExecutor = Executors.newFixedThreadPool(5);
    @javax.annotation.Resource
    private CloseableHttpClient httpClient;

    @GetMapping("/category/tree")
    public List<KbCategory> categoryTree(@RequestAttribute("currentUser") String userId) {
        return categoryService.treeList(userId);
    }

    @GetMapping("/category/page")
    public Page<KbCategory> categoryPage(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestAttribute("currentUser") String userId) {
        return categoryService.pageList(name, pageNum, pageSize, userId);
    }

    @PostMapping("/category")
    public boolean createCategory(@RequestBody KbCategory category,
                                  @RequestAttribute("currentUser") String userId) {
        return categoryService.createCategory(category, userId);
    }

    @PutMapping("/category")
    public boolean updateCategory(@RequestBody KbCategory category,
                                  @RequestAttribute("currentUser") String userId) {
        return categoryService.updateCategory(category, userId);
    }

    @DeleteMapping("/category/{id}")
    public Map<String, Object> deleteCategory(@PathVariable Long id,
                                              @RequestAttribute("currentUser") String userId) {
        Map<String, Object> result = new HashMap<>();
        String errorMsg = categoryService.deleteCategory(id, userId);
        if (errorMsg != null) {
            result.put("success", false);
            result.put("message", errorMsg);
        } else {
            result.put("success", true);
            result.put("message", "删除成功");
        }
        return result;
    }

    @GetMapping("/page")
    public Page<KbKnowledgeBase> page(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestAttribute("currentUser") String userId) {
        return knowledgeBaseService.pageList(categoryId, name, pageNum, pageSize, userId);
    }

    @GetMapping("/admin/page")
    public Page<KbKnowledgeBase> pageForAdmin(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return knowledgeBaseService.pageAllForAdmin(categoryId, name, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    public KbKnowledgeBase getById(@PathVariable Long id) {
        return knowledgeBaseService.getById(id);
    }

    @PostMapping
    public boolean create(@RequestBody KbKnowledgeBase knowledgeBase,
                          @RequestAttribute("currentUser") String userId) {
        return knowledgeBaseService.createKnowledgeBase(knowledgeBase, userId);
    }

    @PutMapping
    public boolean update(@RequestBody KbKnowledgeBase knowledgeBase,
                          @RequestAttribute("currentUser") String userId) {
        return knowledgeBaseService.updateKnowledgeBase(knowledgeBase, userId);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id,
                          @RequestAttribute("currentUser") String userId) {
        return knowledgeBaseService.deleteKnowledgeBase(id, userId);
    }

    @PostMapping("/{id}/sync")
    public Map<String, Object> sync(@PathVariable Long id) {
        boolean success = knowledgeBaseService.syncDocumentsToDify(id);
        return Map.of("success", success, "message", success ? "同步成功" : "同步失败");
    }

    @PostMapping(value = "/{id}/batch-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> batchUploadFiles(
            @PathVariable Long id,
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart(value = "user", required = false) String user
    ) {
        log.info("批量上传请求: kbId={}, filesCount={}", id, files.size());

        KbKnowledgeBase kb = knowledgeBaseService.getById(id);
        if (kb == null) {
            log.warn("知识库不存在: {}", id);
            return Map.of("success", false, "message", "知识库不存在", "taskId", "");
        }

        String datasetId = kb.getDifyDatasetId();
        if (datasetId == null || datasetId.isBlank()) {
            log.warn("知识库未关联Dify Dataset: kbId={}, difyDatasetId={}", id, datasetId);
            return Map.of("success", false, "message", "知识库未关联Dify Dataset，请先同步到Dify", "taskId", "");
        }

        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        progressService.createProgress(taskId, files.size());
        log.info("创建上传任务: taskId={}, totalCount={}, datasetId={}", taskId, files.size(), datasetId);

        String uploadUser = (user != null && !user.isBlank()) ? user : "kb-upload";

        List<Map<String, Object>> fileDataList = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                Map<String, Object> fileData = new HashMap<>();
                fileData.put("fileName", file.getOriginalFilename());
                fileData.put("contentType", file.getContentType());
                fileData.put("bytes", file.getBytes());
                fileDataList.add(fileData);
            } catch (Exception e) {
                log.warn("读取文件失败: {}", file.getOriginalFilename(), e);
            }
        }

        uploadExecutor.submit(() -> {
            log.info("上传任务开始执行: taskId={}, filesCount={}", taskId, fileDataList.size());
            try {
                progressService.updateProgress(taskId, null, false, "开始上传", null);

                int successCount = 0;
                int failedCount = 0;

                for (Map<String, Object> fileData : fileDataList) {
                    String fileName = (String) fileData.get("fileName");
                    String contentType = (String) fileData.get("contentType");
                    byte[] bytes = (byte[]) fileData.get("bytes");

                    log.info("开始上传文件: taskId={}, fileName={}", taskId, fileName);
                    try {
                        boolean success = uploadSingleFile(fileName, contentType, bytes, uploadUser, datasetId);
                        if (success) {
                            successCount++;
                            progressService.updateProgress(taskId, fileName, true, "上传成功", null);
                            log.info("文件上传成功: taskId={}, fileName={}", taskId, fileName);
                        } else {
                            failedCount++;
                            progressService.updateProgress(taskId, fileName, false, "上传失败", null);
                            log.warn("文件上传失败: taskId={}, fileName={}", taskId, fileName);
                        }
                    } catch (Exception e) {
                        failedCount++;
                        log.error("上传文件异常: taskId={}, fileName={}", taskId, fileName, e);
                        progressService.updateProgress(taskId, fileName, false, "上传异常: " + e.getMessage(), null);
                    }
                }

                log.info("批量上传完成: taskId={}, success={}, failed={}", taskId, successCount, failedCount);
            } catch (Exception e) {
                log.error("批量上传异常: taskId={}", taskId, e);
                progressService.markFailed(taskId, e.getMessage());
            }
        });

        return Map.of("success", true, "taskId", taskId, "message", "上传任务已创建");
    }

    private boolean uploadSingleFile(String fileName, String contentType, byte[] bytes, String user, String datasetId) {
        try {
            log.info("上传文件到Dify: fileName={}, size={}bytes, user={}", fileName, bytes.length, user);

            HttpPost httpPost = new HttpPost(difyConfig.getApiUrl() + "/files/upload");
            httpPost.setHeader("Authorization", "Bearer " + difyConfig.getApiKey());

            ContentType ct = contentType != null ? ContentType.parse(contentType) : ContentType.APPLICATION_OCTET_STREAM;

            HttpEntity multipartEntity = MultipartEntityBuilder.create()
                    .setCharset(StandardCharsets.UTF_8)
                    .addBinaryBody("file", bytes, ct, fileName)
                    .addTextBody("user", user, ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8))
                    .build();

            httpPost.setEntity(multipartEntity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String jsonResponse = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                log.info("Dify文件上传响应: fileName={}, statusCode={}, body={}", fileName, statusCode, jsonResponse);

                if (statusCode == 200 || statusCode == 201) {
                    JsonNode jsonNode = objectMapper.readTree(jsonResponse);
                    String difyFileId = jsonNode.path("id").asText(null);

                    if (difyFileId != null) {
                        log.info("Dify文件ID获取成功: fileName={}, difyFileId={}", fileName, difyFileId);
                        return syncFileToDataset(fileName, contentType, bytes, datasetId, difyFileId);
                    } else {
                        log.warn("Dify响应缺少id字段: fileName={}, response={}", fileName, jsonResponse);
                    }
                } else {
                    log.warn("Dify文件上传失败: fileName={}, statusCode={}, body={}", fileName, statusCode, jsonResponse);
                }
                return false;
            }
        } catch (Exception e) {
            log.error("上传文件到Dify异常: fileName={}", fileName, e);
            return false;
        }
    }

    private boolean syncFileToDataset(String fileName, String contentType, byte[] bytes, String datasetId, String difyFileId) {
        try {
            String url = difyConfig.getApiUrl() + "/datasets/" + datasetId + "/document/create-by-file";
            log.info("同步文件到Dataset: fileName={}, url={}, datasetId={}, difyFileId={}", fileName, url, datasetId, difyFileId);

            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Bearer " + difyConfig.getDatasetApiKey());

            String jsonData = "{\"indexing_technique\":\"high_quality\",\"process_rule\":{\"mode\":\"automatic\"}}";

            ContentType ct = contentType != null ? ContentType.parse(contentType).withCharset(StandardCharsets.UTF_8) : ContentType.APPLICATION_OCTET_STREAM;

            HttpEntity multipartEntity = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .setCharset(StandardCharsets.UTF_8)
                    .addBinaryBody("file", bytes, ct, fileName)
                    .addTextBody("data", jsonData, ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8))
                    .build();

            httpPost.setEntity(multipartEntity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String respBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                log.info("同步文件响应: fileName={}, statusCode={}, body={}", fileName, statusCode, respBody);

                if (statusCode == 200 || statusCode == 201) {
                    log.info("文件同步成功: fileName={}, datasetId={}", fileName, datasetId);
                    return true;
                } else {
                    log.warn("文件同步失败: fileName={}, statusCode={}, body={}", fileName, statusCode, respBody);
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("同步文件到知识库异常: fileName={}", fileName, e);
            return false;
        }
    }

    @PostMapping("/sync-from-dify")
    public Map<String, Object> syncFromDify() {
        int count = knowledgeBaseService.syncFromDify();
        return Map.of("success", true, "count", count, "message", "同步完成，共处理 " + count + " 个知识库");
    }

    @GetMapping("/dify/list")
    public List<Map<String, Object>> listDifyDatasets() {
        var datasets = knowledgeBaseService.listDifyDatasets();
        return datasets.stream().map(d -> Map.<String, Object>of(
                "id", d.getId(),
                "name", d.getName() != null ? d.getName() : "",
                "description", d.getDescription() != null ? d.getDescription() : "",
                "documentCount", d.getDocumentCount() != null ? d.getDocumentCount() : 0
        )).toList();
    }

    @GetMapping("/{kbId}/document/page")
    public Page<KbDocument> documentPage(
            @PathVariable Long kbId,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return documentService.pageList(kbId, title, pageNum, pageSize);
    }

    @PostMapping("/document")
    public boolean createDocument(@RequestBody KbDocument document,
                                  @RequestAttribute("currentUser") String userId) {
        if (!knowledgeBaseService.canModifyKb(document.getKnowledgeBaseId(), userId)) {
            throw new RuntimeException("无权在此知识库创建文档");
        }
        return documentService.createDocument(document);
    }

    @PutMapping("/document")
    public boolean updateDocument(@RequestBody KbDocument document,
                                  @RequestAttribute("currentUser") String userId) {
        KbDocument existing = documentService.getById(document.getId());
        if (existing == null) throw new RuntimeException("文档不存在");
        if (!knowledgeBaseService.canModifyKb(existing.getKnowledgeBaseId(), userId)) {
            throw new RuntimeException("无权修改此文档");
        }
        return documentService.updateDocument(document);
    }

    @DeleteMapping("/document/{id}")
    public boolean deleteDocument(@PathVariable Long id,
                                  @RequestAttribute("currentUser") String userId) {
        KbDocument doc = documentService.getById(id);
        if (doc == null) throw new RuntimeException("文档不存在");
        if (!knowledgeBaseService.canModifyKb(doc.getKnowledgeBaseId(), userId)) {
            throw new RuntimeException("无权删除此文档");
        }
        return documentService.deleteDocumentWithDify(id);
    }

    @PostMapping("/document/{id}/sync")
    public Map<String, Object> syncDocument(@PathVariable Long id) {
        boolean success = documentService.syncToDify(id);
        return Map.of("success", success, "message", success ? "同步成功" : "同步失败");
    }

    @PostMapping("/{kbId}/link-category")
    public void linkCategory(@PathVariable Long kbId,
                             @RequestBody Map<String, Object> body,
                             @RequestAttribute("currentUser") String userId) {
        Long categoryId = Long.valueOf(body.get("categoryId").toString());
        categoryMappingService.link(kbId, categoryId, userId);
    }

    @DeleteMapping("/{kbId}/link-category/{mappingId}")
    public void unlinkCategory(@PathVariable Long kbId,
                               @PathVariable Long mappingId,
                               @RequestAttribute("currentUser") String userId) {
        categoryMappingService.unlink(mappingId, userId);
    }

    @GetMapping("/{kbId}/link-category/list")
    public List<KbUserCategoryMapping> listLinkedCategories(@PathVariable Long kbId) {
        return categoryMappingService.listByCategory(kbId);
    }
}