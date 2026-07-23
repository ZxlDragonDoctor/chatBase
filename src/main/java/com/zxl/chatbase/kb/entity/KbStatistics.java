package com.zxl.chatbase.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("kb_statistics")
public class KbStatistics {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate statDate;

    private String channel;

    private Long knowledgeBaseId;

    private Long appId;

    private Integer conversationCount;

    private Integer messageCount;

    private Integer userCount;

    private BigDecimal avgTokens;

    private Long totalTokens;

    private Long totalPromptTokens;

    private Long totalCompletionTokens;

    private Integer avgLatencyMs;

    private BigDecimal totalCost;

    private BigDecimal avgCost;

    private Integer feedbackCount;

    private Integer positiveFeedback;

    private Integer negativeFeedback;

    private Integer docSyncCount;

    private LocalDateTime createTime;
}
