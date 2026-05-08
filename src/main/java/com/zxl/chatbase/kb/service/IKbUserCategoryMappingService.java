package com.zxl.chatbase.kb.service;

import com.zxl.chatbase.kb.entity.KbUserCategoryMapping;

import java.util.List;

public interface IKbUserCategoryMappingService {

    void link(Long kbId, Long categoryId, String userId);

    void unlink(Long id, String userId);

    List<KbUserCategoryMapping> listByCategory(Long categoryId);
}