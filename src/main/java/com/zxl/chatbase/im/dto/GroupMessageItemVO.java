package com.zxl.chatbase.im.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupMessageItemVO {
    private Long id;
    private String platform;
    private String groupId;
    private String userId;
    private String messageType;
    private String rawMessage;
    private LocalDateTime messageTime;
    private Boolean synced;
}
