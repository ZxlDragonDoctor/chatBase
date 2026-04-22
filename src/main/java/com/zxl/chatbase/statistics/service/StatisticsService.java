package com.zxl.chatbase.statistics.service;

import com.zxl.chatbase.statistics.dto.*;

import java.util.Map;

public interface StatisticsService {
    
    TokenStatisticsVO getTokenStatistics(Integer days);
    
    GroupActiveVO getGroupActiveRank(String platform, Integer limit);
    
    KeywordHotVO getHotKeywords(String platform, String groupId, Integer limit);
    
    ConversationStatisticsVO getConversationStatistics(Integer days);
    
    SystemOverviewVO getSystemOverview();

    Map<String, Object> getTokenChartData(Integer days);

    Map<String, Object> getTokenMonthlyData();

    Map<String, Object> getCostChartData(Integer days);

    Map<String, Object> getCostMonthlyData();
}