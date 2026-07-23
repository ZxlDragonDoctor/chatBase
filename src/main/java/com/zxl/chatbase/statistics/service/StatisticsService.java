package com.zxl.chatbase.statistics.service;

import com.zxl.chatbase.statistics.dto.*;

import java.util.Map;

public interface StatisticsService {
    
    TokenStatisticsVO getTokenStatistics(Integer days, String userId);
    
    GroupActiveVO getGroupActiveRank(String platform, Integer limit, String userId);
    
    KeywordHotVO getHotKeywords(String platform, String groupId, Integer limit, String userId);
    
    ConversationStatisticsVO getConversationStatistics(Integer days, String userId);
    
    SystemOverviewVO getSystemOverview(String userId);

    Map<String, Object> getTokenChartData(Integer days, String userId);

    Map<String, Object> getTokenMonthlyData(String userId);

    Map<String, Object> getCostChartData(Integer days, String userId);

    Map<String, Object> getCostMonthlyData(String userId);
}