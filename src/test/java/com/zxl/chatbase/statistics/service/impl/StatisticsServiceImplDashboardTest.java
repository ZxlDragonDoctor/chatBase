package com.zxl.chatbase.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxl.chatbase.im.entity.GroupMessage;
import com.zxl.chatbase.im.mapper.GroupMessageMapper;
import com.zxl.chatbase.im.mapper.ImGroupMapper;
import com.zxl.chatbase.kb.entity.KbConversation;
import com.zxl.chatbase.kb.mapper.KbAppMapper;
import com.zxl.chatbase.kb.mapper.KbConversationMapper;
import com.zxl.chatbase.kb.mapper.KbDocumentMapper;
import com.zxl.chatbase.kb.mapper.KbKnowledgeBaseMapper;
import com.zxl.chatbase.kb.mapper.KbStatisticsMapper;
import com.zxl.chatbase.kb.mapper.SysUserMapper;
import com.zxl.chatbase.wx.config.WxProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StatisticsServiceImplDashboardTest {

    @Test
    void overviewUsesTodayForDailyFiguresAndThirtyDayWindowForActiveGroups() throws Exception {
        KbConversationMapper conversationMapper = mock(KbConversationMapper.class);
        KbStatisticsMapper statisticsMapper = mock(KbStatisticsMapper.class);
        GroupMessageMapper groupMessageMapper = mock(GroupMessageMapper.class);
        KbKnowledgeBaseMapper knowledgeBaseMapper = mock(KbKnowledgeBaseMapper.class);
        KbDocumentMapper documentMapper = mock(KbDocumentMapper.class);
        KbAppMapper appMapper = mock(KbAppMapper.class);
        ImGroupMapper imGroupMapper = mock(ImGroupMapper.class);
        SysUserMapper sysUserMapper = mock(SysUserMapper.class);
        WxProperties wxProperties = mock(WxProperties.class);

        when(conversationMapper.selectMaps(any())).thenReturn(List.of(Map.of(
                "total", 12L,
                "totalTokens", 900L,
                "todayConversations", 3L,
                "todayTokens", 240L,
                "avgLatency", 150L
        )));
        when(conversationMapper.selectCount(any())).thenReturn(11L, 2L);
        when(groupMessageMapper.selectCount(any())).thenReturn(40L);
        when(groupMessageMapper.selectMaps(any())).thenReturn(List.of(Map.of(
                "activeGroups", 5,
                "activeUsers", 9
        )));
        when(knowledgeBaseMapper.selectCount(any())).thenReturn(2L);
        when(documentMapper.selectCount(any())).thenReturn(7L);
        when(wxProperties.isEnable()).thenReturn(false);
        when(wxProperties.getBotToken()).thenReturn("");

        StatisticsServiceImpl service = new StatisticsServiceImpl(
                conversationMapper, statisticsMapper, groupMessageMapper, knowledgeBaseMapper,
                documentMapper, appMapper, imGroupMapper, sysUserMapper, wxProperties);

        JsonNode response = new ObjectMapper().valueToTree(service.getSystemOverview(null));

        assertEquals(3, response.path("todayConversations").asInt(-1));
        assertEquals(240, response.path("todayTokens").asInt(-1));
        assertEquals(66.67, response.path("todaySuccessRate").asDouble(-1), 0.001);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<QueryWrapper<KbConversation>> conversationWrapperCaptor =
                ArgumentCaptor.forClass((Class<QueryWrapper<KbConversation>>) (Class<?>) QueryWrapper.class);
        verify(conversationMapper, atLeastOnce()).selectMaps(conversationWrapperCaptor.capture());
        LocalDate today = LocalDate.now();
        assertTrue(conversationWrapperCaptor.getAllValues().stream().anyMatch(wrapper ->
                wrapper.getSqlSelect().contains("todayConversations")
                        && wrapper.getSqlSegment().contains("create_time")
                        && wrapper.getParamNameValuePairs().containsValue(today.atStartOfDay())
                        && wrapper.getParamNameValuePairs().containsValue(today.plusDays(1).atStartOfDay())));
        assertTrue(conversationWrapperCaptor.getAllValues().stream().anyMatch(wrapper ->
                wrapper.getSqlSelect().contains("avgLatency")
                        && wrapper.getSqlSegment().contains("latency_ms >")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<QueryWrapper<GroupMessage>> groupWrapperCaptor =
                ArgumentCaptor.forClass((Class<QueryWrapper<GroupMessage>>) (Class<?>) QueryWrapper.class);
        verify(groupMessageMapper).selectMaps(groupWrapperCaptor.capture());
        QueryWrapper<GroupMessage> groupWrapper = groupWrapperCaptor.getValue();
        String groupSql = groupWrapper.getSqlSegment().toLowerCase();
        assertTrue(groupSql.contains("message_time"));
        assertTrue(groupSql.contains("conversation_type"));
        assertTrue(groupSql.contains("group_id <>"));
        assertTrue(groupWrapper.getParamNameValuePairs().containsValue(LocalDate.now().minusDays(29).atStartOfDay()));
    }
}
