package com.zxl.chatbase.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_session")
public class ChatSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private String userId;

    private String channel;

    private String title;

    private String difyConversationId;

    private Integer messageCount;

    private LocalDateTime lastMessageTime;

    private Boolean status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}