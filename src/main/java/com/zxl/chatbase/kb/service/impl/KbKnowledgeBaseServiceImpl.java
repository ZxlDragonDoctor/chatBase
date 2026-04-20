package com.zxl.chatbase.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxl.chatbase.dify.model.response.DifyDatasetResponse;
import com.zxl.chatbase.dify.model.response.DifyDocumentResponse;
import com.zxl.chatbase.dify.server.DifyService;
import com.zxl.chatbase.kb.entity.KbDocument;
import com.zxl.chatbase.kb.entity.KbKnowledgeBase;
import com.zxl.chatbase.kb.mapper.KbDocumentMapper;
import com.zxl.chatbase.kb.mapper.KbKnowledgeBaseMapper;
import com.zxl.chatbase.kb.service.IKbDocumentService;
import com.zxl.chatbase.kb.service.IKbKnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KbKnowledgeBaseServiceImpl extends ServiceImpl<KbKnowledgeBaseMapper, KbKnowledgeBase> implements IKbKnowledgeBaseService {

    private final IKbDocumentService documentService;
    private final DifyService difyService;

    @Override
    public Page<KbKnowledgeBase> pageList(Long categoryId, String name, Integer pageNum, Integer pageSize) {
        Page<KbKnowledgeBase> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<KbKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(KbKnowledgeBase::getCategoryId, categoryId);
        }
        if (name != null && !name.isBlank()) {
            wrapper.like(KbKnowledgeBase::getName, name);
        }
        wrapper.orderByDesc(KbKnowledgeBase::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    @Transactional
    public boolean createKnowledgeBase(KbKnowledgeBase knowledgeBase) {
        String difyDatasetId = difyService.createDataset(
                knowledgeBase.getName(),
                knowledgeBase.getDescription()
        );

        if (difyDatasetId == null) {
            log.warn("Dify知识库创建失败，仅保存本地记录");
        }

        knowledgeBase.setDifyDatasetId(difyDatasetId);
        knowledgeBase.setCreateTime(LocalDateTime.now());
        knowledgeBase.setUpdateTime(LocalDateTime.now());
        knowledgeBase.setDocCount(0);
        knowledgeBase.setChunkCount(0);
        if (knowledgeBase.getSourceType() == null) {
            knowledgeBase.setSourceType("manual");
        }
        if (knowledgeBase.getStatus() == null) {
            knowledgeBase.setStatus(true);
        }

        boolean saved = save(knowledgeBase);
        if (saved && difyDatasetId != null) {
            log.info("知识库创建成功，本地ID={}, Dify Dataset ID={}", knowledgeBase.getId(), difyDatasetId);
        }
        return saved;
    }

    @Override
    public boolean updateKnowledgeBase(KbKnowledgeBase knowledgeBase) {
        knowledgeBase.setUpdateTime(LocalDateTime.now());
        return updateById(knowledgeBase);
    }

    @Override
    @Transactional
    public boolean deleteKnowledgeBase(Long id) {
        return removeById(id);
    }

    @Override
    @Transactional
    public boolean syncDocumentsToDify(Long knowledgeBaseId) {
        KbKnowledgeBase kb = getById(knowledgeBaseId);
        if (kb == null) {
            return false;
        }

        LambdaQueryWrapper<KbDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbDocument::getKnowledgeBaseId, knowledgeBaseId);
        wrapper.eq(KbDocument::getSyncStatus, false);
        var documents = documentService.list(wrapper);

        int successCount = 0;
        for (KbDocument doc : documents) {
            if (documentService.syncToDify(doc.getId())) {
                successCount++;
            }
        }

        LambdaUpdateWrapper<KbKnowledgeBase> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(KbKnowledgeBase::getId, knowledgeBaseId)
                .set(KbKnowledgeBase::getDocCount, documents.size())
                .set(KbKnowledgeBase::getUpdateTime, LocalDateTime.now());
        update(updateWrapper);

        return successCount > 0;
    }

    @Override
    public List<DifyDatasetResponse> listDifyDatasets() {
        return difyService.listDatasets();
    }

    @Override
    @Transactional
    public int syncFromDify() {
        List<DifyDatasetResponse> difyDatasets = difyService.listDatasets();
        if (difyDatasets.isEmpty()) {
            log.info("Dify中没有知识库数据");
            return 0;
        }

        int syncCount = 0;
        for (DifyDatasetResponse dataset : difyDatasets) {
            LambdaQueryWrapper<KbKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(KbKnowledgeBase::getDifyDatasetId, dataset.getId())
                    .last("LIMIT 1");
            KbKnowledgeBase existing = getOne(wrapper);

            if (existing == null) {
                KbKnowledgeBase newKb = new KbKnowledgeBase();
                newKb.setName(dataset.getName());
                newKb.setDescription(dataset.getDescription());
                newKb.setDifyDatasetId(dataset.getId());
                newKb.setSourceType("dify_sync");
                newKb.setDocCount(dataset.getDocumentCount() != null ? dataset.getDocumentCount() : 0);
                newKb.setChunkCount(dataset.getWordCount() != null ? dataset.getWordCount() : 0);
                newKb.setStatus(true);
                newKb.setCreateTime(LocalDateTime.now());
                newKb.setUpdateTime(LocalDateTime.now());
                
                boolean saved = save(newKb);
                if (saved) {
                    syncCount++;
                    log.info("同步Dify知识库到本地成功: difyId={}, name={}", dataset.getId(), dataset.getName());
                    syncDocumentsFromDify(newKb.getId());
                }
            } else {
                existing.setDocCount(dataset.getDocumentCount() != null ? dataset.getDocumentCount() : existing.getDocCount());
                existing.setChunkCount(dataset.getWordCount() != null ? dataset.getWordCount() : existing.getChunkCount());
                existing.setUpdateTime(LocalDateTime.now());
                updateById(existing);
                syncCount++;
                log.info("更新本地知识库: difyId={}, name={}", dataset.getId(), dataset.getName());
                syncDocumentsFromDify(existing.getId());
            }
        }

        log.info("Dify知识库同步完成，共处理 {} 个", syncCount);
        return syncCount;
    }

    @Override
    @Transactional
    public int syncDocumentsFromDify(Long knowledgeBaseId) {
        KbKnowledgeBase kb = getById(knowledgeBaseId);
        if (kb == null || kb.getDifyDatasetId() == null) {
            log.warn("知识库不存在或未关联Dify: id={}", knowledgeBaseId);
            return 0;
        }

        List<DifyDocumentResponse> difyDocuments = difyService.listDatasetDocuments(kb.getDifyDatasetId());
        if (difyDocuments.isEmpty()) {
            log.info("Dify知识库中没有文档: datasetId={}", kb.getDifyDatasetId());
            return 0;
        }

        int syncCount = 0;
        for (DifyDocumentResponse difyDoc : difyDocuments) {
            LambdaQueryWrapper<KbDocument> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(KbDocument::getDifyDocumentId, difyDoc.getId())
                    .last("LIMIT 1");
            KbDocument existing = documentService.getOne(wrapper);

            if (existing == null) {
                KbDocument newDoc = new KbDocument();
                newDoc.setKnowledgeBaseId(knowledgeBaseId);
                newDoc.setTitle(difyDoc.getName());
                newDoc.setDifyDocumentId(difyDoc.getId());
                newDoc.setSource("dify_sync");
                newDoc.setSyncStatus(true);
                newDoc.setDifyStatus(difyDoc.getIndexingStatus() != null ? difyDoc.getIndexingStatus() : "completed");
                newDoc.setDifyChunkCount(difyDoc.getWordCount() != null ? difyDoc.getWordCount() : 0);
                newDoc.setStatus(difyDoc.getEnabled() != null ? difyDoc.getEnabled() : true);
                newDoc.setCreateTime(LocalDateTime.now());
                newDoc.setUpdateTime(LocalDateTime.now());
                
                boolean saved = documentService.save(newDoc);
                if (saved) {
                    syncCount++;
                    log.info("同步Dify文档到本地: docId={}, name={}", difyDoc.getId(), difyDoc.getName());
                }
            } else {
                existing.setDifyChunkCount(difyDoc.getWordCount() != null ? difyDoc.getWordCount() : existing.getDifyChunkCount());
                existing.setDifyStatus(difyDoc.getIndexingStatus() != null ? difyDoc.getIndexingStatus() : existing.getDifyStatus());
                existing.setStatus(difyDoc.getEnabled() != null ? difyDoc.getEnabled() : existing.getStatus());
                existing.setUpdateTime(LocalDateTime.now());
                documentService.updateById(existing);
                syncCount++;
            }
        }

        log.info("Dify文档同步完成: knowledgeBaseId={}, syncCount={}", knowledgeBaseId, syncCount);
        return syncCount;
    }
}