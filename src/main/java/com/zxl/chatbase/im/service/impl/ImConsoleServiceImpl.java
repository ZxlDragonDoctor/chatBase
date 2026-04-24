package com.zxl.chatbase.im.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zxl.chatbase.im.dto.BotStatusVO;
import com.zxl.chatbase.im.dto.ConsoleOverviewVO;
import com.zxl.chatbase.im.dto.GroupMessageItemVO;
import com.zxl.chatbase.im.dto.GroupMessagePageVO;
import com.zxl.chatbase.im.dto.GroupSummaryVO;
import com.zxl.chatbase.im.entity.GroupMessage;
import com.zxl.chatbase.im.mapper.GroupMessageMapper;
import com.zxl.chatbase.im.service.ImConsoleService;
import com.zxl.chatbase.qq.QqBotProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImConsoleServiceImpl implements ImConsoleService {

    private final GroupMessageMapper groupMessageMapper;
    private final QqBotProperties qqBotProperties;

    @Override
    public ConsoleOverviewVO overview() {
        ConsoleOverviewVO vo = new ConsoleOverviewVO();
        vo.setTotalMessages(groupMessageMapper.selectCount(null));
        List<GroupSummaryVO> summaries = groupMessageMapper.selectGroupSummaries();
        vo.setDistinctGroups(summaries.size());

        Map<String, Long> msgByPlat = new HashMap<>();
        Map<String, Integer> grpByPlat = new HashMap<>();
        for (GroupSummaryVO s : summaries) {
            String p = s.getPlatform() == null ? "unknown" : s.getPlatform();
            long cnt = s.getMessageCount() == null ? 0L : s.getMessageCount();
            msgByPlat.merge(p, cnt, Long::sum);
            grpByPlat.merge(p, 1, Integer::sum);
        }
        vo.setMessageCountByPlatform(msgByPlat);
        vo.setGroupCountByPlatform(grpByPlat);
        vo.setBots(buildBots());
        return vo;
    }

    @Override
    public List<GroupSummaryVO> listGroups(String platform) {
        List<GroupSummaryVO> all = groupMessageMapper.selectGroupSummaries();
        if (!StringUtils.hasText(platform) || "all".equalsIgnoreCase(platform)) {
            return all;
        }
        return all.stream().filter(s -> platformMatches(s.getPlatform(), platform)).collect(Collectors.toList());
    }

    @Override
    public GroupMessagePageVO pageMessages(String platform, String groupId, int page, int size, String keyword) {
        if (!StringUtils.hasText(groupId)) {
            return new GroupMessagePageVO(List.of(), 0, Math.max(page, 0), Math.max(size, 1));
        }
        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = 20;
        }
        if (size > 200) {
            size = 200;
        }

        Page<GroupMessage> mp = new Page<>(page + 1L, size);
        LambdaQueryWrapper<GroupMessage> w = new LambdaQueryWrapper<GroupMessage>()
                .eq(GroupMessage::getGroupId, groupId);
        if (StringUtils.hasText(platform) && !"all".equalsIgnoreCase(platform)) {
            if ("wecom".equalsIgnoreCase(platform) || "wx".equalsIgnoreCase(platform)) {
                w.eq(GroupMessage::getPlatform, "wecom");
            } else {
                w.eq(GroupMessage::getPlatform, platform);
            }
        }
        if (StringUtils.hasText(keyword)) {
            w.and(wrapper -> wrapper
                    .like(GroupMessage::getRawMessage, keyword)
                    .or()
                    .like(GroupMessage::getUserId, keyword)
            );
        }
        w.orderByDesc(GroupMessage::getMessageTime);

        IPage<GroupMessage> result = groupMessageMapper.selectPage(mp, w);
        List<GroupMessageItemVO> items = result.getRecords().stream()
                .map(this::toItem)
                .collect(Collectors.toList());
        return new GroupMessagePageVO(items, result.getTotal(), page, size);
    }

    private static boolean platformMatches(String actual, String filter) {
        if (!StringUtils.hasText(filter) || "all".equalsIgnoreCase(filter)) {
            return true;
        }
        String a = actual == null ? "" : actual;
        if (filter.equalsIgnoreCase(a)) {
            return true;
        }
        return ("wecom".equalsIgnoreCase(filter) || "wx".equalsIgnoreCase(filter)) && "wecom".equalsIgnoreCase(a);
    }

    private GroupMessageItemVO toItem(GroupMessage m) {
        GroupMessageItemVO v = new GroupMessageItemVO();
        v.setId(m.getId());
        v.setPlatform(m.getPlatform());
        v.setGroupId(m.getGroupId());
        v.setUserId(m.getUserId());
        v.setMessageType(m.getMessageType());
        v.setRawMessage(m.getRawMessage());
        v.setMessageTime(m.getMessageTime());
        v.setSynced(m.getSynced());
        return v;
    }

    private BotStatusVO buildBots() {
        BotStatusVO vo = new BotStatusVO();
        BotStatusVO.QqBotVO qq = new BotStatusVO.QqBotVO();
        qq.setEnabled(qqBotProperties.isEnable());
        qq.setSelfId(qqBotProperties.getSelfId());
        qq.setWsPort(qqBotProperties.getWsPort());
        String http = qqBotProperties.getHttpBaseUrl();
        qq.setHttpConfigured(StringUtils.hasText(http));
        qq.setHttpBaseUrlPreview(shortenUrl(http));
        vo.setQq(qq);

        BotStatusVO.WeComBotVO we = new BotStatusVO.WeComBotVO();
        we.setCallbackPath("/intellrobot/callback/handle");
        we.setNote("企业微信智能机器人回调；需在企微后台配置可公网访问的 URL，并与此路径一致。");
        vo.setWecom(we);
        return vo;
    }

    private static String shortenUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        String t = url.trim();
        if (t.length() <= 64) {
            return t;
        }
        return t.substring(0, 32) + "…" + t.substring(t.length() - 24);
    }
}
