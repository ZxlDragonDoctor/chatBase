package com.zxl.chatbase.kb.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zxl.chatbase.im.entity.ImGroup;
import com.zxl.chatbase.kb.entity.KbApp;

import java.util.List;

public interface IKbAppService {

    List<KbApp> listAll(String userId);

    Page<KbApp> page(Integer pageNum, Integer pageSize, String userId, String name);

    KbApp getById(Long id);

    KbApp getDefaultApp();

    KbApp create(KbApp app, String userId);

    KbApp update(KbApp app, String userId);

    void delete(Long id, String userId);

    KbApp verifyApiKey(String apiKey);

    KbApp getAppInfo(Long id);

    void setDefault(Long id, String userId);

    boolean canUserAccess(Long appId, String userId);

    List<ImGroup> getBoundGroups(Long appId);
}