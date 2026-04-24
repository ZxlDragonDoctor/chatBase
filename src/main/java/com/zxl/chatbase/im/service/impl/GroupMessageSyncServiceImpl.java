package com.zxl.chatbase.im.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxl.chatbase.dify.server.DifyService;
import com.zxl.chatbase.im.entity.GroupMessage;
import com.zxl.chatbase.im.entity.ImGroup;
import com.zxl.chatbase.im.mapper.GroupMessageMapper;
import com.zxl.chatbase.im.mapper.ImGroupMapper;
import com.zxl.chatbase.im.service.GroupMessageSyncService;
import com.zxl.chatbase.kb.entity.KbCategory;
import com.zxl.chatbase.kb.entity.KbDocument;
import com.zxl.chatbase.kb.entity.KbKnowledgeBase;
import com.zxl.chatbase.kb.mapper.KbCategoryMapper;
import com.zxl.chatbase.kb.service.IKbDocumentService;
import com.zxl.chatbase.kb.service.IKbKnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 群聊消息同步服务
 * 所有群聊消息同步到统一的"群聊助手知识库"，每个群一个文档实现数据隔离
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupMessageSyncServiceImpl extends ServiceImpl<GroupMessageMapper, GroupMessage> implements GroupMessageSyncService {

    private final GroupMessageMapper groupMessageMapper;
    private final DifyService difyService;
    private final IKbKnowledgeBaseService knowledgeBaseService;
    private final IKbDocumentService documentService;
    private final ImGroupMapper imGroupMapper;
    private final KbCategoryMapper kbCategoryMapper;

    private static final DateTimeFormatter CONTENT_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH/mm/ss");
    private static final DateTimeFormatter TITLE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final int BATCH_SIZE = 200;

    private static final String IM_SYNC_KB_NAME = "群聊助手知识库";
    private static final String IM_SYNC_CATEGORY_NAME = "群聊消息";
    private static final String IM_SYNC_SOURCE_TYPE = "im_sync";

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

        KbKnowledgeBase imKb = findOrCreateImSyncKnowledgeBase();
        if (imKb == null || imKb.getDifyDatasetId() == null || imKb.getDifyDatasetId().isEmpty()) {
            log.error("无法获取有效的群聊助手知识库，本次同步终止");
            return;
        }

        for (Map.Entry<String, List<GroupMessage>> entry : groupMessageMap.entrySet()) {
            String groupId = entry.getKey();
            List<GroupMessage> newMessages = entry.getValue();

            if (CollectionUtils.isEmpty(newMessages)) {
                continue;
            }

            ImGroup groupInfo = getGroupInfo(groupId);
            String groupName = groupInfo != null && StringUtils.hasText(groupInfo.getGroupName())
                    ? groupInfo.getGroupName() : groupId;

            List<GroupMessage> fileMessages = newMessages.stream()
                    .filter(msg -> "image".equalsIgnoreCase(msg.getMessageType()) || "file".equalsIgnoreCase(msg.getMessageType()))
                    .collect(Collectors.toList());

            List<GroupMessage> textMessages = newMessages.stream()
                    .filter(msg -> !"image".equalsIgnoreCase(msg.getMessageType()) && !"file".equalsIgnoreCase(msg.getMessageType()))
                    .collect(Collectors.toList());

            for (GroupMessage msg : fileMessages) {
                try {
                    boolean success = syncFileMessage(imKb, msg, groupId, groupName);
                    if (success) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("同步群[{}]文件消息失败: messageId={}", groupId, msg.getMessageId(), e);
                }
            }

            if (!CollectionUtils.isEmpty(textMessages)) {
                try {
                    int count = syncTextMessages(imKb, textMessages, groupId, groupName);
                    successCount += count;
                    failCount += (textMessages.size() - count);
                } catch (Exception e) {
                    failCount += textMessages.size();
                    log.error("同步群[{}]文本消息失败", groupId, e);
                }
            }
        }

        log.info("本次群消息同步完成，成功{}条，失败{}条", successCount, failCount);
    }

    private boolean syncFileMessage(KbKnowledgeBase imKb, GroupMessage msg, String groupId, String groupName) {
        String fileUrl = msg.getFileUrl();

        if (fileUrl == null || fileUrl.isEmpty()) {
            log.warn("群[{}]文件消息缺少fileUrl，标记为已处理: messageId={}", groupName, msg.getMessageId());
            msg.setSynced(true);
            msg.setKbDocumentId(null);
            this.updateById(msg);
            return false;
        }

        String fileName = msg.getFileName() != null ? msg.getFileName() : "file_" + System.currentTimeMillis();
        String title = String.format("文件-%s-%s", groupName, fileName);

        log.info("群[{}]从URL创建文件文档: datasetId={}, fileUrl={}, fileName={}", 
                groupName, imKb.getDifyDatasetId(), fileUrl, fileName);

        String documentId = difyService.createDocumentByUrl(
                imKb.getDifyDatasetId(), 
                fileUrl, 
                fileName, 
                msg.getUserId()
        );

        if (documentId != null) {
            msg.setSynced(true);
            msg.setKbDocumentId(documentId);
            this.updateById(msg);
            log.info("文件文档同步成功: groupId={}, documentId={}", groupId, documentId);
            return true;
        } else {
            log.warn("文件文档创建失败（URL可能已过期），标记为已处理: groupId={}, messageId={}", groupId, msg.getMessageId());
            msg.setSynced(true);
            msg.setKbDocumentId(null);
            this.updateById(msg);
            return false;
        }
    }

    private int syncTextMessages(KbKnowledgeBase imKb, List<GroupMessage> newMessages, String groupId, String groupName) {
        LambdaQueryWrapper<GroupMessage> wrapper = new LambdaQueryWrapper<GroupMessage>()
                .eq(GroupMessage::getGroupId, groupId)
                .eq(GroupMessage::getSynced, true)
                .ne(GroupMessage::getMessageType, "image")
                .ne(GroupMessage::getMessageType, "file")
                .orderByAsc(GroupMessage::getMessageTime);

        List<GroupMessage> syncedTextMessages = groupMessageMapper.selectList(wrapper);

        List<GroupMessage> allTextMessages = new ArrayList<>();
        allTextMessages.addAll(syncedTextMessages);
        allTextMessages.addAll(newMessages);

        allTextMessages.sort(Comparator.comparing(
                GroupMessage::getMessageTime,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        String content = allTextMessages.stream()
                .map(m -> formatMessageLine(m))
                .collect(Collectors.joining("\n"));

        LocalDateTime earliestTime = allTextMessages.stream()
                .map(GroupMessage::getMessageTime)
                .filter(t -> t != null)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        String title = String.format("群聊同步-%s-%s",
                groupName,
                earliestTime != null ? earliestTime.format(TITLE_FORMATTER) : String.valueOf(System.currentTimeMillis()));

        String existingDocId = syncedTextMessages.stream()
                .map(GroupMessage::getKbDocumentId)
                .filter(id -> id != null && !id.isEmpty())
                .findFirst()
                .orElse(null);

        boolean success;
        String documentId;

        if (existingDocId != null) {
            log.info("群[{}]已有文本文档[{}]，更新内容（共{}条消息，新增{}条）", 
                    groupName, existingDocId, allTextMessages.size(), newMessages.size());
            success = difyService.updateDatasetDocument(imKb.getDifyDatasetId(), existingDocId, title, content);
            documentId = existingDocId;
        } else {
            log.info("群[{}]创建新文本文档（共{}条消息）", groupName, allTextMessages.size());
            documentId = difyService.createDatasetDocument(imKb.getDifyDatasetId(), title, content);
            success = (documentId != null);
        }

        if (success && documentId != null) {
            for (GroupMessage msg : newMessages) {
                msg.setSynced(true);
                msg.setKbDocumentId(documentId);
            }
            this.updateBatchById(newMessages);

            saveToKbDocument(imKb, title, content, documentId, groupId, groupName);
            log.info("群[{}]文本消息批量同步成功: 新增{}条", groupName, newMessages.size());
            return newMessages.size();
        } else {
            log.warn("群[{}]文本消息同步失败", groupName);
            return 0;
        }
    }

    private String formatMessageLine(GroupMessage msg) {
        String time = msg.getMessageTime() != null
                ? msg.getMessageTime().format(CONTENT_FORMATTER)
                : "";
        return String.format("[%s][user=%s]: %s", time, msg.getUserId(), msg.getRawMessage());
    }

    private ImGroup getGroupInfo(String groupId) {
        try {
            LambdaQueryWrapper<ImGroup> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ImGroup::getGroupId, groupId)
                    .eq(ImGroup::getStatus, true)
                    .last("LIMIT 1");
            return imGroupMapper.selectOne(wrapper);
        } catch (Exception e) {
            log.error("获取群组信息失败: groupId={}", groupId, e);
            return null;
        }
    }

    private KbCategory findOrCreateImSyncCategory() {
        LambdaQueryWrapper<KbCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbCategory::getName, IM_SYNC_CATEGORY_NAME)
                .eq(KbCategory::getStatus, true)
                .last("LIMIT 1");
        KbCategory category = kbCategoryMapper.selectOne(wrapper);

        if (category == null) {
            category = new KbCategory();
            category.setName(IM_SYNC_CATEGORY_NAME);
            category.setDescription("群聊消息同步分类");
            category.setParentId(0L);
            category.setSortOrder(0);
            category.setStatus(true);
            category.setCreateTime(LocalDateTime.now());
            category.setUpdateTime(LocalDateTime.now());
            kbCategoryMapper.insert(category);
            log.info("创建群聊消息分类: id={}, name={}", category.getId(), category.getName());
        }

        return category;
    }

    private KbKnowledgeBase findOrCreateImSyncKnowledgeBase() {
        KbCategory category = findOrCreateImSyncCategory();

        LambdaQueryWrapper<KbKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbKnowledgeBase::getSourceType, IM_SYNC_SOURCE_TYPE)
                .eq(KbKnowledgeBase::getName, IM_SYNC_KB_NAME)
                .eq(KbKnowledgeBase::getStatus, true)
                .last("LIMIT 1");
        KbKnowledgeBase kb = knowledgeBaseService.getOne(wrapper);

        if (kb == null) {
            kb = new KbKnowledgeBase();
            kb.setName(IM_SYNC_KB_NAME);
            kb.setDescription("所有群聊消息同步知识库，每个群一个文档实现数据隔离");
            kb.setCategoryId(category.getId());
            kb.setSourceType(IM_SYNC_SOURCE_TYPE);
            kb.setSyncGroupIds("[]");
            kb.setDocCount(0);
            kb.setStatus(true);
            kb.setCreateTime(LocalDateTime.now());
            kb.setUpdateTime(LocalDateTime.now());

            String difyDatasetId = difyService.createDataset(kb.getName(), kb.getDescription());
            kb.setDifyDatasetId(difyDatasetId);

            knowledgeBaseService.save(kb);
            log.info("创建群聊助手知识库: id={}, name={}, difyDatasetId={}", kb.getId(), kb.getName(), difyDatasetId);
        }

        if (kb.getDifyDatasetId() == null || kb.getDifyDatasetId().trim().isEmpty()) {
            log.warn("知识库缺少Dify Dataset ID，尝试从Dify查询: name={}", kb.getName());
            String difyDatasetId = difyService.createDataset(kb.getName(), kb.getDescription());
            if (difyDatasetId != null && !difyDatasetId.trim().isEmpty()) {
                kb.setDifyDatasetId(difyDatasetId);
                kb.setUpdateTime(LocalDateTime.now());
                knowledgeBaseService.updateById(kb);
                log.info("更新知识库Dify Dataset ID: id={}, difyDatasetId={}", kb.getId(), difyDatasetId);
            } else {
                log.error("无法获取Dify Dataset ID，同步将失败: name={}", kb.getName());
            }
        }

        return kb;
    }

    private void saveToKbDocument(KbKnowledgeBase kb, String title, String content, String difyDocId, String groupId, String groupName) {
        LambdaQueryWrapper<KbDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbDocument::getKnowledgeBaseId, kb.getId())
                .eq(KbDocument::getDifyDocumentId, difyDocId);
        KbDocument existingDoc = documentService.getOne(wrapper);

        if (existingDoc != null) {
            existingDoc.setContent(content);
            existingDoc.setTitle(title);
            existingDoc.setUpdateTime(LocalDateTime.now());
            documentService.updateById(existingDoc);
            log.info("更新kb_document: id={}, groupId={}, groupName={}", existingDoc.getId(), groupId, groupName);
        } else {
            KbDocument kbDoc = new KbDocument();
            kbDoc.setKnowledgeBaseId(kb.getId());
            kbDoc.setTitle(title);
            kbDoc.setContent(content);
            kbDoc.setDifyDocumentId(difyDocId);
            kbDoc.setSource(IM_SYNC_SOURCE_TYPE);
            kbDoc.setSyncStatus(1);
            kbDoc.setSyncTime(LocalDateTime.now());
            kbDoc.setDifyStatus("completed");
            kbDoc.setCreateTime(LocalDateTime.now());
            kbDoc.setUpdateTime(LocalDateTime.now());
            kbDoc.setStatus(true);

            documentService.save(kbDoc);

            kb.setDocCount(kb.getDocCount() != null ? kb.getDocCount() + 1 : 1);
            kb.setUpdateTime(LocalDateTime.now());
            knowledgeBaseService.updateById(kb);

            log.info("创建kb_document: id={}, groupId={}, groupName={}, kbId={}", kbDoc.getId(), groupId, groupName, kb.getId());
        }
    }

public void saveGroupMessage(String messageId, String groupId, String userId,
                                  String rawMessage, String messageType, long time) {
        saveGroupMessage("qq", messageId, groupId, userId, rawMessage, messageType, time, null, null);
    }

    public void saveGroupMessage(String userId, String rawMessage, String messageType, long time) {
        saveGroupMessage("web", null, null, userId, rawMessage, messageType, time, null, null);
    }

    @Override
    public void saveGroupMessage(String platform, String messageId, String groupId, String userId,
                                  String rawMessage, String messageType, long time,
                                  String fileUrl, String fileName) {
        try {
            if (messageId != null && !messageId.isEmpty()) {
                LambdaQueryWrapper<GroupMessage> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(GroupMessage::getPlatform, platform)
                       .eq(GroupMessage::getMessageId, messageId);
                GroupMessage existing = groupMessageMapper.selectOne(wrapper);
                if (existing != null) {
                    log.info("消息已存在，跳过保存: platform={}, messageId={}", platform, messageId);
                    return;
                }
            }

            GroupMessage gm = new GroupMessage();
            gm.setMessageId(messageId);
            gm.setGroupId(groupId);
            gm.setUserId(userId);
            gm.setPlatform(platform);
            gm.setMessageType(messageType);
            gm.setRawMessage(rawMessage);
            gm.setFileUrl(fileUrl);
            gm.setFileName(fileName);
            if (time > 0) {
                gm.setMessageTime(LocalDateTime.ofEpochSecond(time, 0, java.time.ZoneOffset.ofHours(8)));
            } else {
                gm.setMessageTime(LocalDateTime.now());
            }
            gm.setCreateTime(LocalDateTime.now());
            groupMessageMapper.insert(gm);
            log.info("群消息写库成功: platform={}, groupId={}, userId={}, type={}", platform, groupId, userId, messageType);
        } catch (Exception e) {
            log.error("保存群消息失败: platform={}, messageId={}", platform, messageId, e);
        }
    }
}