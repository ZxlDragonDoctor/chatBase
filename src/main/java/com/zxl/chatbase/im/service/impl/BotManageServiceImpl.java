package com.zxl.chatbase.im.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.zxl.chatbase.im.dto.BotInfoVO;
import com.zxl.chatbase.im.mapper.BotManageMapper;
import com.zxl.chatbase.im.service.BotManageService;
import com.zxl.chatbase.qq.QqBotProperties;
import com.zxl.chatbase.wxroboot.webhook.config.WXBizJsonMsgCryptConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotManageServiceImpl implements BotManageService {

    private final QqBotProperties qqBotProperties;
    private final WXBizJsonMsgCryptConfig wechatConfig;
    private final BotManageMapper botManageMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RestTemplate restTemplate;
    private final com.zxl.chatbase.wx.config.WxProperties wxProperties;
    private final com.zxl.chatbase.wx.service.WxIlinkService wxIlinkService;

    @Override
    public List<BotInfoVO> listBots(String userId) {
        List<BotInfoVO> bots = new ArrayList<>();
        bots.add(buildQqBot());
        bots.add(buildWeComBot());
        bots.add(buildWxBot());
        return bots;
    }

    private BotInfoVO buildQqBot() {
        return BotInfoVO.builder()
                .platform("qq")
                .name(getQqBotNickname())
                .botId(String.valueOf(qqBotProperties.getSelfId()))
                .online(isQqOnline())
                .groupCount(botManageMapper.countGroups("qq"))
                .todayMessages(botManageMapper.countTodayMessages("qq"))
                .totalMessages(botManageMapper.countTotalMessages("qq"))
                .lastActiveTime(botManageMapper.getLastActiveTime("qq"))
                .build();
    }

    private BotInfoVO buildWeComBot() {
        return BotInfoVO.builder()
                .platform("wecom")
                .name(wechatConfig.getBotName())
                .botId(null)
                .online(true)
                .groupCount(botManageMapper.countGroups("wecom"))
                .todayMessages(botManageMapper.countTodayMessages("wecom"))
                .totalMessages(botManageMapper.countTotalMessages("wecom"))
                .lastActiveTime(botManageMapper.getLastActiveTime("wecom"))
                .build();
    }

    private BotInfoVO buildWxBot() {
        // 优先用 WxIlinkService：扫码成功后立即在线；同时校验凭证
        boolean online;
        String nickname;
        try {
            online = wxIlinkService.isOnline() || wxIlinkService.hasCredentials();
            if (online && !wxIlinkService.hasCredentials()) {
                online = false;
            }
            nickname = wxIlinkService.getLoginNickname();
        } catch (Exception e) {
            online = "1".equals(stringRedisTemplate.opsForValue().get("bot:wx:online"));
            String credentials = stringRedisTemplate.opsForValue().get("bot:wx:credentials");
            if (online && !StringUtils.hasText(credentials)) {
                online = false;
            }
            nickname = wxProperties.getNickname();
        }
        return BotInfoVO.builder()
                .platform("wx")
                .name(nickname)
                .botId(wxProperties.getBotId())
                .online(online)
                .groupCount(botManageMapper.countGroups("wx"))
                .todayMessages(botManageMapper.countTodayMessages("wx"))
                .totalMessages(botManageMapper.countTotalMessages("wx"))
                .lastActiveTime(botManageMapper.getLastActiveTime("wx"))
                .build();
    }

    private String getQqBotNickname() {
        String baseUrl = qqBotProperties.getHttpBaseUrl();
        if (StringUtils.hasText(baseUrl)) {
            try {
                String url = baseUrl + "/get_stranger_info?user_id=" + qqBotProperties.getSelfId();
                JsonNode resp = restTemplate.getForObject(url, JsonNode.class);
                if (resp != null && "ok".equals(resp.get("status").asText())) {
                    String nickname = resp.get("data").get("nickname").asText();
                    if (StringUtils.hasText(nickname)) {
                        return nickname;
                    }
                }
            } catch (Exception e) {
                log.debug("通过 API 获取 QQ 机器人昵称失败: {}", e.getMessage());
            }
        }

        if (StringUtils.hasText(qqBotProperties.getNickname())) {
            return qqBotProperties.getNickname();
        }

        return "QQ: " + qqBotProperties.getSelfId();
    }

    private boolean isQqOnline() {
        return "1".equals(stringRedisTemplate.opsForValue().get("bot:qq:online"));
    }
}