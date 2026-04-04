package com.zxl.chatbase.im.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zxl.chatbase.im.entity.GroupMessage;
import org.springframework.stereotype.Service;

/**
 * 群消息同步到知识库的服务接口
 */
@Service
public interface GroupMessageSyncService  extends IService<GroupMessage> {

    /**
     * 将未同步的群消息增量同步到知识库（例如 Dify 知识库）
     */
    void syncToKnowledgeBase();

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
     * 保存web消息
     * @param userId
     * @param rawMessage
     * @param messageType
     * @param time
     */
    void saveGroupMessage(String userId, String rawMessage, String messageType, long time);
}

