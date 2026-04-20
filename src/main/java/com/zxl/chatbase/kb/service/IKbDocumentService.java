package com.zxl.chatbase.kb.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zxl.chatbase.kb.entity.KbDocument;

public interface IKbDocumentService extends IService<KbDocument> {
    
    Page<KbDocument> pageList(Long knowledgeBaseId, String title, Integer pageNum, Integer pageSize);
    
    boolean createDocument(KbDocument document);
    
    boolean updateDocument(KbDocument document);
    
    boolean deleteDocument(Long id);
    
    boolean deleteDocumentWithDify(Long id);
    
    boolean syncToDify(Long documentId);
    
    boolean batchSyncToDify(Long knowledgeBaseId);
}
