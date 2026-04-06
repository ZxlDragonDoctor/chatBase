package com.zxl.chatbase.im.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxl.chatbase.dify.server.DifyService;
import com.zxl.chatbase.im.entity.GroupKbMapping;
import com.zxl.chatbase.im.entity.GroupMessage;
import com.zxl.chatbase.im.mapper.GroupMessageMapper;
import com.zxl.chatbase.im.service.GroupKbMappingService;
import com.zxl.chatbase.im.service.GroupMessageSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 群消息增量同步到知识库的简单实现
 * <p>
 * <p>
 * - 定时扫描未同步的 group_message 记录
 * - 将文本内容聚合为一段字符串（对接 Dify 知识库 API）
 * - 标记为已同步
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupMessageSyncServiceImpl extends ServiceImpl<GroupMessageMapper, GroupMessage> implements GroupMessageSyncService {

    private final GroupMessageMapper groupMessageMapper;
    private final GroupKbMappingService groupKbMappingService;
    private final DifyService difyService;

    private static final DateTimeFormatter CONTENT_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH/mm/ss");

    private static final int BATCH_SIZE = 200;

    //    @Deprecated
//    @Override
//    @Scheduled(fixedDelayString = "60000")
//    public void syncToKnowledgeBase() {
//        // 查询一批未同步的消息
//        LambdaQueryWrapper<GroupMessage> wrapper = new LambdaQueryWrapper<GroupMessage>()
//                .eq(GroupMessage::getSynced, false)
//                .orderByAsc(GroupMessage::getMessageTime)
//                .last("LIMIT " + BATCH_SIZE);
//
//        List<GroupMessage> list = groupMessageMapper.selectList(wrapper);
//        if (CollectionUtils.isEmpty(list)) {
//            return;
//        }
//
//        Map<String,List<GroupMessage>> groupMessageMap = list.stream()
//                        .collect(Collectors.groupingBy(GroupMessage::getGroupId));
//
//        log.info("开始同步群消息到Dify知识库，本次数量={}", list.size());
//
//        for(Map.Entry<String,List<GroupMessage>> listEntry : groupMessageMap.entrySet()){
//            // 逐条处理
//            List<GroupMessage> listGroupMessage = listEntry.getValue();
//            // 把这些消息按时间顺序拼成一篇长文本
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//            String content = listGroupMessage.stream()
//                    .sorted(Comparator.comparing(GroupMessage::getMessageTime))
//                    .map(msg -> {
//                        String time = msg.getMessageTime() != null
//                                ? msg.getMessageTime().format(formatter)
//                                : "";
//                        return String.format("[%s][group=%s][user=%s]: %s",
//                                time,
//                                msg.getGroupId(),
//                                msg.getUserId(),
//                                msg.getRawMessage());
//                    })
//                    .collect(Collectors.joining("\n"));
//
//            // 将本批次群聊内容作为一个文档写入 Dify 知识库
//            String title = "群聊同步-" + listGroupMessage.get(0).getGroupId() + "-" +
//                    (listGroupMessage.get(0).getMessageTime() != null
//                            ? listGroupMessage.get(0).getMessageTime().format(DateTimeFormatter.ofPattern("yyyy/MM/dd/HH/mm/ss"))
//                            : System.currentTimeMillis());
//
//            //TODO: 这里需要追加写入到文档的分段，一个群聊一个文档,暂时写到一个文档里面
//            String documentId = difyService.createDatasetDocument(title, content);
//
//            if(documentId != null){
//                // 标记为已同步，并记录文档ID（如果有）
//                for (GroupMessage msg : listGroupMessage) {
//                    msg.setSynced(true);
//                    msg.setKbDocumentId(documentId);
//                    groupMessageMapper.updateById(msg);
//                }
//
//                log.info("本次群消息同步完成，已标记为 synced=true,documentId={}",documentId);
//            }
//        }
//    }
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

        Map<String, List<GroupMessage>> groupMessageMap = list.stream()
                .filter(msg -> msg.getGroupId() != null)
                .collect(Collectors.groupingBy(GroupMessage::getGroupId));

        log.info("开始同步群消息到Dify知识库，本次数量={}, 群数量={}", list.size(), groupMessageMap.size());

        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<String, List<GroupMessage>> listEntry : groupMessageMap.entrySet()) {
            String groupId = listEntry.getKey();
            List<GroupMessage> newMessages = listEntry.getValue();

            if (CollectionUtils.isEmpty(newMessages)) {
                continue;
            }

            try {
                // 查询该群所有已同步的消息（用于构建完整内容）
                LambdaQueryWrapper<GroupMessage> allWrapper = new LambdaQueryWrapper<GroupMessage>()
                        .eq(GroupMessage::getGroupId, groupId)
                        .eq(GroupMessage::getSynced, true)
                        .orderByAsc(GroupMessage::getMessageTime);

                List<GroupMessage> syncedMessages = groupMessageMapper.selectList(allWrapper);

                // 合并已同步和新消息，按时间排序
                List<GroupMessage> allMessages = new ArrayList<>();
                allMessages.addAll(syncedMessages);
                allMessages.addAll(newMessages);

                allMessages.sort(Comparator.comparing(
                        GroupMessage::getMessageTime,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ));

                // 构建完整的群聊内容
                String content = allMessages.stream()
                        .map(msg -> {
                            String time = msg.getMessageTime() != null
                                    ? msg.getMessageTime().format(CONTENT_FORMATTER)
                                    : "";
                            return String.format("[%s][user=%s]: %s",
                                    time,
                                    msg.getUserId(),
                                    msg.getRawMessage());
                        })
                        .collect(Collectors.joining("\n"));

                // 获取最早的消息时间用于标题
                LocalDateTime earliestTime = allMessages.stream()
                        .map(GroupMessage::getMessageTime)
                        .filter(t -> t != null)
                        .min(LocalDateTime::compareTo)
                        .orElse(null);

                String title = "群聊同步-" + groupId + "-" +
                        (earliestTime != null
                                ? earliestTime.format(CONTENT_FORMATTER)
                                : String.valueOf(System.currentTimeMillis()));

                // 检查该群是否已有文档ID
                String existingDocId = syncedMessages.stream()
                        .map(GroupMessage::getKbDocumentId)
                        .filter(id -> id != null && !id.isEmpty())
                        .findFirst()
                        .orElse(null);

                boolean success;
                String documentId;

                if (existingDocId != null) {
                    // 更新已有文档（覆盖式更新，但内容是完整的）
                    log.info("群[{}]已有文档[{}]，更新为完整内容（共{}条消息）",
                            groupId, existingDocId, allMessages.size());
                    success = difyService.updateDatasetDocument(existingDocId, title, content);
                    documentId = existingDocId;
                } else {
                    // 创建新文档
                    log.info("群[{}]无文档，创建新文档（共{}条消息）", groupId, allMessages.size());
                    documentId = difyService.createDatasetDocument(title, content);
                    // 记录映射关系
                    groupKbMappingService.save(new GroupKbMapping(Long.valueOf(documentId), groupId));
                    success = (documentId != null);
                }

                if (success) {
                    // 只标记新消息为已同步
                    for (GroupMessage msg : newMessages) {
                        msg.setSynced(true);
                        msg.setKbDocumentId(documentId);
                    }
                    // 批量更新新消息
                    this.updateBatchById(newMessages);

                    successCount += newMessages.size();
                    log.info("群[{}]同步完成，新增{}条，文档共{}条，documentId={}",
                            groupId, newMessages.size(), allMessages.size(), documentId);
                } else {
                    failCount += newMessages.size();
                    log.warn("群[{}]消息同步失败", groupId);
                }
            } catch (Exception e) {
                failCount += newMessages.size();
                log.error("群[{}]消息同步异常", groupId, e);
            }
        }

        log.info("本次群消息同步完成，成功{}条，失败{}条", successCount, failCount);
    }


    public void saveGroupMessage(String messageId, String groupId, String userId,
                                 String rawMessage, String messageType, long time) {
        try {
            GroupMessage gm = new GroupMessage();
            gm.setMessageId(messageId);
            gm.setGroupId(groupId);
            gm.setUserId(userId);
            gm.setMessageId(messageId);
            gm.setMessageType(messageType);
            gm.setRawMessage(rawMessage);
            if (time > 0) {
                gm.setMessageTime(java.time.LocalDateTime.ofEpochSecond(
                        time, 0, java.time.ZoneOffset.ofHours(8)));
            } else {
                gm.setMessageTime(java.time.LocalDateTime.now());
            }
            gm.setCreateTime(java.time.LocalDateTime.now());
            groupMessageMapper.insert(gm);
            log.info("群消息写库成功");
        } catch (Exception e) {
            log.error("保存群消息失败", e);
        }
    }

    public void saveGroupMessage(String userId, String rawMessage, String messageType, long time) {
        saveGroupMessage(null, null, userId, rawMessage, messageType, time);
    }

}

