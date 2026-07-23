package com.zxl.chatbase.im.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群聊消息实体，用于采集 QQ 群消息
 */
@Data
@TableName("group_message")
public class GroupMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息来源平台，例如：qq / wecom / wx
     */
    private String platform;

    /**
     * 会话类型：group-群聊，single-单聊
     */
    private String conversationType;

    /**
     * 群 ID（平台原始 ID，群聊时必填）
     */
    private String groupId;

    /**
     * 会话ID（单聊时使用，格式：single:{platform}:{userId}）
     */
    private String conversationId;

    /**
     * 发送人 ID（平台原始 ID）
     */
    private String userId;

    /**
     * 平台消息 ID --》记录websocket返回的消息Id,其他平台无记录
     */
    private String messageId;

    /**
     * 消息类型：text / image / file 等
     */
    private String messageType;

    /**
     * 原始消息内容（包括 CQ 码）
     */
    private String rawMessage;

    /**
     * 消息发送时间（由 平台上报的时间戳转换）
     */
    private LocalDateTime messageTime;

    /**
     * 记录创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否已同步到知识库
     */
    private Boolean synced;

    /**
     * 在知识库中的文档ID（如果有）
     */
    private String kbDocumentId;

    /**
     * 文件URL（图片/文件的下载链接）
     */
    private String fileUrl;

    /**
     * 上传到Dify后的文件ID
     */
    private String difyFileId;

    /**
     * 文件名（如果有）
     */
    private String fileName;

}

