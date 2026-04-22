package com.zxl.chatbase.controller;

import com.zxl.chatbase.feedback.service.IFeedbackStatsService;
import com.zxl.chatbase.kb.service.IKbKeywordService;
import com.zxl.chatbase.statistics.dto.*;
import com.zxl.chatbase.statistics.service.IStatisticsAggregateService;
import com.zxl.chatbase.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final IKbKeywordService keywordService;
    private final IFeedbackStatsService feedbackStatsService;
    private final IStatisticsAggregateService aggregateService;

    @GetMapping("/token/daily")
    public TokenStatisticsVO getTokenDaily(@RequestParam(defaultValue = "7") Integer days) {
        return statisticsService.getTokenStatistics(days);
    }

    @GetMapping("/token/total")
    public TokenStatisticsVO getTokenTotal() {
        return statisticsService.getTokenStatistics(30);
    }

    @GetMapping("/group/active")
    public GroupActiveVO getGroupActive(
            @RequestParam(defaultValue = "all") String platform,
            @RequestParam(defaultValue = "10") Integer limit) {
        return statisticsService.getGroupActiveRank(platform, limit);
    }

    @GetMapping("/group/hot-keywords")
    public KeywordHotVO getHotKeywords(
            @RequestParam(defaultValue = "all") String platform,
            @RequestParam(required = false) String groupId,
            @RequestParam(defaultValue = "20") Integer limit) {
        return statisticsService.getHotKeywords(platform, groupId, limit);
    }

    @GetMapping("/conversation/overview")
    public ConversationStatisticsVO getConversationOverview(@RequestParam(defaultValue = "7") Integer days) {
        return statisticsService.getConversationStatistics(days);
    }

    @GetMapping("/conversation/trend")
    public ConversationStatisticsVO getConversationTrend(@RequestParam(defaultValue = "30") Integer days) {
        return statisticsService.getConversationStatistics(days);
    }

    @GetMapping("/system/overview")
    public SystemOverviewVO getSystemOverview() {
        return statisticsService.getSystemOverview();
    }

    @GetMapping("/keyword/cloud")
    public KeywordHotVO getKeywordCloud(
            @RequestParam(defaultValue = "all") String source,
            @RequestParam(defaultValue = "30") Integer days,
            @RequestParam(defaultValue = "50") Integer limit) {
        return keywordService.getKeywordCloud(source, days, limit);
    }

    @GetMapping("/keyword/top")
    public KeywordHotVO getTopKeywords(
            @RequestParam(defaultValue = "all") String source,
            @RequestParam(defaultValue = "20") Integer limit) {
        return keywordService.getKeywordCloud(source, null, limit);
    }

    @PostMapping("/keyword/batch-extract")
    public Map<String, Object> batchExtractKeywords(
            @RequestParam(defaultValue = "30") Integer days) {
        int msgCount = keywordService.batchExtractFromMessages("all", days);
        int convCount = keywordService.batchExtractFromConversations(days);
        int totalCount = msgCount + convCount;
        return Map.of(
                "success", true,
                "messageCount", msgCount,
                "conversationCount", convCount,
                "totalCount", totalCount,
                "message", "已处理群聊 " + msgCount + " 条, Web对话 " + convCount + " 条"
        );
    }

    @PostMapping("/keyword/sync-latest")
    public Map<String, Object> syncLatestKeywords(
            @RequestParam(defaultValue = "500") Integer limit) {
        int count = keywordService.syncLatestKeywords(limit);
        return Map.of("success", true, "count", count, "message", "已同步 " + count + " 条最新数据");
    }

    @GetMapping("/feedback/daily")
    public Map<String, Object> getFeedbackDailyStats(
            @RequestParam(defaultValue = "7") Integer days) {
        return feedbackStatsService.getDailyStats(days);
    }

    @GetMapping("/feedback/overview")
    public Map<String, Object> getFeedbackOverview() {
        return feedbackStatsService.getOverallStats();
    }

    @GetMapping("/token/chart")
    public Map<String, Object> getTokenChart(
            @RequestParam(defaultValue = "7") Integer days) {
        return statisticsService.getTokenChartData(days);
    }

    @GetMapping("/token/monthly")
    public Map<String, Object> getTokenMonthly() {
        return statisticsService.getTokenMonthlyData();
    }

    @PostMapping("/aggregate")
    public Map<String, Object> aggregateStatistics(
            @RequestParam(defaultValue = "7") Integer days) {
        aggregateService.fillMissingDates(days);
        return Map.of("success", true, "message", "统计聚合完成");
    }

    @GetMapping("/cost/chart")
    public Map<String, Object> getCostChart(
            @RequestParam(defaultValue = "7") Integer days) {
        return statisticsService.getCostChartData(days);
    }

    @GetMapping("/cost/monthly")
    public Map<String, Object> getCostMonthly() {
        return statisticsService.getCostMonthlyData();
    }
}