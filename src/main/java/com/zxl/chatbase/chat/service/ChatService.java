package com.zxl.chatbase.chat.service;

import com.zxl.chatbase.dify.model.request.FileInfo;
import com.zxl.chatbase.dify.model.response.DifyChatResponse;
import java.util.List;

/**
 * 统一对话服务，封装群聊 / Web 与 Dify 的交互
 */
public interface ChatService {

    /**
     * 发送聊天消息，自动维护会话 ID（存储在 Redis）
     *
     * @param channel 渠道标识：im / web / wx
     * @param userId  终端用户唯一标识
     * @param groupId 群聊 ID（web 渠道可为空）
     * @param query   用户问题
     * @return Dify 回复
     */
    DifyChatResponse chat(String channel, String userId, String groupId, String query);

    /**
     * 发送聊天消息（支持携带文件）
     *
     * @param channel 渠道标识：im / web
     * @param userId  终端用户唯一标识
     * @param groupId 群聊 ID（web 渠道可为空）
     * @param query   用户问题
     * @param files   附件列表（支持 local_file / remote_url）
     * @return Dify 回复
     */
    DifyChatResponse chat(String channel, String userId, String groupId, String query, List<FileInfo> files);

    /**
     * 发送聊天消息（指定会话ID）
     *
     * @param sessionId 本地会话ID
     * @param channel   渠道标识
     * @param userId    用户标识
     * @param query     用户问题
     * @param files     附件列表
     * @return Dify 回复
     */
    DifyChatResponse chatWithSession(String sessionId, String channel, String userId, String query, List<FileInfo> files);
}

