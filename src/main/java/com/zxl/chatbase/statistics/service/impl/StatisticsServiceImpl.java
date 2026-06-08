package com.zxl.chatbase.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zxl.chatbase.im.entity.GroupMessage;
import com.zxl.chatbase.im.entity.ImGroup;
import com.zxl.chatbase.im.mapper.GroupMessageMapper;
import com.zxl.chatbase.im.mapper.ImGroupMapper;
import com.zxl.chatbase.kb.entity.KbApp;
import com.zxl.chatbase.kb.entity.KbConversation;
import com.zxl.chatbase.kb.entity.KbDocument;
import com.zxl.chatbase.kb.entity.KbKnowledgeBase;
import com.zxl.chatbase.kb.entity.KbStatistics;
import com.zxl.chatbase.kb.entity.SysUser;
import com.zxl.chatbase.kb.mapper.KbAppMapper;
import com.zxl.chatbase.kb.mapper.KbConversationMapper;
import com.zxl.chatbase.kb.mapper.KbDocumentMapper;
import com.zxl.chatbase.kb.mapper.KbKnowledgeBaseMapper;
import com.zxl.chatbase.kb.mapper.KbStatisticsMapper;
import com.zxl.chatbase.kb.mapper.SysUserMapper;
import com.zxl.chatbase.statistics.dto.*;
import com.zxl.chatbase.statistics.service.StatisticsService;
import com.zxl.chatbase.wx.config.WxProperties;
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
    private final KbAppMapper appMapper;
    private final ImGroupMapper imGroupMapper;
    private final SysUserMapper sysUserMapper;
    private final WxProperties wxProperties;

    private boolean isAdmin(String userId) {
        if (userId == null) return true;
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, userId).select(SysUser::getRole));
        return user != null && "admin".equals(user.getRole());
    }

    private List<Long> getUserAppIds(String userId) {
        if (userId == null) return List.of();
        return appMapper.selectList(
                new LambdaQueryWrapper<KbApp>()
                        .eq(KbApp::getCreateBy, userId)
                        .select(KbApp::getId)
        ).stream().map(KbApp::getId).collect(Collectors.toList());
    }

    private List<String> getUserGroupIds(String userId) {
        if (userId == null) return List.of();
        List<Long> appIds = getUserAppIds(userId);
        LambdaQueryWrapper<ImGroup> wrapper = new LambdaQueryWrapper<ImGroup>()
                .eq(ImGroup::getCreatedBy, userId)
                .select(ImGroup::getGroupId);
        if (!appIds.isEmpty()) {
            wrapper.or().in(ImGroup::getAppId, appIds);
        }
        return imGroupMapper.selectList(wrapper)
                .stream().map(ImGroup::getGroupId)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<Long> getUserKbIds(String userId) {
        if (userId == null) return List.of();
        return knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KbKnowledgeBase>()
                        .eq(KbKnowledgeBase::getCreateBy, userId)
                        .select(KbKnowledgeBase::getId)
        ).stream().map(KbKnowledgeBase::getId).collect(Collectors.toList());
    }

    @Override
    public TokenStatisticsVO getTokenStatistics(Integer days, String userId) {
        if (isAdmin(userId)) userId = null;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days != null ? days : 7);

        List<Long> appIds = getUserAppIds(userId);
        QueryWrapper<KbConversation> wrapper = new QueryWrapper<>();
        wrapper.select("IFNULL(SUM(tokens), 0) as totalTokens", "COUNT(1) as totalConversations")
                .ge("create_time", startDate.atStartOfDay())
                .le("create_time", endDate.atTime(23, 59, 59));
        if (!appIds.isEmpty()) {
            wrapper.in("app_id", appIds);
        } else if (userId != null) {
            return new TokenStatisticsVO(0L, BigDecimal.ZERO, 0, List.of());
        }

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
        if (userId != null) {
            statsWrapper.eq("created_by", userId);
        }
        List<KbStatistics> stats = statisticsMapper.selectList(statsWrapper);
        if (userId != null && stats.isEmpty()) {
            // fallback: compute daily from kb_conversation
            dailyTokens = computeDailyTokenStats(startDate, endDate, appIds);
        } else {
            for (KbStatistics stat : stats) {
                TokenStatisticsVO.DailyTokenVO daily = new TokenStatisticsVO.DailyTokenVO();
                daily.setDate(stat.getStatDate().toString());
                daily.setTokens(stat.getTotalTokens() != null ? stat.getTotalTokens() : 0L);
                daily.setConversations(stat.getConversationCount() != null ? stat.getConversationCount() : 0);
                dailyTokens.add(daily);
            }
        }

        TokenStatisticsVO vo = new TokenStatisticsVO();
        vo.setTotalTokens(totalTokens);
        vo.setAvgTokensPerConversation(avgTokens);
        vo.setTotalConversations(totalConversations);
        vo.setDailyTokens(dailyTokens);
        return vo;
    }

    private List<TokenStatisticsVO.DailyTokenVO> computeDailyTokenStats(LocalDate startDate, LocalDate endDate, List<Long> appIds) {
        List<TokenStatisticsVO.DailyTokenVO> result = new ArrayList<>();
        QueryWrapper<KbConversation> wrapper = new QueryWrapper<>();
        wrapper.select("DATE(create_time) as stat_date", "COUNT(1) as cnt", "IFNULL(SUM(tokens), 0) as tok")
                .ge("create_time", startDate.atStartOfDay())
                .lt("create_time", endDate.plusDays(1).atStartOfDay())
                .groupBy("DATE(create_time)")
                .orderByAsc("DATE(create_time)");
        if (!appIds.isEmpty()) {
            wrapper.in("app_id", appIds);
        }
        List<Map<String, Object>> rows = conversationMapper.selectMaps(wrapper);
        Map<LocalDate, Map<String, Object>> rowMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object dateObj = row.get("stat_date");
            if (dateObj != null) {
                LocalDate d = toLocalDate(dateObj);
                rowMap.put(d, row);
            }
        }
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            TokenStatisticsVO.DailyTokenVO daily = new TokenStatisticsVO.DailyTokenVO();
            daily.setDate(d.toString());
            Map<String, Object> row = rowMap.get(d);
            if (row != null) {
                daily.setTokens(((Number) row.getOrDefault("tok", 0L)).longValue());
                daily.setConversations(((Number) row.getOrDefault("cnt", 0)).intValue());
            } else {
                daily.setTokens(0L);
                daily.setConversations(0);
            }
            result.add(daily);
        }
        return result;
    }

    private LocalDate toLocalDate(Object obj) {
        if (obj instanceof java.sql.Date) {
            return ((java.sql.Date) obj).toLocalDate();
        }
        if (obj instanceof LocalDate) {
            return (LocalDate) obj;
        }
        if (obj instanceof String) {
            return LocalDate.parse((String) obj);
        }
        return LocalDate.now();
    }

    @Override
    public GroupActiveVO getGroupActiveRank(String platform, Integer limit, String userId) {
        if (isAdmin(userId)) userId = null;
        List<String> groupIds = getUserGroupIds(userId);
        if (userId != null && groupIds.isEmpty()) {
            return new GroupActiveVO(List.of(), 0L, 0L);
        }

        QueryWrapper<GroupMessage> wrapper = new QueryWrapper<>();
        wrapper.select("platform", "group_id", "COUNT(1) as messageCount", "MAX(message_time) as lastMessageTime")
                .groupBy("platform", "group_id")
                .orderByDesc("messageCount")
                .last("LIMIT " + (limit != null ? limit : 10));
        if (platform != null && !platform.equals("all")) {
            wrapper.eq("platform", platform);
        }
        if (!groupIds.isEmpty()) {
            wrapper.in("group_id", groupIds);
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
        if (!groupIds.isEmpty()) {
            countWrapper.in("group_id", groupIds);
        }
        if (platform != null && !platform.equals("all")) {
            countWrapper.eq("platform", platform);
        }
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
    public KeywordHotVO getHotKeywords(String platform, String groupId, Integer limit, String userId) {
        if (isAdmin(userId)) userId = null;
        List<String> groupIds = getUserGroupIds(userId);
        if (userId != null && groupIds.isEmpty()) {
            return new KeywordHotVO(List.of(), platform, groupId);
        }

        QueryWrapper<GroupMessage> wrapper = new QueryWrapper<>();
        if (platform != null && !platform.equals("all")) {
            wrapper.eq("platform", platform);
        }
        if (groupId != null && !groupId.isEmpty()) {
            wrapper.eq("group_id", groupId);
        } else if (!groupIds.isEmpty()) {
            wrapper.in("group_id", groupIds);
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
    public ConversationStatisticsVO getConversationStatistics(Integer days, String userId) {
        if (isAdmin(userId)) userId = null;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days != null ? days : 7);

        List<Long> appIds = getUserAppIds(userId);
        boolean empty = userId != null && appIds.isEmpty();

        QueryWrapper<KbConversation> wrapper = new QueryWrapper<>();
        wrapper.ge("create_time", startDate.atStartOfDay())
                .le("create_time", endDate.atTime(23, 59, 59));
        if (!appIds.isEmpty()) {
            wrapper.in("app_id", appIds);
        }
        Long total = empty ? 0L : conversationMapper.selectCount(wrapper);

        QueryWrapper<KbConversation> successWrapper = new QueryWrapper<>();
        successWrapper.ge("create_time", startDate.atStartOfDay())
                .le("create_time", endDate.atTime(23, 59, 59))
                .eq("status", true);
        if (!appIds.isEmpty()) {
            successWrapper.in("app_id", appIds);
        }
        Long successCount = empty ? 0L : conversationMapper.selectCount(successWrapper);

        Long failCount = total - successCount;
        BigDecimal successRate = total > 0 
                ? BigDecimal.valueOf(successCount * 100).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        QueryWrapper<KbConversation> avgWrapper = new QueryWrapper<>();
        avgWrapper.select("IFNULL(AVG(latency_ms), 0) as avgLatency")
                .ge("create_time", startDate.atStartOfDay())
                .le("create_time", endDate.atTime(23, 59, 59))
                .isNotNull("latency_ms");
        if (!appIds.isEmpty()) {
            avgWrapper.in("app_id", appIds);
        }
        Map<String, Object> avgResult = empty ? new HashMap<>() : conversationMapper.selectMaps(avgWrapper).stream()
                .findFirst()
                .orElse(new HashMap<>());
        BigDecimal avgLatency = BigDecimal.valueOf(((Number) avgResult.getOrDefault("avgLatency", 0)).doubleValue())
                .setScale(0, RoundingMode.HALF_UP);

        List<ConversationStatisticsVO.DailyConversationVO> dailyTrend = new ArrayList<>();
        if (!empty) {
            dailyTrend = computeDailyConversationStats(startDate, endDate, appIds);
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

    private List<ConversationStatisticsVO.DailyConversationVO> computeDailyConversationStats(LocalDate startDate, LocalDate endDate, List<Long> appIds) {
        List<ConversationStatisticsVO.DailyConversationVO> result = new ArrayList<>();
        QueryWrapper<KbConversation> wrapper = new QueryWrapper<>();
        wrapper.select("DATE(create_time) as stat_date", "COUNT(1) as cnt", "IFNULL(SUM(tokens), 0) as tok")
                .ge("create_time", startDate.atStartOfDay())
                .lt("create_time", endDate.plusDays(1).atStartOfDay())
                .groupBy("DATE(create_time)")
                .orderByAsc("DATE(create_time)");
        if (!appIds.isEmpty()) {
            wrapper.in("app_id", appIds);
        }
        List<Map<String, Object>> rows = conversationMapper.selectMaps(wrapper);
        Map<LocalDate, Map<String, Object>> rowMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object dateObj = row.get("stat_date");
            if (dateObj != null) {
                LocalDate d = toLocalDate(dateObj);
                rowMap.put(d, row);
            }
        }
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            ConversationStatisticsVO.DailyConversationVO daily = new ConversationStatisticsVO.DailyConversationVO();
            daily.setDate(d.toString());
            Map<String, Object> row = rowMap.get(d);
            if (row != null) {
                daily.setCount(((Number) row.getOrDefault("cnt", 0)).intValue());
                daily.setSuccessCount(((Number) row.getOrDefault("cnt", 0)).intValue());
                daily.setTokens(((Number) row.getOrDefault("tok", 0L)).longValue());
            } else {
                daily.setCount(0);
                daily.setSuccessCount(0);
                daily.setTokens(0L);
            }
            result.add(daily);
        }
        return result;
    }

    @Override
    public SystemOverviewVO getSystemOverview(String userId) {
        if (isAdmin(userId)) userId = null;
        List<String> groupIds = getUserGroupIds(userId);
        List<Long> appIds = getUserAppIds(userId);
        List<Long> kbIds = getUserKbIds(userId);
        boolean empty = userId != null;

        Long totalMessages;
        if (empty && groupIds.isEmpty()) {
            totalMessages = 0L;
        } else {
            QueryWrapper<GroupMessage> msgWrapper = new QueryWrapper<>();
            if (!groupIds.isEmpty()) {
                msgWrapper.in("group_id", groupIds);
            }
            totalMessages = groupMessageMapper.selectCount(msgWrapper);
        }

        QueryWrapper<KbConversation> convWrapper = new QueryWrapper<>();
        convWrapper.select("COUNT(1) as total", "IFNULL(SUM(tokens), 0) as totalTokens");
        if (!appIds.isEmpty()) {
            convWrapper.in("app_id", appIds);
        }
        Map<String, Object> convStats = (empty && appIds.isEmpty())
                ? Map.of("total", 0L, "totalTokens", 0L)
                : conversationMapper.selectMaps(convWrapper).stream().findFirst().orElse(Map.of("total", 0L, "totalTokens", 0L));
        Long totalConversations = ((Number) convStats.getOrDefault("total", 0L)).longValue();
        Long totalTokens = ((Number) convStats.getOrDefault("totalTokens", 0L)).longValue();

        QueryWrapper<GroupMessage> groupWrapper = new QueryWrapper<>();
        groupWrapper.select("COUNT(DISTINCT group_id) as activeGroups", "COUNT(DISTINCT user_id) as activeUsers");
        if (!groupIds.isEmpty()) {
            groupWrapper.in("group_id", groupIds);
        }
        Map<String, Object> groupStats = (empty && groupIds.isEmpty())
                ? Map.of("activeGroups", 0, "activeUsers", 0)
                : groupMessageMapper.selectMaps(groupWrapper).stream().findFirst().orElse(Map.of("activeGroups", 0, "activeUsers", 0));
        Integer activeGroups = ((Number) groupStats.getOrDefault("activeGroups", 0)).intValue();
        Integer activeUsers = ((Number) groupStats.getOrDefault("activeUsers", 0)).intValue();

        Long knowledgeBases;
        if (empty && kbIds.isEmpty()) {
            knowledgeBases = 0L;
        } else {
            QueryWrapper<KbKnowledgeBase> kbWrapper = new QueryWrapper<>();
            if (!kbIds.isEmpty()) {
                kbWrapper.in("id", kbIds);
            }
            knowledgeBases = knowledgeBaseMapper.selectCount(kbWrapper);
        }

        Long documents;
        if (empty && kbIds.isEmpty()) {
            documents = 0L;
        } else {
            QueryWrapper<KbDocument> docWrapper = new QueryWrapper<>();
            if (!kbIds.isEmpty()) {
                docWrapper.in("knowledge_base_id", kbIds);
            }
            documents = documentMapper.selectCount(docWrapper);
        }

        QueryWrapper<KbConversation> avgLatencyWrapper = new QueryWrapper<>();
        avgLatencyWrapper.select("IFNULL(AVG(latency_ms), 0) as avgLatency").isNotNull("latency_ms");
        if (!appIds.isEmpty()) {
            avgLatencyWrapper.in("app_id", appIds);
        }
        Map<String, Object> latencyResult = (empty && appIds.isEmpty())
                ? Map.of("avgLatency", 0)
                : conversationMapper.selectMaps(avgLatencyWrapper).stream().findFirst().orElse(Map.of("avgLatency", 0));
        BigDecimal avgLatency = BigDecimal.valueOf(((Number) latencyResult.getOrDefault("avgLatency", 0)).doubleValue())
                .setScale(0, RoundingMode.HALF_UP);

        QueryWrapper<KbConversation> successWrapper = new QueryWrapper<>();
        successWrapper.select("COUNT(1) as total");
        if (!appIds.isEmpty()) {
            successWrapper.in("app_id", appIds);
        }
        Map<String, Object> totalConv = (empty && appIds.isEmpty())
                ? Map.of("total", 0L)
                : conversationMapper.selectMaps(successWrapper).stream().findFirst().orElse(Map.of("total", 0L));
        Long total = ((Number) totalConv.getOrDefault("total", 0L)).longValue();

        QueryWrapper<KbConversation> successCountWrapper = new QueryWrapper<>();
        successCountWrapper.eq("status", true);
        if (!appIds.isEmpty()) {
            successCountWrapper.in("app_id", appIds);
        }
        Long successCount = (empty && appIds.isEmpty()) ? 0L : conversationMapper.selectCount(successCountWrapper);
        BigDecimal successRate = total > 0 
                ? BigDecimal.valueOf(successCount * 100).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        SystemOverviewVO.BotStatusVO botStatus = new SystemOverviewVO.BotStatusVO();
        botStatus.setQqEnabled(true);
        botStatus.setQqSelfId("configured");
        botStatus.setWecomEnabled(true);
        botStatus.setWecomCallbackPath("/intellrobot/callback/handle");
        botStatus.setWxEnabled(wxProperties.isEnable() &&
                org.springframework.util.StringUtils.hasText(wxProperties.getBotToken()));
        botStatus.setWxNickname(wxProperties.getNickname());

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
    public Map<String, Object> getTokenChartData(Integer days, String userId) {
        if (isAdmin(userId)) userId = null;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<Long> appIds = getUserAppIds(userId);
        List<TokenStatisticsVO.DailyTokenVO> dailyData;
        if (userId != null && appIds.isEmpty()) {
            dailyData = new ArrayList<>();
            for (int i = 0; i < days; i++) {
                TokenStatisticsVO.DailyTokenVO d = new TokenStatisticsVO.DailyTokenVO();
                d.setDate(startDate.plusDays(i).toString());
                d.setTokens(0L);
                d.setConversations(0);
                dailyData.add(d);
            }
        } else if (userId != null) {
            dailyData = computeDailyTokenStats(startDate, endDate, appIds);
        } else {
            QueryWrapper<KbStatistics> wrapper = new QueryWrapper<>();
            wrapper.ge("stat_date", startDate);
            wrapper.le("stat_date", endDate);
            wrapper.eq("channel", "all");
            wrapper.orderByAsc("stat_date");
            List<KbStatistics> stats = statisticsMapper.selectList(wrapper);

            dailyData = new ArrayList<>();
            for (int i = 0; i < days; i++) {
                LocalDate date = startDate.plusDays(i);
                TokenStatisticsVO.DailyTokenVO d = new TokenStatisticsVO.DailyTokenVO();
                d.setDate(date.toString());
                final LocalDate fd = date;
                KbStatistics stat = stats.stream().filter(s -> s.getStatDate().equals(fd)).findFirst().orElse(null);
                d.setTokens(stat != null && stat.getTotalTokens() != null ? stat.getTotalTokens() : 0L);
                d.setConversations(stat != null && stat.getConversationCount() != null ? stat.getConversationCount() : 0);
                dailyData.add(d);
            }
        }

        List<String> dates = new ArrayList<>();
        List<Long> tokens = new ArrayList<>();
        List<Integer> conversations = new ArrayList<>();
        for (TokenStatisticsVO.DailyTokenVO d : dailyData) {
            dates.add(d.getDate());
            tokens.add(d.getTokens());
            conversations.add(d.getConversations());
        }

        long totalTokensSum = tokens.stream().mapToLong(Long::longValue).sum();
        int totalConversationsSum = conversations.stream().mapToInt(Integer::intValue).sum();

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
    public Map<String, Object> getCostChartData(Integer days, String userId) {
        if (isAdmin(userId)) userId = null;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<Long> appIds = getUserAppIds(userId);
        List<Map<String, Object>> dailyRows;
        if (userId != null && appIds.isEmpty()) {
            dailyRows = new ArrayList<>();
        } else if (userId != null) {
            QueryWrapper<KbConversation> wrapper = new QueryWrapper<>();
            wrapper.select("DATE(create_time) as stat_date",
                           "IFNULL(SUM(total_price), 0) as cost",
                           "IFNULL(SUM(prompt_tokens), 0) as pt",
                           "IFNULL(SUM(completion_tokens), 0) as ct")
                    .ge("create_time", startDate.atStartOfDay())
                    .lt("create_time", endDate.plusDays(1).atStartOfDay())
                    .groupBy("DATE(create_time)")
                    .orderByAsc("DATE(create_time)");
            if (!appIds.isEmpty()) wrapper.in("app_id", appIds);
            dailyRows = conversationMapper.selectMaps(wrapper);
        } else {
            QueryWrapper<KbStatistics> wrapper = new QueryWrapper<>();
            wrapper.ge("stat_date", startDate);
            wrapper.le("stat_date", endDate);
            wrapper.eq("channel", "all");
            wrapper.orderByAsc("stat_date");
            List<KbStatistics> stats = statisticsMapper.selectList(wrapper);

            dailyRows = new ArrayList<>();
            for (KbStatistics s : stats) {
                Map<String, Object> row = new HashMap<>();
                row.put("stat_date", s.getStatDate());
                row.put("cost", s.getTotalCost() != null ? s.getTotalCost() : BigDecimal.ZERO);
                row.put("pt", s.getTotalPromptTokens() != null ? s.getTotalPromptTokens() : 0L);
                row.put("ct", s.getTotalCompletionTokens() != null ? s.getTotalCompletionTokens() : 0L);
                dailyRows.add(row);
            }
        }

        Map<LocalDate, Map<String, Object>> rowMap = new HashMap<>();
        for (Map<String, Object> row : dailyRows) {
            Object dateObj = row.get("stat_date");
            if (dateObj != null) {
                rowMap.put(toLocalDate(dateObj), row);
            }
        }

        List<String> dates = new ArrayList<>();
        List<BigDecimal> costs = new ArrayList<>();
        List<Long> promptTokens = new ArrayList<>();
        List<Long> completionTokens = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            dates.add(date.toString());
            Map<String, Object> row = rowMap.get(date);
            if (row != null) {
                costs.add((BigDecimal) row.getOrDefault("cost", BigDecimal.ZERO));
                promptTokens.add(((Number) row.getOrDefault("pt", 0L)).longValue());
                completionTokens.add(((Number) row.getOrDefault("ct", 0L)).longValue());
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
    public Map<String, Object> getCostMonthlyData(String userId) {
        if (isAdmin(userId)) userId = null;
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        List<Long> appIds = getUserAppIds(userId);
        List<Map<String, Object>> dailyRows;
        if (userId != null && appIds.isEmpty()) {
            dailyRows = List.of();
        } else if (userId != null) {
            QueryWrapper<KbConversation> wrapper = new QueryWrapper<>();
            wrapper.select("DATE(create_time) as stat_date", "IFNULL(SUM(total_price), 0) as cost")
                    .ge("create_time", firstDayOfMonth.atStartOfDay())
                    .lt("create_time", today.plusDays(1).atStartOfDay())
                    .groupBy("DATE(create_time)")
                    .orderByAsc("DATE(create_time)");
            if (!appIds.isEmpty()) wrapper.in("app_id", appIds);
            dailyRows = conversationMapper.selectMaps(wrapper);
        } else {
            QueryWrapper<KbStatistics> wrapper = new QueryWrapper<>();
            wrapper.ge("stat_date", firstDayOfMonth);
            wrapper.le("stat_date", today);
            wrapper.eq("channel", "all");
            wrapper.orderByAsc("stat_date");
            dailyRows = new ArrayList<>();
            for (KbStatistics s : statisticsMapper.selectList(wrapper)) {
                Map<String, Object> row = new HashMap<>();
                row.put("stat_date", s.getStatDate());
                row.put("cost", s.getTotalCost() != null ? s.getTotalCost() : BigDecimal.ZERO);
                dailyRows.add(row);
            }
        }

        Map<LocalDate, BigDecimal> costMap = new HashMap<>();
        for (Map<String, Object> row : dailyRows) {
            Object dateObj = row.get("stat_date");
            if (dateObj != null) {
                costMap.put(toLocalDate(dateObj), (BigDecimal) row.getOrDefault("cost", BigDecimal.ZERO));
            }
        }

        List<String> dates = new ArrayList<>();
        List<BigDecimal> costs = new ArrayList<>();
        for (int i = 0; i < today.getDayOfMonth(); i++) {
            LocalDate date = firstDayOfMonth.plusDays(i);
            dates.add(date.toString());
            costs.add(costMap.getOrDefault(date, BigDecimal.ZERO));
        }

        BigDecimal totalCost = costs.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        int daysInMonth = today.lengthOfMonth();

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
    public Map<String, Object> getTokenMonthlyData(String userId) {
        if (isAdmin(userId)) userId = null;
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        int daysInMonth = today.lengthOfMonth();
        List<Long> appIds = getUserAppIds(userId);

        List<TokenStatisticsVO.DailyTokenVO> dailyData;
        if (userId != null && appIds.isEmpty()) {
            dailyData = new ArrayList<>();
            for (int i = 0; i < today.getDayOfMonth(); i++) {
                TokenStatisticsVO.DailyTokenVO d = new TokenStatisticsVO.DailyTokenVO();
                d.setDate(firstDayOfMonth.plusDays(i).toString());
                d.setTokens(0L);
                d.setConversations(0);
                dailyData.add(d);
            }
        } else if (userId != null) {
            dailyData = computeDailyTokenStats(firstDayOfMonth, today, appIds);
        } else {
            QueryWrapper<KbStatistics> wrapper = new QueryWrapper<>();
            wrapper.ge("stat_date", firstDayOfMonth);
            wrapper.le("stat_date", today);
            wrapper.eq("channel", "all");
            wrapper.orderByAsc("stat_date");
            List<KbStatistics> stats = statisticsMapper.selectList(wrapper);

            dailyData = new ArrayList<>();
            for (int i = 0; i < today.getDayOfMonth(); i++) {
                LocalDate date = firstDayOfMonth.plusDays(i);
                TokenStatisticsVO.DailyTokenVO d = new TokenStatisticsVO.DailyTokenVO();
                d.setDate(date.toString());
                final LocalDate fd = date;
                KbStatistics stat = stats.stream().filter(s -> s.getStatDate().equals(fd)).findFirst().orElse(null);
                d.setTokens(stat != null && stat.getTotalTokens() != null ? stat.getTotalTokens() : 0L);
                d.setConversations(stat != null && stat.getConversationCount() != null ? stat.getConversationCount() : 0);
                dailyData.add(d);
            }
        }

        List<String> dates = new ArrayList<>();
        List<Long> tokens = new ArrayList<>();
        List<Integer> conversations = new ArrayList<>();
        for (TokenStatisticsVO.DailyTokenVO d : dailyData) {
            dates.add(d.getDate());
            tokens.add(d.getTokens());
            conversations.add(d.getConversations());
        }

        long totalTokensSum = tokens.stream().mapToLong(Long::longValue).sum();
        int totalConversationsSum = conversations.stream().mapToInt(Integer::intValue).sum();

        BigDecimal dailyAvg = totalConversationsSum > 0
                ? BigDecimal.valueOf(totalTokensSum).divide(BigDecimal.valueOf(totalConversationsSum), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("tokens", tokens);
        result.put("conversations", conversations);
        result.put("totalTokens", totalTokensSum);
        result.put("totalConversations", totalConversationsSum);
        result.put("avgTokens", dailyAvg);
        result.put("projectedMonthlyTokens", dailyAvg.multiply(BigDecimal.valueOf(daysInMonth)));
        result.put("monthProgress", BigDecimal.valueOf(today.getDayOfMonth())
                .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)));
        return result;
    }
}
