package com.zxl.chatbase.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zxl.chatbase.feedback.service.IFeedbackStatsService;
import com.zxl.chatbase.kb.entity.SysUser;
import com.zxl.chatbase.kb.mapper.SysUserMapper;
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
    private final SysUserMapper sysUserMapper;

    private String resolveUserId(String currentUser, String scope) {
        if (currentUser == null) return null;
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, currentUser)
                .select(SysUser::getRole);
        SysUser user = sysUserMapper.selectOne(wrapper);
        boolean isAdmin = user != null && "admin".equals(user.getRole());
        if (isAdmin && !"mine".equals(scope)) {
            return null;
        }
        return currentUser;
    }

    @GetMapping("/token/daily")
    public TokenStatisticsVO getTokenDaily(
            @RequestParam(defaultValue = "7") Integer days,
            @RequestAttribute("currentUser") String currentUser,
            @RequestParam(required = false, defaultValue = "all") String scope) {
        return statisticsService.getTokenStatistics(days, resolveUserId(currentUser, scope));
    }

    @GetMapping("/token/total")
    public TokenStatisticsVO getTokenTotal(
            @RequestAttribute("currentUser") String currentUser,
            @RequestParam(required = false, defaultValue = "all") String scope) {
        return statisticsService.getTokenStatistics(30, resolveUserId(currentUser, scope));
    }

    @GetMapping("/group/active")
    public GroupActiveVO getGroupActive(
            @RequestParam(defaultValue = "all") String platform,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestAttribute("currentUser") String currentUser,
            @RequestParam(required = false, defaultValue = "all") String scope) {
        return statisticsService.getGroupActiveRank(platform, limit, resolveUserId(currentUser, scope));
    }

    @GetMapping("/group/hot-keywords")
    public KeywordHotVO getHotKeywords(
            @RequestParam(defaultValue = "all") String platform,
            @RequestParam(required = false) String groupId,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestAttribute("currentUser") String currentUser,
            @RequestParam(required = false, defaultValue = "all") String scope) {
        return statisticsService.getHotKeywords(platform, groupId, limit, resolveUserId(currentUser, scope));
    }

    @GetMapping("/conversation/overview")
    public ConversationStatisticsVO getConversationOverview(
            @RequestParam(defaultValue = "7") Integer days,
            @RequestAttribute("currentUser") String currentUser,
            @RequestParam(required = false, defaultValue = "all") String scope) {
        return statisticsService.getConversationStatistics(days, resolveUserId(currentUser, scope));
    }

    @GetMapping("/conversation/trend")
    public ConversationStatisticsVO getConversationTrend(
            @RequestParam(defaultValue = "30") Integer days,
            @RequestAttribute("currentUser") String currentUser,
            @RequestParam(required = false, defaultValue = "all") String scope) {
        return statisticsService.getConversationStatistics(days, resolveUserId(currentUser, scope));
    }

    @GetMapping("/system/overview")
    public SystemOverviewVO getSystemOverview(
            @RequestAttribute("currentUser") String currentUser,
            @RequestParam(required = false, defaultValue = "all") String scope) {
        return statisticsService.getSystemOverview(resolveUserId(currentUser, scope));
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
            @RequestParam(defaultValue = "7") Integer days,
            @RequestAttribute("currentUser") String currentUser,
            @RequestParam(required = false, defaultValue = "all") String scope) {
        return statisticsService.getTokenChartData(days, resolveUserId(currentUser, scope));
    }

    @GetMapping("/token/monthly")
    public Map<String, Object> getTokenMonthly(
            @RequestAttribute("currentUser") String currentUser,
            @RequestParam(required = false, defaultValue = "all") String scope) {
        return statisticsService.getTokenMonthlyData(resolveUserId(currentUser, scope));
    }

    @PostMapping("/aggregate")
    public Map<String, Object> aggregateStatistics(
            @RequestParam(defaultValue = "7") Integer days) {
        aggregateService.fillMissingDates(days);
        return Map.of("success", true, "message", "统计聚合完成");
    }

    @GetMapping("/cost/chart")
    public Map<String, Object> getCostChart(
            @RequestParam(defaultValue = "7") Integer days,
            @RequestAttribute("currentUser") String currentUser,
            @RequestParam(required = false, defaultValue = "all") String scope) {
        return statisticsService.getCostChartData(days, resolveUserId(currentUser, scope));
    }

    @GetMapping("/cost/monthly")
    public Map<String, Object> getCostMonthly(
            @RequestAttribute("currentUser") String currentUser,
            @RequestParam(required = false, defaultValue = "all") String scope) {
        return statisticsService.getCostMonthlyData(resolveUserId(currentUser, scope));
    }
}