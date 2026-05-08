package com.zxl.chatbase.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zxl.chatbase.kb.entity.KbKnowledgeBase;
import com.zxl.chatbase.kb.entity.KbUserCategoryMapping;
import com.zxl.chatbase.kb.mapper.KbUserCategoryMappingMapper;
import com.zxl.chatbase.kb.service.IKbKnowledgeBaseService;
import com.zxl.chatbase.kb.service.IKbUserCategoryMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KbUserCategoryMappingServiceImpl implements IKbUserCategoryMappingService {

    private final KbUserCategoryMappingMapper mappingMapper;
    private final IKbKnowledgeBaseService knowledgeBaseService;

    @Override
    public void link(Long kbId, Long categoryId, String userId) {
        KbKnowledgeBase kb = knowledgeBaseService.getById(kbId);
        if (kb == null) {
            throw new RuntimeException("知识库不存在");
        }
        if (!Boolean.TRUE.equals(kb.getIsPublic()) && !userId.equals(kb.getCreateBy())) {
            throw new RuntimeException("只能关联公开知识库到自己的分类");
        }

        LambdaQueryWrapper<KbUserCategoryMapping> existsCheck = new LambdaQueryWrapper<>();
        existsCheck.eq(KbUserCategoryMapping::getCategoryId, categoryId)
                .eq(KbUserCategoryMapping::getKbId, kbId)
                .eq(KbUserCategoryMapping::getUserId, userId);
        if (mappingMapper.selectCount(existsCheck) > 0) {
            throw new RuntimeException("已关联过此知识库");
        }

        KbUserCategoryMapping mapping = new KbUserCategoryMapping();
        mapping.setCategoryId(categoryId);
        mapping.setKbId(kbId);
        mapping.setUserId(userId);
        mapping.setCreateTime(LocalDateTime.now());
        mappingMapper.insert(mapping);
        log.info("用户关联知识库到分类: userId={}, kbId={}, categoryId={}", userId, kbId, categoryId);
    }

    @Override
    public void unlink(Long id, String userId) {
        KbUserCategoryMapping mapping = mappingMapper.selectById(id);
        if (mapping == null) {
            throw new RuntimeException("关联不存在");
        }
        if (!mapping.getUserId().equals(userId)) {
            throw new RuntimeException("无权取消此关联");
        }
        mappingMapper.deleteById(id);
    }

    @Override
    public List<KbUserCategoryMapping> listByCategory(Long categoryId) {
        LambdaQueryWrapper<KbUserCategoryMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbUserCategoryMapping::getCategoryId, categoryId);
        return mappingMapper.selectList(wrapper);
    }
}