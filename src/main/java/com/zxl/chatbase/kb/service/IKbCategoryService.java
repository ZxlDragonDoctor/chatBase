package com.zxl.chatbase.kb.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zxl.chatbase.kb.entity.KbCategory;

import java.util.List;

public interface IKbCategoryService extends IService<KbCategory> {

    List<KbCategory> treeList(String userId);

    Page<KbCategory> pageList(String name, Integer pageNum, Integer pageSize, String userId);

    boolean createCategory(KbCategory category, String userId);

    boolean updateCategory(KbCategory category, String userId);

    String deleteCategory(Long id, String userId);
}