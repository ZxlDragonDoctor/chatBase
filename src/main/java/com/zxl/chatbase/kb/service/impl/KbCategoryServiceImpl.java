package com.zxl.chatbase.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxl.chatbase.kb.entity.KbCategory;
import com.zxl.chatbase.kb.entity.KbKnowledgeBase;
import com.zxl.chatbase.kb.entity.SysUser;
import com.zxl.chatbase.kb.mapper.KbCategoryMapper;
import com.zxl.chatbase.kb.mapper.KbKnowledgeBaseMapper;
import com.zxl.chatbase.kb.mapper.SysUserMapper;
import com.zxl.chatbase.kb.service.IKbCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KbCategoryServiceImpl extends ServiceImpl<KbCategoryMapper, KbCategory> implements IKbCategoryService {

    private final KbKnowledgeBaseMapper kbMapper;
    private final SysUserMapper sysUserMapper;

    private boolean isAdmin(String userId) {
        if (!StringUtils.hasText(userId)) return false;
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, userId);
        SysUser user = sysUserMapper.selectOne(wrapper);
        return user != null && "admin".equals(user.getRole());
    }

    @Override
    public List<KbCategory> treeList(String userId) {
        LambdaQueryWrapper<KbCategory> categoryWrapper = new LambdaQueryWrapper<>();
        categoryWrapper.and(w -> w.eq(KbCategory::getCreateBy, userId)
                .or().isNull(KbCategory::getCreateBy));
        List<KbCategory> allCategories = list(categoryWrapper);

        LambdaQueryWrapper<KbKnowledgeBase> kbWrapper = new LambdaQueryWrapper<>();
        kbWrapper.eq(KbKnowledgeBase::getStatus, true);
        List<KbKnowledgeBase> allKbs = kbMapper.selectList(kbWrapper);

        Map<Long, Integer> kbCountMap = allKbs.stream()
                .filter(kb -> kb.getCategoryId() != null)
                .collect(Collectors.groupingBy(KbKnowledgeBase::getCategoryId, Collectors.summingInt(e -> 1)));

        Map<Long, List<KbCategory>> grouped = allCategories.stream()
                .collect(Collectors.groupingBy(KbCategory::getParentId));

        return buildTree(0L, grouped, kbCountMap);
    }

    private List<KbCategory> buildTree(Long parentId, Map<Long, List<KbCategory>> grouped, Map<Long, Integer> kbCountMap) {
        List<KbCategory> children = grouped.getOrDefault(parentId, new ArrayList<>());
        return children.stream()
                .peek(c -> {
                    c.setKbCount(kbCountMap.getOrDefault(c.getId(), 0));
                    c.setChildren(buildTree(c.getId(), grouped, kbCountMap));
                })
                .collect(Collectors.toList());
    }

    @Override
    public Page<KbCategory> pageList(String name, Integer pageNum, Integer pageSize, String userId) {
        Page<KbCategory> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<KbCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(KbCategory::getCreateBy, userId)
                .or().isNull(KbCategory::getCreateBy));
        if (StringUtils.hasText(name)) {
            wrapper.like(KbCategory::getName, name);
        }
        wrapper.orderByAsc(KbCategory::getSortOrder);
        return page(page, wrapper);
    }

    @Override
    @Transactional
    public boolean createCategory(KbCategory category, String userId) {
        category.setCreateBy(userId);
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        return save(category);
    }

    @Override
    public boolean updateCategory(KbCategory category, String userId) {
        KbCategory existing = getById(category.getId());
        if (existing == null) return false;
        if (!existing.getCreateBy().equals(userId)) {
            throw new RuntimeException("无权修改此分类");
        }
        category.setUpdateTime(LocalDateTime.now());
        return updateById(category);
    }

    @Override
    public String deleteCategory(Long id, String userId) {
        KbCategory category = getById(id);
        if (category == null) return "分类不存在";
        if (!category.getCreateBy().equals(userId)) {
            return "无权删除此分类";
        }
        if ("admin".equals(category.getCreateBy())) {
            return "系统分类无法删除";
        }

        LambdaQueryWrapper<KbCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbCategory::getParentId, id);
        long childCount = count(wrapper);
        if (childCount > 0) {
            return "该分类下有子分类，无法删除";
        }

        LambdaQueryWrapper<KbKnowledgeBase> kbWrapper = new LambdaQueryWrapper<>();
        kbWrapper.eq(KbKnowledgeBase::getCategoryId, id);
        long kbCount = kbMapper.selectCount(kbWrapper);
        if (kbCount > 0) {
            return "该分类下有知识库，无法删除";
        }

        boolean success = removeById(id);
        return success ? null : "删除失败，请稍后重试";
    }
}