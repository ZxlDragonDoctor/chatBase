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

import java.time.LocalDateTime;
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

    @Override
    public List<BotInfoVO> listBots() {
        List<BotInfoVO> bots = new ArrayList<>();
        bots.add(buildQqBot());
        bots.add(buildWeComBot());
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
