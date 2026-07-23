package com.zxl.chatbase.statistics.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SystemOverviewVO {
    private Long totalMessages;
    private Long totalConversations;
    private Long totalTokens;
    private Integer activeGroups;
    private Integer activeUsers;
    private Integer knowledgeBases;
    private Integer documents;
    private BigDecimal avgLatencyMs;
    private BigDecimal successRate;
    private BotStatusVO bots;

    @Data
    public static class BotStatusVO {
        private Boolean qqEnabled;
        private String qqSelfId;
        private Boolean wecomEnabled;
        private String wecomCallbackPath;
        private Boolean wxEnabled;
        private String wxNickname;
    }
}