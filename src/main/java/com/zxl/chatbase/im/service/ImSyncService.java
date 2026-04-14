package com.zxl.chatbase.im.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zxl.chatbase.kb.entity.KbKnowledgeBase;

public interface ImSyncService {
    
    void syncGroupMessagesToKnowledgeBase();
    
    void syncByKnowledgeBase(Long knowledgeBaseId);
    
    Page<KbKnowledgeBase> listSyncableGroups();
}
