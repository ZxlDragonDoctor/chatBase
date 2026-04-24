package com.zxl.chatbase.im.service;

import com.zxl.chatbase.im.dto.ConsoleOverviewVO;
import com.zxl.chatbase.im.dto.GroupMessagePageVO;
import com.zxl.chatbase.im.dto.GroupSummaryVO;

import java.util.List;

/**
 * 控制台：群聊采集与机器人状态（供 Web 管理端）
 */
public interface ImConsoleService {

    ConsoleOverviewVO overview();

    List<GroupSummaryVO> listGroups(String platform);

    GroupMessagePageVO pageMessages(String platform, String groupId, int page, int size, String keyword);
}
