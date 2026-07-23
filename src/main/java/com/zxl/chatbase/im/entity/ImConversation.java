package com.zxl.chatbase.im.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_conversation")
public class ImConversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String platform;

    private String conversationId;

    private String title;

    private String userId;

    private String userNickname;

    private String conversationType;

    private String lastMessage;

    private LocalDateTime lastMessageTime;

    private Integer messageCount;

    private String createdBy;

    private Boolean status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
