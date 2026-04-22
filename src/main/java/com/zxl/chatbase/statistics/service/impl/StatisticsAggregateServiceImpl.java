package com.zxl.chatbase.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zxl.chatbase.kb.entity.KbConversation;
import com.zxl.chatbase.kb.entity.KbStatistics;
import com.zxl.chatbase.kb.mapper.KbConversationMapper;
import com.zxl.chatbase.kb.mapper.KbStatisticsMapper;
import com.zxl.chatbase.statistics.service.IStatisticsAggregateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsAggregateServiceImpl implements IStatisticsAggregateService {

    private final KbConversationMapper conversationMapper;
    private final KbStatisticsMapper statisticsMapper;

    @Override
    @Scheduled(cron = "0 5 0 * * ?")
    public void aggregateYesterdayStatistics() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        aggregateDailyStatistics(yesterday);
        log.info("每日统计聚合完成: {}", yesterday);
    }

    @Override
    @Transactional
    public void aggregateDailyStatistics(LocalDate date) {
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.plusDays(1).atStartOfDay();

        LambdaQueryWrapper<KbConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(KbConversation::getCreateTime, startTime);
        wrapper.lt(KbConversation::getCreateTime, endTime);
        wrapper.eq(KbConversation::getStatus, true);

        List<KbConversation> conversations = conversationMapper.selectList(wrapper);

        if (conversations.isEmpty()) {
            saveEmptyStatistics(date, "all");
            return;
        }

        int conversationCount = conversations.size();
        long totalTokens = conversations.stream()
                .filter(c -> c.getTokens() != null)
                .mapToLong(KbConversation::getTokens)
                .sum();
        
        long totalPromptTokens = conversations.stream()
                .filter(c -> c.getPromptTokens() != null)
                .mapToLong(KbConversation::getPromptTokens)
                .sum();
        
        long totalCompletionTokens = conversations.stream()
                .filter(c -> c.getCompletionTokens() != null)
                .mapToLong(KbConversation::getCompletionTokens)
                .sum();
        
        BigDecimal totalCost = conversations.stream()
                .filter(c -> c.getTotalPrice() != null)
                .map(KbConversation::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal avgTokens = conversationCount > 0
                ? BigDecimal.valueOf(totalTokens).divide(BigDecimal.valueOf(conversationCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal avgCost = conversationCount > 0
                ? totalCost.divide(BigDecimal.valueOf(conversationCount), 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        double avgLatency = conversations.stream()
                .filter(c -> c.getLatencyMs() != null)
                .mapToInt(KbConversation::getLatencyMs)
                .average()
                .orElse(0);

        Set<String> users = new HashSet<>();
        conversations.forEach(c -> {
            if (c.getUserId() != null) users.add(c.getUserId());
        });

        KbStatistics stats = new KbStatistics();
        stats.setStatDate(date);
        stats.setChannel("all");
        stats.setConversationCount(conversationCount);
        stats.setMessageCount(conversationCount);
        stats.setUserCount(users.size());
        stats.setTotalTokens(totalTokens);
        stats.setTotalPromptTokens(totalPromptTokens);
        stats.setTotalCompletionTokens(totalCompletionTokens);
        stats.setAvgTokens(avgTokens);
        stats.setTotalCost(totalCost);
        stats.setAvgCost(avgCost);
        stats.setAvgLatencyMs((int) avgLatency);
        stats.setCreateTime(LocalDateTime.now());

        LambdaQueryWrapper<KbStatistics> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(KbStatistics::getStatDate, date);
        existWrapper.eq(KbStatistics::getChannel, "all");
        
        KbStatistics existing = statisticsMapper.selectOne(existWrapper);
        if (existing != null) {
            stats.setId(existing.getId());
            statisticsMapper.updateById(stats);
        } else {
            statisticsMapper.insert(stats);
        }

        log.info("统计聚合: date={}, conversations={}, totalTokens={}, promptTokens={}, completionTokens={}, totalCost={}", 
                date, conversationCount, totalTokens, totalPromptTokens, totalCompletionTokens, totalCost);
    }

    @Override
    public void aggregateLast7Days() {
        for (int i = 1; i <= 7; i++) {
            LocalDate date = LocalDate.now().minusDays(i);
            aggregateDailyStatistics(date);
        }
    }

    @Override
    public void fillMissingDates(int days) {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < days; i++) {
            LocalDate date = today.minusDays(i);
            LambdaQueryWrapper<KbStatistics> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(KbStatistics::getStatDate, date);
            wrapper.eq(KbStatistics::getChannel, "all");
            
            if (statisticsMapper.selectCount(wrapper) == 0) {
                aggregateDailyStatistics(date);
            }
        }
    }

    private void saveEmptyStatistics(LocalDate date, String channel) {
        LambdaQueryWrapper<KbStatistics> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(KbStatistics::getStatDate, date);
        existWrapper.eq(KbStatistics::getChannel, channel);
        
        if (statisticsMapper.selectCount(existWrapper) == 0) {
            KbStatistics stats = new KbStatistics();
            stats.setStatDate(date);
            stats.setChannel(channel);
            stats.setConversationCount(0);
            stats.setMessageCount(0);
            stats.setUserCount(0);
            stats.setTotalTokens(0L);
            stats.setTotalPromptTokens(0L);
            stats.setTotalCompletionTokens(0L);
            stats.setAvgTokens(BigDecimal.ZERO);
            stats.setTotalCost(BigDecimal.ZERO);
            stats.setAvgCost(BigDecimal.ZERO);
            stats.setAvgLatencyMs(0);
            stats.setCreateTime(LocalDateTime.now());
            statisticsMapper.insert(stats);
        }
    }
}