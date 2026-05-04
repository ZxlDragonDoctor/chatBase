package com.zxl.chatbase.im.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zxl.chatbase.im.entity.GroupMessage;


/**
 * 群消息同步到知识库的服务接口
 */
public interface GroupMessageSyncService  extends IService<GroupMessage> {

    /**
     * 将未同步的群消息增量同步到知识库（例如 Dify 知识库）
     * @deprecated 建议使用 Redis Stream 消息队列方案（GroupMessageConsumer）
     */
    @Deprecated(since = "2026-05-04")
    void syncToKnowledgeBase();

    /**
     * 同步单条消息到知识库
     * 供 Redis Stream 消费者调用
     * @param messageId 消息ID
     */
    void syncSingleMessage(Long messageId);

    /**
     * 保存qq群聊消息
     * @param messageId
     * @param groupId
     * @param userId
     * @param rawMessage
     * @param messageType
     * @param time
     */
    void saveGroupMessage(String messageId, String groupId, String userId,
                          String rawMessage, String messageType, long time);

    /**
     * 保存群聊消息（包含文件信息）
     * @param platform 平台标识：qq / wecom
     */
    void saveGroupMessage(String platform, String messageId, String groupId, String userId,
                          String rawMessage, String messageType, long time,
                          String fileUrl, String fileName);

    /**
     * 保存web消息，无具体群聊id
     * @param userId
     * @param rawMessage
     * @param messageType
     * @param time
     */
    void saveGroupMessage(String userId, String rawMessage, String messageType, long time);
}   

