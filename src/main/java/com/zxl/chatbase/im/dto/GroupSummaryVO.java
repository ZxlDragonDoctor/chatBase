package com.zxl.chatbase.im.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupSummaryVO {
    private String platform;
    private String groupId;
    private String groupName;
    private Long messageCount;
    private LocalDateTime lastMessageTime;
}
