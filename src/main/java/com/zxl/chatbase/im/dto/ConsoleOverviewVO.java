package com.zxl.chatbase.im.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ConsoleOverviewVO {
    private long totalMessages;
    private int distinctGroups;
    private Map<String, Long> messageCountByPlatform;
    private Map<String, Integer> groupCountByPlatform;
    private BotStatusVO bots;
}
