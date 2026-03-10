package com.zxl.chatbase.chat;

import com.zxl.chatbase.dify.model.response.DifyChatResponse;

/**
 * 统一对话服务，封装群聊 / Web 与 Dify 的交互
 */
public interface ChatService {

    /**
     * 发送聊天消息，自动维护会话 ID（存储在 Redis）
     *
     * @param channel 渠道标识：im / web
     * @param userId  终端用户唯一标识
     * @param groupId 群聊 ID（web 渠道可为空）
     * @param query   用户问题
     * @return Dify 回复
     */
    DifyChatResponse chat(String channel, String userId, String groupId, String query);
}

