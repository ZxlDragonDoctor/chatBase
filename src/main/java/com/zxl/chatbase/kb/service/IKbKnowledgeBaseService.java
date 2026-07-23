package com.zxl.chatbase.kb.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zxl.chatbase.dify.model.response.DifyDatasetResponse;
import com.zxl.chatbase.kb.entity.KbKnowledgeBase;

import java.util.List;

public interface IKbKnowledgeBaseService extends IService<KbKnowledgeBase> {

    Page<KbKnowledgeBase> pageList(Long categoryId, String name, Integer pageNum, Integer pageSize, String userId);

    Page<KbKnowledgeBase> pageAllForAdmin(Long categoryId, String name, Integer pageNum, Integer pageSize);

    boolean createKnowledgeBase(KbKnowledgeBase knowledgeBase, String userId);

    boolean updateKnowledgeBase(KbKnowledgeBase knowledgeBase, String userId);

    boolean deleteKnowledgeBase(Long id, String userId);

    boolean syncDocumentsToDify(Long knowledgeBaseId);

    int syncFromDify();

    int syncDocumentsFromDify(Long knowledgeBaseId);

    List<DifyDatasetResponse> listDifyDatasets();

    boolean canViewKb(Long kbId, String userId);

    boolean canModifyKb(Long kbId, String userId);
}