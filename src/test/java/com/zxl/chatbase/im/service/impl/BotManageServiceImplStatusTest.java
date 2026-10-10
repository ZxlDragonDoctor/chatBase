package com.zxl.chatbase.im.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxl.chatbase.im.mapper.BotManageMapper;
import com.zxl.chatbase.qq.QqBotProperties;
import com.zxl.chatbase.wx.service.WxIlinkService;
import com.zxl.chatbase.wx.config.WxProperties;
import com.zxl.chatbase.wxroboot.webhook.config.WXBizJsonMsgCryptConfig;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BotManageServiceImplStatusTest {

    @Test
    void disconnectedWxAndUnconfiguredWecomAreNotReportedOnline() {
        QqBotProperties qqProperties = mock(QqBotProperties.class);
        WXBizJsonMsgCryptConfig wecomConfig = mock(WXBizJsonMsgCryptConfig.class);
        BotManageMapper botMapper = mock(BotManageMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        WxProperties wxProperties = mock(WxProperties.class);
        WxIlinkService wxIlinkService = mock(WxIlinkService.class);

        when(qqProperties.getHttpBaseUrl()).thenReturn("");
        when(qqProperties.getNickname()).thenReturn("QQ Bot");
        when(redis.opsForValue()).thenReturn(values);
        when(wecomConfig.getBotName()).thenReturn("WeCom Bot");
        when(wecomConfig.getWxcpt()).thenReturn(null);
        when(wxIlinkService.isOnline()).thenReturn(false);
        when(wxIlinkService.hasCredentials()).thenReturn(true);
        when(wxIlinkService.getLoginNickname()).thenReturn("WX Bot");

        BotManageServiceImpl service = new BotManageServiceImpl(
                qqProperties, wecomConfig, botMapper, redis, restTemplate, wxProperties, wxIlinkService);

        JsonNode bots = new ObjectMapper().valueToTree(service.listBots("admin"));
        JsonNode wecom = findBot(bots, "wecom");
        JsonNode wx = findBot(bots, "wx");

        assertFalse(wecom.path("online").asBoolean());
        assertEquals("未配置", wecom.path("statusLabel").asText());
        assertFalse(wx.path("online").asBoolean());
    }

    private JsonNode findBot(JsonNode bots, String platform) {
        for (JsonNode bot : bots) {
            if (platform.equals(bot.path("platform").asText())) return bot;
        }
        throw new AssertionError("找不到平台机器人: " + platform);
    }
}
