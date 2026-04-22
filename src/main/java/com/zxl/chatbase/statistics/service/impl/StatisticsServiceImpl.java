package com.zxl.chatbase.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zxl.chatbase.im.entity.GroupMessage;
import com.zxl.chatbase.im.mapper.GroupMessageMapper;
import com.zxl.chatbase.kb.entity.KbConversation;
import com.zxl.chatbase.kb.entity.KbDocument;
import com.zxl.chatbase.kb.entity.KbKnowledgeBase;
import com.zxl.chatbase.kb.entity.KbStatistics;
import com.zxl.chatbase.kb.mapper.KbConversationMapper;
import com.zxl.chatbase.kb.mapper.KbDocumentMapper;
import com.zxl.chatbase.kb.mapper.KbKnowledgeBaseMapper;
import com.zxl.chatbase.kb.mapper.KbStatisticsMapper;
import com.zxl.chatbase.statistics.dto.*;
import com.zxl.chatbase.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final KbConversationMapper conversationMapper;
    private final KbStatisticsMapper statisticsMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final KbKnowledgeBaseMapper knowledgeBaseMapper;
    private final KbDocumentMapper documentMapper;

    @Override
    public TokenStatisticsVO getTokenStatistics(Integer days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days != null ? days : 7);

        QueryWrapper<KbConversation> wrapper = new QueryWrapper<>();
        wrapper.select("IFNULL(SUM(tokens), 0) as totalTokens", "COUNT(1) as totalConversations")
                .ge("create_time", startDate.atStartOfDay())
                .le("create_time", endDate.atTime(23, 59, 59));

        Map<String, Object> totals = conversationMapper.selectMaps(wrapper).stream()
                .findFirst()
                .orElse(new HashMap<>());

        Long totalTokens = ((Number) totals.getOrDefault("totalTokens", 0L)).longValue();
        Integer totalConversations = ((Number) totals.getOrDefault("totalConversations", 0)).intValue();

        BigDecimal avgTokens = totalConversations > 0 
                ? BigDecimal.valueOf(totalTokens).divide(BigDecimal.valueOf(totalConversations), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<TokenStatisticsVO.DailyTokenVO> dailyTokens = new ArrayList<>();
        QueryWrapper<KbStatistics> statsWrapper = new QueryWrapper<>();
        statsWrapper.ge("stat_date", startDate)
                .le("stat_date", endDate)
                .eq("channel", "all")
                .orderByAsc("stat_date");
        List<KbStatistics> stats = statisticsMapper.selectList(statsWrapper);
        
        for (KbStatistics stat : stats) {
            TokenStatisticsVO.DailyTokenVO daily = new TokenStatisticsVO.DailyTokenVO();
            daily.setDate(stat.getStatDate().toString());
            daily.setTokens(stat.getTotalTokens() != null ? stat.getTotalTokens() : 0L);
            daily.setConversations(stat.getConversationCount() != null ? stat.getConversationCount() : 0);
            dailyTokens.add(daily);
        }

        TokenStatisticsVO vo = new TokenStatisticsVO();
        vo.setTotalTokens(totalTokens);
        vo.setAvgTokensPerConversation(avgTokens);
        vo.setTotalConversations(totalConversations);
        vo.setDailyTokens(dailyTokens);
        return vo;
    }

    @Override
    public GroupActiveVO getGroupActiveRank(String platform, Integer limit) {
        QueryWrapper<GroupMessage> wrapper = new QueryWrapper<>();
        wrapper.select("platform", "group_id", "COUNT(1) as messageCount", "MAX(message_time) as lastMessageTime")
                .groupBy("platform", "group_id")
                .orderByDesc("messageCount")
                .last("LIMIT " + (limit != null ? limit : 10));

        if (platform != null && !platform.equals("all")) {
            wrapper.eq("platform", platform);
        }

        List<Map<String, Object>> results = groupMessageMapper.selectMaps(wrapper);

        List<GroupActiveVO.GroupRankItem> topGroups = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> row : results) {
            GroupActiveVO.GroupRankItem item = new GroupActiveVO.GroupRankItem();
            item.setPlatform((String) row.get("platform"));
            item.setGroupId((String) row.get("group_id"));
            item.setMessageCount(((Number) row.get("messageCount")).longValue());
            Object lastTime = row.get("lastMessageTime");
            item.setLastMessageTime(lastTime != null ? lastTime.toString() : null);
            item.setRank(rank++);
            topGroups.add(item);
        }

        QueryWrapper<GroupMessage> countWrapper = new QueryWrapper<>();
        countWrapper.select("COUNT(DISTINCT group_id) as totalGroups", "COUNT(1) as totalMessages");
        Map<String, Object> counts = groupMessageMapper.selectMaps(countWrapper).stream()
                .findFirst()
                .orElse(new HashMap<>());

        GroupActiveVO vo = new GroupActiveVO();
        vo.setTopGroups(topGroups);
        vo.setTotalGroups(((Number) counts.getOrDefault("totalGroups", 0L)).longValue());
        vo.setTotalMessages(((Number) counts.getOrDefault("totalMessages", 0L)).longValue());
        return vo;
    }

    @Override
    public KeywordHotVO getHotKeywords(String platform, String groupId, Integer limit) {
        QueryWrapper<GroupMessage> wrapper = new QueryWrapper<>();
        wrapper.select("raw_message");
        
        if (platform != null && !platform.equals("all")) {
            wrapper.eq("platform", platform);
        }
        if (groupId != null && !groupId.isEmpty()) {
            wrapper.eq("group_id", groupId);
        }
        wrapper.isNotNull("raw_message")
                .ne("raw_message", "")
                .last("LIMIT 1000");

        List<GroupMessage> messages = groupMessageMapper.selectList(wrapper);

        Map<String, Integer> keywordCounts = new HashMap<>();
        Pattern chinesePattern = Pattern.compile("[\\u4e00-\\u9fa5]{2,8}");
        Pattern wordPattern = Pattern.compile("[a-zA-Z]{3,12}");

        for (GroupMessage msg : messages) {
            String content = msg.getRawMessage();
            if (content == null) continue;
            
            Matcher chineseMatcher = chinesePattern.matcher(content);
            while (chineseMatcher.find()) {
                String word = chineseMatcher.group();
                keywordCounts.merge(word, 1, Integer::sum);
            }
            
            Matcher wordMatcher = wordPattern.matcher(content);
            while (wordMatcher.find()) {
                String word = wordMatcher.group().toLowerCase();
                keywordCounts.merge(word, 1, Integer::sum);
            }
        }

        List<KeywordHotVO.KeywordItem> keywords = keywordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit != null ? limit : 20)
                .map(e -> {
                    KeywordHotVO.KeywordItem item = new KeywordHotVO.KeywordItem();
                    item.setKeyword(e.getKey());
                    item.setCount(e.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        int rank = 1;
        for (KeywordHotVO.KeywordItem item : keywords) {
            item.setRank(rank++);
        }

        KeywordHotVO vo = new KeywordHotVO();
        vo.setKeywords(keywords);
        vo.setPlatform(platform);
        vo.setGroupId(groupId);
        return vo;
    }

    @Override
    public ConversationStatisticsVO getConversationStatistics(Integer days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days != null ? days : 7);

        QueryWrapper<KbConversation> wrapper = new QueryWrapper<>();
        wrapper.ge("create_time", startDate.atStartOfDay())
                .le("create_time", endDate.atTime(23, 59, 59));

        Long total = conversationMapper.selectCount(wrapper);

        QueryWrapper<KbConversation> successWrapper = new QueryWrapper<>();
        successWrapper.ge("create_time", startDate.atStartOfDay())
                .le("create_time", endDate.atTime(23, 59, 59))
                .eq("status", true);
        Long successCount = conversationMapper.selectCount(successWrapper);

        Long failCount = total - successCount;
        BigDecimal successRate = total > 0 
                ? BigDecimal.valueOf(successCount * 100).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        QueryWrapper<KbConversation> avgWrapper = new QueryWrapper<>();
        avgWrapper.select("IFNULL(AVG(latency_ms), 0) as avgLatency")
                .ge("create_time", startDate.atStartOfDay())
                .le("create_time", endDate.atTime(23, 59, 59))
                .isNotNull("latency_ms");
        Map<String, Object> avgResult = conversationMapper.selectMaps(avgWrapper).stream()
                .findFirst()
                .orElse(new HashMap<>());
        BigDecimal avgLatency = BigDecimal.valueOf(((Number) avgResult.getOrDefault("avgLatency", 0)).doubleValue())
                .setScale(0, RoundingMode.HALF_UP);

        List<ConversationStatisticsVO.DailyConversationVO> dailyTrend = new ArrayList<>();
        QueryWrapper<KbStatistics> statsWrapper = new QueryWrapper<>();
        statsWrapper.ge("stat_date", startDate)
                .le("stat_date", endDate)
                .eq("channel", "all")
                .orderByAsc("stat_date");
        List<KbStatistics> stats = statisticsMapper.selectList(statsWrapper);
        
        for (KbStatistics stat : stats) {
            ConversationStatisticsVO.DailyConversationVO daily = new ConversationStatisticsVO.DailyConversationVO();
            daily.setDate(stat.getStatDate().toString());
            daily.setCount(stat.getConversationCount() != null ? stat.getConversationCount() : 0);
            daily.setSuccessCount(stat.getConversationCount() != null ? stat.getConversationCount() : 0);
            daily.setTokens(stat.getTotalTokens() != null ? stat.getTotalTokens() : 0L);
            dailyTrend.add(daily);
        }

        ConversationStatisticsVO vo = new ConversationStatisticsVO();
        vo.setTotalConversations(total);
        vo.setSuccessfulConversations(successCount);
        vo.setFailedConversations(failCount);
        vo.setSuccessRate(successRate);
        vo.setAvgLatencyMs(avgLatency);
        vo.setDailyTrend(dailyTrend);
        return vo;
    }

    @Override
    public SystemOverviewVO getSystemOverview() {
        Long totalMessages = groupMessageMapper.selectCount(new QueryWrapper<>());
        
        QueryWrapper<KbConversation> convWrapper = new QueryWrapper<>();
        convWrapper.select("COUNT(1) as total", "IFNULL(SUM(tokens), 0) as totalTokens");
        Map<String, Object> convStats = conversationMapper.selectMaps(convWrapper).stream()
                .findFirst()
                .orElse(new HashMap<>());
        Long totalConversations = ((Number) convStats.getOrDefault("total", 0L)).longValue();
        Long totalTokens = ((Number) convStats.getOrDefault("totalTokens", 0L)).longValue();

        QueryWrapper<GroupMessage> groupWrapper = new QueryWrapper<>();
        groupWrapper.select("COUNT(DISTINCT group_id) as activeGroups", "COUNT(DISTINCT user_id) as activeUsers");
        Map<String, Object> groupStats = groupMessageMapper.selectMaps(groupWrapper).stream()
                .findFirst()
                .orElse(new HashMap<>());
        Integer activeGroups = ((Number) groupStats.getOrDefault("activeGroups", 0)).intValue();
        Integer activeUsers = ((Number) groupStats.getOrDefault("activeUsers", 0)).intValue();

        Long knowledgeBases = knowledgeBaseMapper.selectCount(new QueryWrapper<>());
        Long documents = documentMapper.selectCount(new QueryWrapper<>());

        QueryWrapper<KbConversation> avgLatencyWrapper = new QueryWrapper<>();
        avgLatencyWrapper.select("IFNULL(AVG(latency_ms), 0) as avgLatency").isNotNull("latency_ms");
        Map<String, Object> latencyResult = conversationMapper.selectMaps(avgLatencyWrapper).stream()
                .findFirst()
                .orElse(new HashMap<>());
        BigDecimal avgLatency = BigDecimal.valueOf(((Number) latencyResult.getOrDefault("avgLatency", 0)).doubleValue())
                .setScale(0, RoundingMode.HALF_UP);

        QueryWrapper<KbConversation> successWrapper = new QueryWrapper<>();
        successWrapper.select("COUNT(1) as total");
        Map<String, Object> totalConv = conversationMapper.selectMaps(successWrapper).stream()
                .findFirst()
                .orElse(new HashMap<>());
        Long total = ((Number) totalConv.getOrDefault("total", 0L)).longValue();

        QueryWrapper<KbConversation> successCountWrapper = new QueryWrapper<>();
        successCountWrapper.eq("status", true);
        Long successCount = conversationMapper.selectCount(successCountWrapper);
        BigDecimal successRate = total > 0 
                ? BigDecimal.valueOf(successCount * 100).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        SystemOverviewVO.BotStatusVO botStatus = new SystemOverviewVO.BotStatusVO();
        botStatus.setQqEnabled(true);
        botStatus.setQqSelfId("configured");
        botStatus.setWecomEnabled(true);
        botStatus.setWecomCallbackPath("/intellrobot/callback/handle");

        SystemOverviewVO vo = new SystemOverviewVO();
        vo.setTotalMessages(totalMessages);
        vo.setTotalConversations(totalConversations);
        vo.setTotalTokens(totalTokens);
        vo.setActiveGroups(activeGroups);
        vo.setActiveUsers(activeUsers);
        vo.setKnowledgeBases(knowledgeBases.intValue());
        vo.setDocuments(documents.intValue());
        vo.setAvgLatencyMs(avgLatency);
        vo.setSuccessRate(successRate);
        vo.setBots(botStatus);
        return vo;
    }

    @Override
    public Map<String, Object> getTokenChartData(Integer days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        log.info("getTokenChartData: startDate={}, endDate={}, days={}", startDate, endDate, days);

        QueryWrapper<KbStatistics> wrapper = new QueryWrapper<>();
        wrapper.ge("stat_date", startDate);
        wrapper.le("stat_date", endDate);
        wrapper.eq("channel", "all");
        wrapper.orderByAsc("stat_date");
        
        List<KbStatistics> stats = statisticsMapper.selectList(wrapper);
        
        log.info("getTokenChartData查询结果: kb_statistics表共{}条记录", stats.size());
        if (stats.isEmpty()) {
            log.warn("kb_statistics表无数据，请先调用聚合接口 POST /api/statistics/aggregate");
        }

        List<String> dates = new ArrayList<>();
        List<Long> tokens = new ArrayList<>();
        List<Integer> conversations = new ArrayList<>();
        
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            dates.add(date.toString());
            
            KbStatistics stat = stats.stream()
                    .filter(s -> s.getStatDate().equals(date))
                    .findFirst()
                    .orElse(null);
            
            if (stat != null) {
                tokens.add(stat.getTotalTokens() != null ? stat.getTotalTokens() : 0L);
                conversations.add(stat.getConversationCount() != null ? stat.getConversationCount() : 0);
                log.debug("日期{}有数据: tokens={}, conversations={}", date, stat.getTotalTokens(), stat.getConversationCount());
            } else {
                tokens.add(0L);
                conversations.add(0);
            }
        }

        long totalTokensSum = tokens.stream().mapToLong(Long::longValue).sum();
        int totalConversationsSum = conversations.stream().mapToInt(Integer::intValue).sum();

        log.info("getTokenChartData结果: totalTokens={}, totalConversations={}, 数据点数={}", totalTokensSum, totalConversationsSum, dates.size());

Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("tokens", tokens);
        result.put("conversations", conversations);
        result.put("totalTokens", totalTokensSum);
        result.put("totalConversations", totalConversationsSum);
        result.put("avgTokens", totalConversationsSum > 0 
                ? BigDecimal.valueOf(totalTokensSum).divide(BigDecimal.valueOf(totalConversationsSum), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        
        return result;
    }

    @Override
    public Map<String, Object> getCostChartData(Integer days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        QueryWrapper<KbStatistics> wrapper = new QueryWrapper<>();
        wrapper.ge("stat_date", startDate);
        wrapper.le("stat_date", endDate);
        wrapper.eq("channel", "all");
        wrapper.orderByAsc("stat_date");
        
        List<KbStatistics> stats = statisticsMapper.selectList(wrapper);

        List<String> dates = new ArrayList<>();
        List<BigDecimal> costs = new ArrayList<>();
        List<Long> promptTokens = new ArrayList<>();
        List<Long> completionTokens = new ArrayList<>();
        
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            dates.add(date.toString());
            
            KbStatistics stat = stats.stream()
                    .filter(s -> s.getStatDate().equals(date))
                    .findFirst()
                    .orElse(null);
            
            if (stat != null) {
                costs.add(stat.getTotalCost() != null ? stat.getTotalCost() : BigDecimal.ZERO);
                promptTokens.add(stat.getTotalPromptTokens() != null ? stat.getTotalPromptTokens() : 0L);
                completionTokens.add(stat.getTotalCompletionTokens() != null ? stat.getTotalCompletionTokens() : 0L);
            } else {
                costs.add(BigDecimal.ZERO);
                promptTokens.add(0L);
                completionTokens.add(0L);
            }
        }

        BigDecimal totalCost = costs.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalPromptTokens = promptTokens.stream().mapToLong(Long::longValue).sum();
        long totalCompletionTokens = completionTokens.stream().mapToLong(Long::longValue).sum();

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("costs", costs);
        result.put("promptTokens", promptTokens);
        result.put("completionTokens", completionTokens);
        result.put("totalCost", totalCost);
        result.put("totalPromptTokens", totalPromptTokens);
        result.put("totalCompletionTokens", totalCompletionTokens);
        
        return result;
    }

    @Override
    public Map<String, Object> getCostMonthlyData() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        int daysInMonth = today.lengthOfMonth();

        QueryWrapper<KbStatistics> wrapper = new QueryWrapper<>();
        wrapper.ge("stat_date", firstDayOfMonth);
        wrapper.le("stat_date", today);
        wrapper.eq("channel", "all");
        wrapper.orderByAsc("stat_date");
        
        List<KbStatistics> stats = statisticsMapper.selectList(wrapper);

        List<String> dates = new ArrayList<>();
        List<BigDecimal> costs = new ArrayList<>();
        
        for (int i = 0; i < today.getDayOfMonth(); i++) {
            LocalDate date = firstDayOfMonth.plusDays(i);
            dates.add(date.toString());
            
            KbStatistics stat = stats.stream()
                    .filter(s -> s.getStatDate().equals(date))
                    .findFirst()
                    .orElse(null);
            
            if (stat != null) {
                costs.add(stat.getTotalCost() != null ? stat.getTotalCost() : BigDecimal.ZERO);
            } else {
                costs.add(BigDecimal.ZERO);
            }
        }

        BigDecimal totalCost = costs.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("costs", costs);
        result.put("totalCost", totalCost);
        result.put("monthProgress", BigDecimal.valueOf(today.getDayOfMonth())
                .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)));
        
        return result;
    }

    @Override
    public Map<String, Object> getTokenMonthlyData() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        int daysInMonth = today.lengthOfMonth();

        QueryWrapper<KbStatistics> wrapper = new QueryWrapper<>();
        wrapper.ge("stat_date", firstDayOfMonth);
        wrapper.le("stat_date", today);
        wrapper.eq("channel", "all");
        wrapper.orderByAsc("stat_date");
        
        List<KbStatistics> stats = statisticsMapper.selectList(wrapper);

        List<String> dates = new ArrayList<>();
        List<Long> tokens = new ArrayList<>();
        List<Integer> conversations = new ArrayList<>();
        
        for (int i = 0; i < today.getDayOfMonth(); i++) {
            LocalDate date = firstDayOfMonth.plusDays(i);
            dates.add(date.toString());
            
            KbStatistics stat = stats.stream()
                    .filter(s -> s.getStatDate().equals(date))
                    .findFirst()
                    .orElse(null);
            
            if (stat != null) {
                tokens.add(stat.getTotalTokens() != null ? stat.getTotalTokens() : 0L);
                conversations.add(stat.getConversationCount() != null ? stat.getConversationCount() : 0);
            } else {
                tokens.add(0L);
                conversations.add(0);
            }
        }

        long totalTokensSum = tokens.stream().mapToLong(Long::longValue).sum();
        int totalConversationsSum = conversations.stream().mapToInt(Integer::intValue).sum();

        BigDecimal dailyAvg = totalConversationsSum > 0
                ? BigDecimal.valueOf(totalTokensSum).divide(BigDecimal.valueOf(totalConversationsSum), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal projectedMonthly = dailyAvg.multiply(BigDecimal.valueOf(daysInMonth));

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("tokens", tokens);
        result.put("conversations", conversations);
        result.put("totalTokens", totalTokensSum);
        result.put("totalConversations", totalConversationsSum);
        result.put("avgTokens", dailyAvg);
        result.put("projectedMonthlyTokens", projectedMonthly);
        result.put("monthProgress", BigDecimal.valueOf(today.getDayOfMonth())
                .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)));
        
        return result;
    }
}