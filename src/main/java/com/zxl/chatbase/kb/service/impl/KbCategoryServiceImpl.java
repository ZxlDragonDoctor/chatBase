package com.zxl.chatbase.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxl.chatbase.kb.entity.KbCategory;
import com.zxl.chatbase.kb.mapper.KbCategoryMapper;
import com.zxl.chatbase.kb.service.IKbCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KbCategoryServiceImpl extends ServiceImpl<KbCategoryMapper, KbCategory> implements IKbCategoryService {

    @Override
    public List<KbCategory> treeList() {
        List<KbCategory> allCategories = list();
        Map<Long, List<KbCategory>> grouped = allCategories.stream()
                .collect(Collectors.groupingBy(KbCategory::getParentId));
        
        return buildTree(0L, grouped);
    }

    private List<KbCategory> buildTree(Long parentId, Map<Long, List<KbCategory>> grouped) {
        List<KbCategory> children = grouped.getOrDefault(parentId, new ArrayList<>());
        return children.stream()
                .peek(c -> {
                })
                .collect(Collectors.toList());
    }

    @Override
    public Page<KbCategory> pageList(String name, Integer pageNum, Integer pageSize) {
        Page<KbCategory> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<KbCategory> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) {
            wrapper.like(KbCategory::getName, name);
        }
        wrapper.orderByAsc(KbCategory::getSortOrder);
        return page(page, wrapper);
    }

    @Override
    @Transactional
    public boolean createCategory(KbCategory category) {
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        return save(category);
    }

    @Override
    public boolean updateCategory(KbCategory category) {
        category.setUpdateTime(LocalDateTime.now());
        return updateById(category);
    }

    @Override
    public boolean deleteCategory(Long id) {
        LambdaQueryWrapper<KbCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbCategory::getParentId, id);
        long childCount = count(wrapper);
        if (childCount > 0) {
            return false;
        }
        return removeById(id);
    }
}
