package com.zxl.chatbase.im.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.zxl.chatbase.im.dto.BotInfoVO;
import com.zxl.chatbase.im.entity.ImGroup;
import com.zxl.chatbase.im.mapper.BotManageMapper;
import com.zxl.chatbase.im.mapper.ImGroupMapper;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotManageServiceImpl implements BotManageService {

    private final QqBotProperties qqBotProperties;
    private final WXBizJsonMsgCryptConfig wechatConfig;
    private final BotManageMapper botManageMapper;
    private final ImGroupMapper imGroupMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RestTemplate restTemplate;
    private final com.zxl.chatbase.wx.config.WxProperties wxProperties;

    @Override
    public List<BotInfoVO> listBots(String userId) {
        List<String> userGroupIds = imGroupMapper.selectList(
                new LambdaQueryWrapper<ImGroup>()
                        .eq(ImGroup::getCreatedBy, userId)
                        .select(ImGroup::getGroupId)
        ).stream().map(ImGroup::getGroupId).collect(Collectors.toList());

        List<BotInfoVO> bots = new ArrayList<>();
        bots.add(buildQqBot(userGroupIds));
        bots.add(buildWeComBot(userGroupIds));
        bots.add(buildWxBot(userGroupIds));
        return bots;
    }

    private BotInfoVO buildQqBot(List<String> groupIds) {
        return BotInfoVO.builder()
                .platform("qq")
                .name(getQqBotNickname())
                .botId(String.valueOf(qqBotProperties.getSelfId()))
                .online(isQqOnline())
                .groupCount(groupIds.isEmpty() ? 0 : botManageMapper.countGroups("qq", groupIds))
                .todayMessages(groupIds.isEmpty() ? 0 : botManageMapper.countTodayMessages("qq", groupIds))
                .totalMessages(groupIds.isEmpty() ? 0 : botManageMapper.countTotalMessages("qq", groupIds))
                .lastActiveTime(groupIds.isEmpty() ? null : botManageMapper.getLastActiveTime("qq", groupIds))
                .build();
    }

    private BotInfoVO buildWeComBot(List<String> groupIds) {
        return BotInfoVO.builder()
                .platform("wecom")
                .name(wechatConfig.getBotName())
                .botId(null)
                .online(true)
                .groupCount(groupIds.isEmpty() ? 0 : botManageMapper.countGroups("wecom", groupIds))
                .todayMessages(groupIds.isEmpty() ? 0 : botManageMapper.countTodayMessages("wecom", groupIds))
                .totalMessages(groupIds.isEmpty() ? 0 : botManageMapper.countTotalMessages("wecom", groupIds))
                .lastActiveTime(groupIds.isEmpty() ? null : botManageMapper.getLastActiveTime("wecom", groupIds))
                .build();
    }

    private BotInfoVO buildWxBot(List<String> groupIds) {
        boolean online = "1".equals(stringRedisTemplate.opsForValue().get("bot:wx:online"));
        return BotInfoVO.builder()
                .platform("wx")
                .name(wxProperties.getNickname())
                .botId(wxProperties.getBotId())
                .online(online)
                .groupCount(groupIds.isEmpty() ? 0 : botManageMapper.countGroups("wx", groupIds))
                .todayMessages(groupIds.isEmpty() ? 0 : botManageMapper.countTodayMessages("wx", groupIds))
                .totalMessages(groupIds.isEmpty() ? 0 : botManageMapper.countTotalMessages("wx", groupIds))
                .lastActiveTime(groupIds.isEmpty() ? null : botManageMapper.getLastActiveTime("wx", groupIds))
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
