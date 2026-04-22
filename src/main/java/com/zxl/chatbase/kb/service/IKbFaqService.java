package com.zxl.chatbase.kb.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zxl.chatbase.kb.entity.KbFaq;

import java.util.List;
import java.util.Map;

public interface IKbFaqService extends IService<KbFaq> {
    
    Page<KbFaq> pageList(Long knowledgeBaseId, String question, Integer pageNum, Integer pageSize);
    
    boolean createFaq(KbFaq faq);
    
    boolean updateFaq(KbFaq faq);
    
    boolean deleteFaq(Long id);
    
    KbFaq findSimilar(String question);

    int extractFaqFromConversations(Long knowledgeBaseId, int minCount, int days);

    List<Map<String, Object>> getHotQuestions(int days, int limit);

    Map<String, Object> getFaqStats();
}
