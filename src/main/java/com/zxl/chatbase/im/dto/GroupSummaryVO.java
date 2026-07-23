package com.zxl.chatbase.im.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupSummaryVO {
    private Long id;
    private String platform;
    private String groupId;
    private String groupName;
    private Long messageCount;
    private LocalDateTime lastMessageTime;
    private Long appId;
    private String appName;
    private String createdBy;
}
