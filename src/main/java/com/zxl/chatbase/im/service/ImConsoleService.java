package com.zxl.chatbase.im.service;

import com.zxl.chatbase.im.dto.ConsoleOverviewVO;
import com.zxl.chatbase.im.dto.ConversationSummaryVO;
import com.zxl.chatbase.im.dto.GroupMessagePageVO;
import com.zxl.chatbase.im.dto.GroupSummaryVO;

import java.util.List;

/**
 * 控制台：群聊/单聊采集与机器人状态（供 Web 管理端）
 */
public interface ImConsoleService {

    ConsoleOverviewVO overview(String userId);

    List<GroupSummaryVO> listGroups(String platform, String userId, String scope);

    GroupMessagePageVO pageMessages(String platform, String groupId, int page, int size, String keyword, String userId);

    List<ConversationSummaryVO> listConversations(String userId);

    GroupMessagePageVO pagePrivateMessages(String conversationId, int page, int size, String keyword, String userId);
}
