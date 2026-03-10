package com.zxl.chatbase.im.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zxl.chatbase.dify.server.DifyService;
import com.zxl.chatbase.im.entity.GroupMessage;
import com.zxl.chatbase.im.mapper.GroupMessageMapper;
import com.zxl.chatbase.im.service.GroupMessageSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 群消息增量同步到知识库的简单实现
 *
 * 这里先实现一个骨架：
 * - 定时扫描未同步的 group_message 记录
 * - 将文本内容聚合为一段字符串（后续可对接 Dify 知识库 API）
 * - 标记为已同步
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupMessageSyncServiceImpl implements GroupMessageSyncService {

    private final GroupMessageMapper groupMessageMapper;
    private final DifyService difyService;

    private static final int BATCH_SIZE = 200;

    @Override
    @Scheduled(fixedDelayString = "60000")
    public void syncToKnowledgeBase() {
        // 查询一批未同步的消息
        LambdaQueryWrapper<GroupMessage> wrapper = new LambdaQueryWrapper<GroupMessage>()
                .eq(GroupMessage::getSynced, false)
                .orderByAsc(GroupMessage::getMessageTime)
                .last("LIMIT " + BATCH_SIZE);

        List<GroupMessage> list = groupMessageMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        log.info("开始同步群消息到知识库，本次数量={}", list.size());

        // 简单示例：把这些消息按时间顺序拼成一篇长文本
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String content = list.stream()
                .map(msg -> {
                    String time = msg.getMessageTime() != null
                            ? msg.getMessageTime().format(formatter)
                            : "";
                    return String.format("[%s][group=%s][user=%s]: %s",
                            time,
                            msg.getGroupId(),
                            msg.getUserId(),
                            msg.getRawMessage());
                })
                .collect(Collectors.joining("\n"));

        // 将本批次群聊内容作为一个文档写入 Dify 知识库
        String title = "群聊同步-" + list.get(0).getGroupId() + "-" +
                (list.get(0).getMessageTime() != null
                        ? list.get(0).getMessageTime().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                        : System.currentTimeMillis());

        String documentId = difyService.createDatasetDocument(title, content);

        // 标记为已同步，并记录文档ID（如果有）
        for (GroupMessage msg : list) {
            msg.setSynced(true);
            if (documentId != null) {
                msg.setKbDocumentId(documentId);
            }
            groupMessageMapper.updateById(msg);
        }

        log.info("本次群消息同步完成，已标记为 synced=true");
    }
}

