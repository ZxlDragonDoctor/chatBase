package com.zxl.chatbase.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxl.chatbase.kb.entity.KbFaq;
import com.zxl.chatbase.kb.mapper.KbFaqMapper;
import com.zxl.chatbase.kb.service.IKbFaqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KbFaqServiceImpl extends ServiceImpl<KbFaqMapper, KbFaq> implements IKbFaqService {

    @Override
    public Page<KbFaq> pageList(Long knowledgeBaseId, String question, Integer pageNum, Integer pageSize) {
        Page<KbFaq> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<KbFaq> wrapper = new LambdaQueryWrapper<>();
        if (knowledgeBaseId != null) {
            wrapper.eq(KbFaq::getKnowledgeBaseId, knowledgeBaseId);
        }
        if (question != null && !question.isBlank()) {
            wrapper.like(KbFaq::getQuestion, question);
        }
        wrapper.orderByDesc(KbFaq::getHitCount);
        return page(page, wrapper);
    }

    @Override
    @Transactional
    public boolean createFaq(KbFaq faq) {
        faq.setCreateTime(LocalDateTime.now());
        faq.setUpdateTime(LocalDateTime.now());
        faq.setHitCount(0);
        return save(faq);
    }

    @Override
    public boolean updateFaq(KbFaq faq) {
        faq.setUpdateTime(LocalDateTime.now());
        return updateById(faq);
    }

    @Override
    public boolean deleteFaq(Long id) {
        return removeById(id);
    }

    @Override
    public KbFaq findSimilar(String question) {
        LambdaQueryWrapper<KbFaq> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(KbFaq::getQuestion, question)
                .or()
                .like(KbFaq::getKeywords, question);
        wrapper.orderByDesc(KbFaq::getHitCount);
        wrapper.last("LIMIT 1");
        
        List<KbFaq> list = list(wrapper);
        if (!list.isEmpty()) {
            KbFaq faq = list.get(0);
            lambdaUpdate().eq(KbFaq::getId, faq.getId())
                    .setSql("hit_count = hit_count + 1")
                    .update();
            return faq;
        }
        return null;
    }
}
