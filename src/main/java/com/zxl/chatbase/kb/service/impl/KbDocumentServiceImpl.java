package com.zxl.chatbase.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxl.chatbase.kb.entity.KbDocument;
import com.zxl.chatbase.kb.mapper.KbDocumentMapper;
import com.zxl.chatbase.kb.service.IKbDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class KbDocumentServiceImpl extends ServiceImpl<KbDocumentMapper, KbDocument> implements IKbDocumentService {

    @Override
    public Page<KbDocument> pageList(Long knowledgeBaseId, String title, Integer pageNum, Integer pageSize) {
        Page<KbDocument> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<KbDocument> wrapper = new LambdaQueryWrapper<>();
        if (knowledgeBaseId != null) {
            wrapper.eq(KbDocument::getKnowledgeBaseId, knowledgeBaseId);
        }
        if (title != null && !title.isBlank()) {
            wrapper.like(KbDocument::getTitle, title);
        }
        wrapper.orderByDesc(KbDocument::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    @Transactional
    public boolean createDocument(KbDocument document) {
        document.setCreateTime(LocalDateTime.now());
        document.setUpdateTime(LocalDateTime.now());
        document.setSyncStatus(false);
        return save(document);
    }

    @Override
    public boolean updateDocument(KbDocument document) {
        document.setUpdateTime(LocalDateTime.now());
        return updateById(document);
    }

    @Override
    public boolean deleteDocument(Long id) {
        return removeById(id);
    }

    @Override
    @Transactional
    public boolean syncToDify(Long documentId) {
        KbDocument document = getById(documentId);
        if (document == null) {
            return false;
        }
        
        try {
            LambdaUpdateWrapper<KbDocument> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(KbDocument::getId, documentId)
                    .set(KbDocument::getSyncStatus, true)
                    .set(KbDocument::getSyncTime, LocalDateTime.now())
                    .set(KbDocument::getDifyStatus, "completed");
            update(updateWrapper);
            return true;
        } catch (Exception e) {
            LambdaUpdateWrapper<KbDocument> errorWrapper = new LambdaUpdateWrapper<>();
            errorWrapper.eq(KbDocument::getId, documentId)
                    .set(KbDocument::getSyncStatus, false)
                    .set(KbDocument::getSyncError, e.getMessage())
                    .set(KbDocument::getDifyStatus, "failed");
            update(errorWrapper);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean batchSyncToDify(Long knowledgeBaseId) {
        LambdaQueryWrapper<KbDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbDocument::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KbDocument::getSyncStatus, false);
        var documents = list(wrapper);
        
        for (KbDocument doc : documents) {
            syncToDify(doc.getId());
        }
        return true;
    }
}
