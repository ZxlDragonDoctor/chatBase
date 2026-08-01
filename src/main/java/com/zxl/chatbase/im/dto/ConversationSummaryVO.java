package com.zxl.chatbase.im.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationSummaryVO {
    private Long id;
    private String conversationId;
    private String platform;
    private String userId;
    private String userNickname;
    private String title;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Integer messageCount;
    private String createdBy;
    private Long appId;
    private String appName;
}
