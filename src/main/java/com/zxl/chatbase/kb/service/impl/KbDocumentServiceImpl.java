package com.zxl.chatbase.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxl.chatbase.dify.server.DifyService;
import com.zxl.chatbase.kb.entity.KbDocument;
import com.zxl.chatbase.kb.entity.KbKnowledgeBase;
import com.zxl.chatbase.kb.mapper.KbDocumentMapper;
import com.zxl.chatbase.kb.service.IKbDocumentService;
import com.zxl.chatbase.kb.service.IKbKnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class KbDocumentServiceImpl extends ServiceImpl<KbDocumentMapper, KbDocument> implements IKbDocumentService {

    private final IKbKnowledgeBaseService knowledgeBaseService;
    private final DifyService difyService;

    public KbDocumentServiceImpl(@Lazy IKbKnowledgeBaseService knowledgeBaseService, DifyService difyService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.difyService = difyService;
    }

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
        KbKnowledgeBase kb = knowledgeBaseService.getById(document.getKnowledgeBaseId());

        String difyDocId = null;
        if (kb != null && kb.getDifyDatasetId() != null && document.getContent() != null) {
            difyDocId = difyService.createDatasetDocument(document.getTitle(), document.getContent());
            if (difyDocId != null) {
                log.info("文档同步到Dify成功, documentId={}", difyDocId);
            } else {
                log.warn("文档同步到Dify失败");
            }
        }

        document.setDifyDocumentId(difyDocId);
        document.setSyncStatus(difyDocId != null);
        if (difyDocId != null) {
            document.setDifyStatus("completed");
            document.setSyncTime(LocalDateTime.now());
        }
        document.setCreateTime(LocalDateTime.now());
        document.setUpdateTime(LocalDateTime.now());
        if (document.getSource() == null) {
            document.setSource("manual");
        }
        if (document.getStatus() == null) {
            document.setStatus(true);
        }

        boolean saved = save(document);

        if (saved && kb != null && difyDocId != null) {
            kb.setDocCount(kb.getDocCount() != null ? kb.getDocCount() + 1 : 1);
            kb.setUpdateTime(LocalDateTime.now());
            knowledgeBaseService.updateById(kb);
        }

        return saved;
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
    public boolean deleteDocumentWithDify(Long id) {
        KbDocument doc = getById(id);
        if (doc == null) {
            log.warn("文档不存在: {}", id);
            return false;
        }

        KbKnowledgeBase kb = knowledgeBaseService.getById(doc.getKnowledgeBaseId());

        if (doc.getDifyDocumentId() != null && kb != null && kb.getDifyDatasetId() != null) {
            boolean difyDeleted = difyService.deleteDatasetDocument(kb.getDifyDatasetId(), doc.getDifyDocumentId());
            if (difyDeleted) {
                log.info("Dify文档删除成功: datasetId={}, documentId={}", kb.getDifyDatasetId(), doc.getDifyDocumentId());
            } else {
                log.warn("Dify文档删除失败: datasetId={}, documentId={}", kb.getDifyDatasetId(), doc.getDifyDocumentId());
            }
        }

        boolean removed = removeById(id);
        if (removed && kb != null && doc.getDifyDocumentId() != null) {
            kb.setDocCount(Math.max(0, kb.getDocCount() != null ? kb.getDocCount() - 1 : 0));
            kb.setUpdateTime(LocalDateTime.now());
            knowledgeBaseService.updateById(kb);
        }

        return removed;
    }

    @Override
    @Transactional
    public boolean syncToDify(Long documentId) {
        KbDocument doc = getById(documentId);
        if (doc == null) {
            log.warn("文档不存在: {}", documentId);
            return false;
        }

        KbKnowledgeBase kb = knowledgeBaseService.getById(doc.getKnowledgeBaseId());
        if (kb == null || kb.getDifyDatasetId() == null) {
            log.warn("知识库未关联Dify Dataset: knowledgeBaseId={}", doc.getKnowledgeBaseId());

            LambdaUpdateWrapper<KbDocument> errorWrapper = new LambdaUpdateWrapper<>();
            errorWrapper.eq(KbDocument::getId, documentId)
                    .set(KbDocument::getSyncStatus, false)
                    .set(KbDocument::getSyncError, "知识库未关联Dify Dataset")
                    .set(KbDocument::getDifyStatus, "failed")
                    .set(KbDocument::getUpdateTime, LocalDateTime.now());
            update(errorWrapper);
            return false;
        }

        try {
            String difyDocId = doc.getDifyDocumentId();
            boolean success;

            if (difyDocId != null && !difyDocId.isEmpty()) {
                success = difyService.updateDatasetDocument(difyDocId, doc.getTitle(), doc.getContent());
                log.info("更新Dify文档: documentId={}, success={}", difyDocId, success);
            } else {
                difyDocId = difyService.createDatasetDocument(doc.getTitle(), doc.getContent());
                success = difyDocId != null;
                if (success) {
                    doc.setDifyDocumentId(difyDocId);
                    log.info("创建Dify文档: documentId={}", difyDocId);
                }
            }

            LambdaUpdateWrapper<KbDocument> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(KbDocument::getId, documentId)
                    .set(KbDocument::getSyncStatus, success)
                    .set(KbDocument::getSyncTime, LocalDateTime.now())
                    .set(KbDocument::getDifyStatus, success ? "completed" : "failed")
                    .set(KbDocument::getDifyDocumentId, difyDocId)
                    .set(KbDocument::getUpdateTime, LocalDateTime.now());

            if (!success) {
                updateWrapper.set(KbDocument::getSyncError, "Dify同步失败");
            }

            update(updateWrapper);

            if (success && doc.getSyncStatus() == null || !doc.getSyncStatus()) {
                kb.setDocCount(kb.getDocCount() != null ? kb.getDocCount() + 1 : 1);
                kb.setUpdateTime(LocalDateTime.now());
                knowledgeBaseService.updateById(kb);
            }

            return success;

        } catch (Exception e) {
            log.error("同步文档到Dify异常: documentId={}", documentId, e);

            LambdaUpdateWrapper<KbDocument> errorWrapper = new LambdaUpdateWrapper<>();
            errorWrapper.eq(KbDocument::getId, documentId)
                    .set(KbDocument::getSyncStatus, false)
                    .set(KbDocument::getSyncError, e.getMessage())
                    .set(KbDocument::getDifyStatus, "failed")
                    .set(KbDocument::getUpdateTime, LocalDateTime.now());
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