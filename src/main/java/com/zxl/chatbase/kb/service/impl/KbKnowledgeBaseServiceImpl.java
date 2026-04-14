package com.zxl.chatbase.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
        knowledgeBase.setCreateTime(LocalDateTime.now());
        knowledgeBase.setUpdateTime(LocalDateTime.now());
        knowledgeBase.setDocCount(0);
        knowledgeBase.setChunkCount(0);
        return save(knowledgeBase);
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
        wrapper.eq(KbDocument::getSyncStatus, 0);
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
}
