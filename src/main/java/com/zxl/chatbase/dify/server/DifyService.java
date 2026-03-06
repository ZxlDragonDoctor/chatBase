package com.zxl.chatbase.dify.server;


import com.zxl.chatbase.dify.model.request.DifyChatRequest;
import com.zxl.chatbase.dify.model.response.DifyChatResponse;



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
}