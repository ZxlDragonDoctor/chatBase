package com.zxl.chatbase.statistics.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TokenStatisticsVO {
    private Long totalTokens;
    private BigDecimal avgTokensPerConversation;
    private Integer totalConversations;
    private List<DailyTokenVO> dailyTokens;

    @Data
    public static class DailyTokenVO {
        private String date;
        private Long tokens;
        private Integer conversations;
    }
}