package com.zxl.chatbase.im.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxl.chatbase.dify.server.DifyService;
import com.zxl.chatbase.im.entity.GroupKbMapping;
import com.zxl.chatbase.im.entity.GroupMessage;
import com.zxl.chatbase.im.mapper.GroupMessageMapper;
import com.zxl.chatbase.im.service.GroupKbMappingService;
import com.zxl.chatbase.im.service.GroupMessageSyncService;
import com.zxl.chatbase.im.service.ImSyncService;
import com.zxl.chatbase.kb.entity.KbDocument;
import com.zxl.chatbase.kb.entity.KbKnowledgeBase;
import com.zxl.chatbase.kb.mapper.KbDocumentMapper;
import com.zxl.chatbase.kb.mapper.KbKnowledgeBaseMapper;
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
public class ImSyncServiceImpl implements ImSyncService {

    private final GroupMessageMapper groupMessageMapper;
    private final GroupKbMappingService groupKbMappingService;
    private final KbKnowledgeBaseMapper kbKnowledgeBaseMapper;
    private final KbDocumentMapper kbDocumentMapper;
    private final DifyService difyService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter CONTENT_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH/mm/ss");
    private static final int BATCH_SIZE = 200;

    @Override
    @Scheduled(fixedDelayString = "60000")
    public void syncGroupMessagesToKnowledgeBase() {
        List<KbKnowledgeBase> syncableKbs = getSyncableKnowledgeBases();
        if (syncableKbs.isEmpty()) {
            log.debug("没有配置自动同步的知识库");
            return;
        }

        for (KbKnowledgeBase kb : syncableKbs) {
            try {
                syncByKnowledgeBase(kb.getId());
            } catch (Exception e) {
                log.error("同步知识库[{}]失败", kb.getId(), e);
            }
        }
    }

    private List<KbKnowledgeBase> getSyncableKnowledgeBases() {
        LambdaQueryWrapper<KbKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbKnowledgeBase::getAutoSync, true);
        wrapper.eq(KbKnowledgeBase::getStatus, true);
        wrapper.eq(KbKnowledgeBase::getSourceType, "im_sync");
        return kbKnowledgeBaseMapper.selectList(wrapper);
    }

    @Override
    public void syncByKnowledgeBase(Long knowledgeBaseId) {
        KbKnowledgeBase kb = kbKnowledgeBaseMapper.selectById(knowledgeBaseId);
        if (kb == null || !Boolean.TRUE.equals(kb.getStatus())) {
            log.warn("知识库不存在或已禁用: {}", knowledgeBaseId);
            return;
        }

        String platform = kb.getSyncPlatform();
        List<String> groupIds = parseGroupIds(kb.getSyncGroupIds());

        LambdaQueryWrapper<GroupMessage> wrapper = new LambdaQueryWrapper<>();
        if (platform != null && !platform.isEmpty()) {
            wrapper.eq(GroupMessage::getPlatform, platform);
        }
        if (!groupIds.isEmpty()) {
            wrapper.in(GroupMessage::getGroupId, groupIds);
        }
        wrapper.eq(GroupMessage::getSynced, false)
                .orderByAsc(GroupMessage::getMessageTime)
                .last("LIMIT " + BATCH_SIZE);

        List<GroupMessage> unsyncedMessages = groupMessageMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(unsyncedMessages)) {
            log.debug("知识库[{}]没有待同步消息", knowledgeBaseId);
            return;
        }

        Map<String, List<GroupMessage>> groupedMessages = unsyncedMessages.stream()
                .filter(msg -> msg.getGroupId() != null)
                .collect(Collectors.groupingBy(GroupMessage::getGroupId));

        log.info("开始同步群消息到知识库[{}]，消息数={}, 群数量={}", knowledgeBaseId, unsyncedMessages.size(), groupedMessages.size());

        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<String, List<GroupMessage>> entry : groupedMessages.entrySet()) {
            String groupId = entry.getKey();
            List<GroupMessage> messages = entry.getValue();

            try {
                boolean success = syncMessagesForGroup(kb, groupId, messages);
                if (success) {
                    successCount += messages.size();
                } else {
                    failCount += messages.size();
                }
            } catch (Exception e) {
                failCount += messages.size();
                log.error("同步群[{}]消息失败", groupId, e);
            }
        }

        log.info("知识库[{}]同步完成，成功={}, 失败={}", knowledgeBaseId, successCount, failCount);
    }

    private boolean syncMessagesForGroup(KbKnowledgeBase kb, String groupId, List<GroupMessage> messages) {
        List<GroupMessage> allMessages = new ArrayList<>();

        LambdaQueryWrapper<GroupMessage> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(GroupMessage::getGroupId, groupId)
                .eq(GroupMessage::getSynced, true)
                .orderByAsc(GroupMessage::getMessageTime);
        List<GroupMessage> syncedMessages = groupMessageMapper.selectList(allWrapper);
        allMessages.addAll(syncedMessages);
        allMessages.addAll(messages);

        allMessages.sort(Comparator.comparing(
                GroupMessage::getMessageTime,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        String content = buildMessageContent(allMessages);

        LocalDateTime earliestTime = allMessages.stream()
                .map(GroupMessage::getMessageTime)
                .filter(t -> t != null)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        String title = "群聊同步-" + groupId + "-" +
                (earliestTime != null ? earliestTime.format(CONTENT_FORMATTER) : String.valueOf(System.currentTimeMillis()));

        String existingDocId = syncedMessages.stream()
                .map(GroupMessage::getKbDocumentId)
                .filter(id -> id != null && !id.isEmpty())
                .findFirst()
                .orElse(null);

        boolean success;
        String documentId;

        if (existingDocId != null) {
            log.info("群[{}]已有文档[{}]，更新内容", groupId, existingDocId);
            success = difyService.updateDatasetDocument(existingDocId, title, content);
            documentId = existingDocId;
        } else {
            log.info("群[{}]无文档，创建新文档", groupId);
            documentId = difyService.createDatasetDocument(title, content);
            success = (documentId != null);

            if (success) {
                saveGroupKbMapping(groupId, documentId);
            }
        }

        if (success) {
            for (GroupMessage msg : messages) {
                msg.setSynced(true);
                msg.setKbDocumentId(documentId);
            }
            updateBatchById(messages);

            saveKbDocument(kb.getId(), title, content, documentId, messages);

            log.info("群[{}]同步成功，文档ID={}", groupId, documentId);
        }

        return success;
    }

    private String buildMessageContent(List<GroupMessage> messages) {
        return messages.stream()
                .map(msg -> {
                    String time = msg.getMessageTime() != null
                            ? msg.getMessageTime().format(CONTENT_FORMATTER)
                            : "";
                    return String.format("[%s][user=%s]: %s", time, msg.getUserId(), msg.getRawMessage());
                })
                .collect(Collectors.joining("\n"));
    }

    private List<String> parseGroupIds(String syncGroupIds) {
        if (syncGroupIds == null || syncGroupIds.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(syncGroupIds, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析 syncGroupIds 失败: {}", syncGroupIds);
            return new ArrayList<>();
        }
    }

    private void saveGroupKbMapping(String groupId, String documentId) {
        GroupKbMapping mapping = new GroupKbMapping(groupId, documentId);
        groupKbMappingService.save(mapping);
    }

    private void saveKbDocument(Long knowledgeBaseId, String title, String content,
                                 String difyDocumentId, List<GroupMessage> sourceMessages) {
        KbDocument doc = new KbDocument();
        doc.setKnowledgeBaseId(knowledgeBaseId);
        doc.setTitle(title);
        doc.setContent(content);
        doc.setDifyDocumentId(difyDocumentId);
        doc.setDifyStatus("completed");
        doc.setSource("im_sync");
        doc.setSyncStatus(true);
        doc.setSyncTime(LocalDateTime.now());
        doc.setStatus(true);
        doc.setCreateTime(LocalDateTime.now());
        doc.setUpdateTime(LocalDateTime.now());

        if (!sourceMessages.isEmpty()) {
            doc.setSourceMessageId(sourceMessages.get(0).getMessageId());
        }

        kbDocumentMapper.insert(doc);
    }

    private void updateBatchById(List<GroupMessage> messages) {
        for (GroupMessage msg : messages) {
            groupMessageMapper.updateById(msg);
        }
    }

    @Override
    public Page<KbKnowledgeBase> listSyncableGroups() {
        LambdaQueryWrapper<KbKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbKnowledgeBase::getSourceType, "im_sync")
                .eq(KbKnowledgeBase::getStatus, true)
                .orderByDesc(KbKnowledgeBase::getCreateTime);
        return new Page<>(1, 100);
    }
}
