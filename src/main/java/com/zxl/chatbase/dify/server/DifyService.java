package com.zxl.chatbase.dify.server;


import com.zxl.chatbase.dify.model.request.DifyChatRequest;
import com.zxl.chatbase.dify.model.response.BatchUploadResponse;
import com.zxl.chatbase.dify.model.response.DifyChatResponse;
import com.zxl.chatbase.dify.model.response.DifyDatasetResponse;
import com.zxl.chatbase.dify.model.response.DifyDocumentResponse;
import com.zxl.chatbase.dify.model.response.DifyFileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface DifyService {
    
    /**
     * 发送聊天消息（阻塞模式）
     * @param query 用户问题
     * @param conversationId 会话ID（新对话传空）
     * @param userId 用户标识
     * @return Dify回复
     */
    DifyChatResponse sendChatMessage(String query, String conversationId, String userId);
    
    /**
     * 发送聊天消息（带inputs变量）
     */
    DifyChatResponse sendChatMessage(DifyChatRequest request);

    /**
     * 发送聊天消息（流式模式，避免超时）
     */
    DifyChatResponse sendChatMessageStream(DifyChatRequest request);

    /**
     * 发送聊天消息（使用指定API Key）
     */
    DifyChatResponse sendChatMessage(DifyChatRequest request, String apiKey);

    /**
     * 发送聊天消息（流式模式，使用指定API Key）
     */
    DifyChatResponse sendChatMessageStream(DifyChatRequest request, String apiKey);

    /**
     * 上传文件
     *
     * @param file 要上传的文件
     * @param user 用户标识
     * @return 文件上传响应
     */
    DifyFileUploadResponse uploadFile(MultipartFile file, String user);

    /**
     * 上传文件（带默认用户标识）
     *
     * @param file 要上传的文件
     * @return 文件上传响应
     */
    default DifyFileUploadResponse uploadFile(MultipartFile file) {
        return uploadFile(file, "abc-123");
    }

    /**
     * 批量上传文件到Dify知识库
     * 由于Dify限制每次最多上传5个文件，会自动分批处理
     *
     * @param files      文件列表
     * @param user       用户标识
     * @param datasetId  目标知识库ID
     * @return 批量上传结果
     */
    BatchUploadResponse batchUploadFiles(List<MultipartFile> files, String user, String datasetId);

    /**
     * 批量上传文件到Dify知识库（带默认用户标识）
     *
     * @param files      文件列表
     * @param datasetId  目标知识库ID
     * @return 批量上传结果
     */
    default BatchUploadResponse batchUploadFiles(List<MultipartFile> files, String datasetId) {
        return batchUploadFiles(files, "abc-123", datasetId);
    }

    /**
     * 将一段纯文本作为文档写入 Dify 知识库（Dataset）
     *
     * @param title   文档标题
     * @param content 文本内容
     * @return Dify 返回的文档ID（如果失败返回 null）
     */
    String createDatasetDocument(String title, String content);


    /**
     * 用文本更新 Dify 知识库中的已有文档
     *
     * @param documentId 文档ID
     * @param name       文档名称
     * @param content    文本内容
     * @return 是否更新成功
     */
    boolean updateDatasetDocument(String documentId, String name, String content);

    /**
     * 创建一个新的空知识库（Dataset）
     *
     * @param name        知识库名称
     * @param description 描述（可选）
     * @return 新建知识库的 ID（dataset_id），失败返回 null
     */
    String createDataset(String name, String description);

    /**
     * 获取Dify中已创建的知识库列表
     *
     * @return Dify知识库列表
     */
    List<DifyDatasetResponse> listDatasets();

    /**
     * 获取Dify知识库中的文档列表
     *
     * @param datasetId 知识库ID
     * @return 文档列表
     */
    List<DifyDocumentResponse> listDatasetDocuments(String datasetId);

    /**
     * 删除Dify知识库中的文档
     *
     * @param datasetId 知识库ID
     * @param documentId 文档ID
     * @return 是否删除成功
     */
    boolean deleteDatasetDocument(String datasetId, String documentId);

    boolean deleteDataset(String datasetId);

}