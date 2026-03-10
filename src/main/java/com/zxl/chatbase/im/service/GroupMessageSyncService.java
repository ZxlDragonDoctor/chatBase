package com.zxl.chatbase.im.service;

/**
 * 群消息同步到知识库的服务接口
 */
public interface GroupMessageSyncService {

    /**
     * 将未同步的群消息增量同步到知识库（例如 Dify 知识库）
     */
    void syncToKnowledgeBase();
}

