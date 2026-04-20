package com.zxl.chatbase.im.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxl.chatbase.dify.server.DifyService;
import com.zxl.chatbase.im.entity.GroupKbMapping;
import com.zxl.chatbase.im.entity.GroupMessage;
import com.zxl.chatbase.im.mapper.GroupMessageMapper;
import com.zxl.chatbase.im.service.GroupKbMappingService;
import com.zxl.chatbase.im.service.GroupMessageSyncService;
import com.zxl.chatbase.kb.entity.KbDocument;
import com.zxl.chatbase.kb.entity.KbKnowledgeBase;
import com.zxl.chatbase.kb.service.IKbDocumentService;
import com.zxl.chatbase.kb.service.IKbKnowledgeBaseService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupMessageSyncServiceImpl extends ServiceImpl<GroupMessageMapper, GroupMessage> implements GroupMessageSyncService {

    private final GroupMessageMapper groupMessageMapper;
    private final GroupKbMappingService groupKbMappingService;
    private final DifyService difyService;
    private final IKbKnowledgeBaseService knowledgeBaseService;
    private final IKbDocumentService documentService;

    private static final DateTimeFormatter CONTENT_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH/mm/ss");

    private static final int BATCH_SIZE = 200;

    @Override
    @Scheduled(fixedDelayString = "60000")
    public void syncToKnowledgeBase() {
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
                LambdaQueryWrapper<GroupMessage> allWrapper = new LambdaQueryWrapper<GroupMessage>()
                        .eq(GroupMessage::getGroupId, groupId)
                        .eq(GroupMessage::getSynced, true)
                        .orderByAsc(GroupMessage::getMessageTime);

                List<GroupMessage> syncedMessages = groupMessageMapper.selectList(allWrapper);

                List<GroupMessage> allMessages = new ArrayList<>();
                allMessages.addAll(syncedMessages);
                allMessages.addAll(newMessages);

                allMessages.sort(Comparator.comparing(
                        GroupMessage::getMessageTime,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ));

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

                LocalDateTime earliestTime = allMessages.stream()
                        .map(GroupMessage::getMessageTime)
                        .filter(t -> t != null)
                        .min(LocalDateTime::compareTo)
                        .orElse(null);

                String title = "群聊同步-" + groupId + "-" +
                        (earliestTime != null
                                ? earliestTime.format(CONTENT_FORMATTER)
                                : String.valueOf(System.currentTimeMillis()));

                String existingDocId = syncedMessages.stream()
                        .map(GroupMessage::getKbDocumentId)
                        .filter(id -> id != null && !id.isEmpty())
                        .findFirst()
                        .orElse(null);

                boolean success;
                String documentId;

                if (existingDocId != null) {
                    log.info("群[{}]已有文档[{}]，更新为完整内容（共{}条消息）",
                            groupId, existingDocId, allMessages.size());
                    success = difyService.updateDatasetDocument(existingDocId, title, content);
                    documentId = existingDocId;
                } else {
                    log.info("群[{}]无文档，创建新文档（共{}条消息）", groupId, allMessages.size());
                    documentId = difyService.createDatasetDocument(title, content);
                    groupKbMappingService.save(new GroupKbMapping(documentId, groupId));
                    success = (documentId != null);
                }

                if (success) {
                    for (GroupMessage msg : newMessages) {
                        msg.setSynced(true);
                        msg.setKbDocumentId(documentId);
                    }
                    this.updateBatchById(newMessages);

                    KbKnowledgeBase imKb = findOrCreateImSyncKnowledgeBase(groupId);
                    if (imKb != null && documentId != null) {
                        saveToKbDocument(imKb, title, content, documentId, groupId);
                    }

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

    private KbKnowledgeBase findOrCreateImSyncKnowledgeBase(String groupId) {
        LambdaQueryWrapper<KbKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbKnowledgeBase::getSourceType, "im_sync")
                .like(KbKnowledgeBase::getSyncGroupIds, groupId);
        KbKnowledgeBase kb = knowledgeBaseService.getOne(wrapper);

        if (kb == null) {
            kb = new KbKnowledgeBase();
            kb.setName("群聊同步-" + groupId);
            kb.setSourceType("im_sync");
            kb.setSyncGroupIds("[\"" + groupId + "\"]");
            kb.setDocCount(0);
            kb.setStatus(true);
            kb.setCreateTime(LocalDateTime.now());
            kb.setUpdateTime(LocalDateTime.now());

            String difyDatasetId = difyService.createDataset(kb.getName(), "IM群聊消息自动同步");
            kb.setDifyDatasetId(difyDatasetId);

            knowledgeBaseService.save(kb);
            log.info("创建IM同步知识库: id={}, groupId={}", kb.getId(), groupId);
        }

        return kb;
    }

    private void saveToKbDocument(KbKnowledgeBase kb, String title, String content, String difyDocId, String groupId) {
        LambdaQueryWrapper<KbDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbDocument::getKnowledgeBaseId, kb.getId())
                .eq(KbDocument::getDifyDocumentId, difyDocId);
        KbDocument existingDoc = documentService.getOne(wrapper);

        if (existingDoc != null) {
            existingDoc.setContent(content);
            existingDoc.setUpdateTime(LocalDateTime.now());
            documentService.updateById(existingDoc);
            log.info("更新kb_document: id={}, groupId={}", existingDoc.getId(), groupId);
        } else {
            KbDocument kbDoc = new KbDocument();
            kbDoc.setKnowledgeBaseId(kb.getId());
            kbDoc.setTitle(title);
            kbDoc.setContent(content);
            kbDoc.setDifyDocumentId(difyDocId);
            kbDoc.setSource("im_sync");
            kbDoc.setSyncStatus(true);
            kbDoc.setSyncTime(LocalDateTime.now());
            kbDoc.setDifyStatus("completed");
            kbDoc.setCreateTime(LocalDateTime.now());
            kbDoc.setUpdateTime(LocalDateTime.now());
            kbDoc.setStatus(true);

            documentService.save(kbDoc);

            kb.setDocCount(kb.getDocCount() != null ? kb.getDocCount() + 1 : 1);
            kb.setUpdateTime(LocalDateTime.now());
            knowledgeBaseService.updateById(kb);

            log.info("创建kb_document: id={}, groupId={}", kbDoc.getId(), groupId);
        }
    }

    public void saveGroupMessage(String messageId, String groupId, String userId,
                                 String rawMessage, String messageType, long time) {
        try {
            GroupMessage gm = new GroupMessage();
            gm.setMessageId(messageId);
            gm.setGroupId(groupId);
            gm.setUserId(userId);
            gm.setPlatform("qq");
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