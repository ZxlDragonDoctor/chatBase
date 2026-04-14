package com.zxl.chatbase.kb.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zxl.chatbase.kb.entity.KbCategory;

import java.util.List;

public interface IKbCategoryService extends IService<KbCategory> {
    
    List<KbCategory> treeList();
    
    Page<KbCategory> pageList(String name, Integer pageNum, Integer pageSize);
    
    boolean createCategory(KbCategory category);
    
    boolean updateCategory(KbCategory category);
    
    boolean deleteCategory(Long id);
}
