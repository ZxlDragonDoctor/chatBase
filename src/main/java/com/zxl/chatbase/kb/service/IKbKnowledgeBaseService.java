package com.zxl.chatbase.kb.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zxl.chatbase.dify.model.response.DifyDatasetResponse;
import com.zxl.chatbase.kb.entity.KbKnowledgeBase;
import com.zxl.chatbase.kb.entity.KbDocument;

import java.util.List;

public interface IKbKnowledgeBaseService extends IService<KbKnowledgeBase> {
    
    Page<KbKnowledgeBase> pageList(Long categoryId, String name, Integer pageNum, Integer pageSize);
    
    boolean createKnowledgeBase(KbKnowledgeBase knowledgeBase);
    
    boolean updateKnowledgeBase(KbKnowledgeBase knowledgeBase);
    
    boolean deleteKnowledgeBase(Long id);
    
    boolean syncDocumentsToDify(Long knowledgeBaseId);

    /**
     * 同步Dify知识库到本地数据库
     * 获取Dify中已创建的知识库，并与本地数据进行同步
     * 
     * @return 同步成功的知识库数量
     */
    int syncFromDify();

    /**
     * 同步单个知识库的文档到本地
     * 
     * @param knowledgeBaseId 本地知识库ID
     * @return 同步的文档数量
     */
    int syncDocumentsFromDify(Long knowledgeBaseId);

    /**
     * 获取Dify知识库列表
     */
    List<DifyDatasetResponse> listDifyDatasets();
}
