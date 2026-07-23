package com.zxl.chatbase.im.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BotInfoVO {

    private String platform;

    private String name;

    private String botId;

    private boolean online;

    private int groupCount;

    private int todayMessages;

    private int totalMessages;

    private LocalDateTime lastActiveTime;
}
