package com.zxl.chatbase.kb.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zxl.chatbase.kb.entity.KbKnowledgeBase;
import com.zxl.chatbase.kb.entity.KbDocument;

public interface IKbKnowledgeBaseService extends IService<KbKnowledgeBase> {
    
    Page<KbKnowledgeBase> pageList(Long categoryId, String name, Integer pageNum, Integer pageSize);
    
    boolean createKnowledgeBase(KbKnowledgeBase knowledgeBase);
    
    boolean updateKnowledgeBase(KbKnowledgeBase knowledgeBase);
    
    boolean deleteKnowledgeBase(Long id);
    
    boolean syncDocumentsToDify(Long knowledgeBaseId);
}
