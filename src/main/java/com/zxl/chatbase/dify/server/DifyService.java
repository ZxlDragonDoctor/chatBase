package com.zxl.chatbase.dify.server;


import com.zxl.chatbase.dify.model.request.DifyChatRequest;
import com.zxl.chatbase.dify.model.response.DifyChatResponse;
import com.zxl.chatbase.dify.model.response.DifyFileUploadResponse;
import org.springframework.web.multipart.MultipartFile;


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
     * 将一段纯文本作为文档写入 Dify 知识库（Dataset）
     *
     * @param title   文档标题
     * @param content 文本内容
     * @return Dify 返回的文档ID（如果失败返回 null）
     */
    String createDatasetDocument(String title, String content);

    /**
     * 创建一个新的空知识库（Dataset）
     *
     * @param name        知识库名称
     * @param description 描述（可选）
     * @return 新建知识库的 ID（dataset_id），失败返回 null
     */
    String createDataset(String name, String description);

}