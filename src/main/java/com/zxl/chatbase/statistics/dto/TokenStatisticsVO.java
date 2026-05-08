package com.zxl.chatbase.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
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