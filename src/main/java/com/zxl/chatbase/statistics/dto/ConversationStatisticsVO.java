package com.zxl.chatbase.statistics.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ConversationStatisticsVO {
    private Long totalConversations;
    private Long successfulConversations;
    private Long failedConversations;
    private BigDecimal successRate;
    private BigDecimal avgLatencyMs;
    private List<DailyConversationVO> dailyTrend;

    @Data
    public static class DailyConversationVO {
        private String date;
        private Integer count;
        private Integer successCount;
        private Long tokens;
    }
}