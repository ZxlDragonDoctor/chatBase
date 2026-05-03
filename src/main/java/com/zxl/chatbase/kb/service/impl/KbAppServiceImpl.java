package com.zxl.chatbase.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxl.chatbase.dify.config.DifyConfig;
import com.zxl.chatbase.im.entity.ImGroup;
import com.zxl.chatbase.im.mapper.ImGroupMapper;
import com.zxl.chatbase.kb.entity.KbApp;
import com.zxl.chatbase.kb.mapper.KbAppMapper;
import com.zxl.chatbase.kb.service.IKbAppService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class KbAppServiceImpl implements IKbAppService {

    @Autowired
    private KbAppMapper appMapper;
    @Autowired
    private DifyConfig difyConfig;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ImGroupMapper imGroupMapper;
    @javax.annotation.Resource
    private CloseableHttpClient httpClient;

    @Override
    public List<KbApp> listAll(String userId) {
        LambdaQueryWrapper<KbApp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbApp::getStatus, true)
                .and(w -> w.eq(KbApp::getIsPublic, true)
                        .or()
                        .eq(KbApp::getCreateBy, userId))
                .orderByDesc(KbApp::getIsDefault)
                .orderByDesc(KbApp::getCreateTime);
        return appMapper.selectList(wrapper);
    }

    @Override
    public Page<KbApp> page(Integer pageNum, Integer pageSize, String userId, String name) {
        Page<KbApp> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<KbApp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbApp::getStatus, true)
                .and(w -> w.eq(KbApp::getIsPublic, true)
                        .or()
                        .eq(KbApp::getCreateBy, userId))
                .like(StringUtils.hasText(name), KbApp::getName, name)
                .orderByDesc(KbApp::getIsDefault)
                .orderByDesc(KbApp::getCreateTime);
        return appMapper.selectPage(page, wrapper);
    }

    @Override
    public KbApp getById(Long id) {
        return appMapper.selectById(id);
    }

    @Override
    public KbApp getDefaultApp() {
        LambdaQueryWrapper<KbApp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbApp::getStatus, true)
                .eq(KbApp::getIsDefault, true)
                .last("LIMIT 1");
        KbApp app = appMapper.selectOne(wrapper);
        if (app == null) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(KbApp::getStatus, true)
                    .last("LIMIT 1");
            app = appMapper.selectOne(wrapper);
        }
        return app;
    }

    @Override
    @Transactional
    public KbApp create(KbApp app, String userId) {
        if (verifyApiKey(app.getDifyApiKey()) == null) {
            throw new RuntimeException("Dify API Key验证失败");
        }
        app.setCreateBy(userId);
        app.setStatus(true);
        app.setIsDefault(false);
        if (app.getIsPublic() == null) {
            app.setIsPublic(true);
        }
        appMapper.insert(app);
        return app;
    }

    @Override
    @Transactional
    public KbApp update(KbApp app, String userId) {
        KbApp existing = appMapper.selectById(app.getId());
        if (existing == null) {
            throw new RuntimeException("应用不存在");
        }
        if (!canUserAccess(app.getId(), userId)) {
            throw new RuntimeException("无权限修改此应用");
        }
        if (StringUtils.hasText(app.getDifyApiKey()) && !app.getDifyApiKey().equals(existing.getDifyApiKey())) {
            KbApp verified = verifyApiKey(app.getDifyApiKey());
            if (verified == null) {
                throw new RuntimeException("新的Dify API Key验证失败");
            }
            app.setDifyAppName(verified.getDifyAppName());
            app.setDifyAppMode(verified.getDifyAppMode());
        }
        appMapper.updateById(app);
        return appMapper.selectById(app.getId());
    }

    @Override
    @Transactional
    public void delete(Long id, String userId) {
        KbApp app = appMapper.selectById(id);
        if (app == null) {
            throw new RuntimeException("应用不存在");
        }
        if (!canUserAccess(id, userId)) {
            throw new RuntimeException("无权限删除此应用");
        }
        if (app.getIsDefault()) {
            throw new RuntimeException("不能删除默认应用");
        }
        appMapper.deleteById(id);
    }

    @Override
    public KbApp verifyApiKey(String apiKey) {
        try {
            HttpGet httpGet = new HttpGet(difyConfig.getApiUrl() + "/info");
            httpGet.setHeader("Authorization", "Bearer " + apiKey);
            httpGet.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    JsonNode node = objectMapper.readTree(json);
                    KbApp app = new KbApp();
                    app.setDifyAppName(node.path("name").asText());
                    app.setDifyAppMode(node.path("mode").asText());
                    app.setDifyApiKey(apiKey);
                    return app;
                } else {
                    log.error("验证Dify API Key失败: status={}", statusCode);
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("验证Dify API Key异常", e);
            return null;
        }
    }

    @Override
    public KbApp getAppInfo(Long id) {
        KbApp app = appMapper.selectById(id);
        if (app == null) {
            throw new RuntimeException("应用不存在");
        }
        KbApp verified = verifyApiKey(app.getDifyApiKey());
        if (verified != null) {
            LambdaUpdateWrapper<KbApp> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(KbApp::getId, id)
                    .set(KbApp::getDifyAppName, verified.getDifyAppName())
                    .set(KbApp::getDifyAppMode, verified.getDifyAppMode());
            appMapper.update(null, wrapper);
            app.setDifyAppName(verified.getDifyAppName());
            app.setDifyAppMode(verified.getDifyAppMode());
        }
        return app;
    }

    @Override
    @Transactional
    public void setDefault(Long id, String userId) {
        KbApp app = appMapper.selectById(id);
        if (app == null) {
            throw new RuntimeException("应用不存在");
        }
        if (!canUserAccess(id, userId)) {
            throw new RuntimeException("无权限设置默认应用");
        }
        LambdaUpdateWrapper<KbApp> clearWrapper = new LambdaUpdateWrapper<>();
        clearWrapper.eq(KbApp::getIsDefault, true)
                .set(KbApp::getIsDefault, false);
        appMapper.update(null, clearWrapper);
        LambdaUpdateWrapper<KbApp> setWrapper = new LambdaUpdateWrapper<>();
        setWrapper.eq(KbApp::getId, id)
                .set(KbApp::getIsDefault, true);
        appMapper.update(null, setWrapper);
    }

    @Override
    public boolean canUserAccess(Long appId, String userId) {
        KbApp app = appMapper.selectById(appId);
        if (app == null) {
            return false;
        }
        return app.getIsPublic() || app.getCreateBy().equals(userId);
    }

    @Override
    public List<ImGroup> getBoundGroups(Long appId) {
        LambdaQueryWrapper<ImGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImGroup::getAppId, appId)
                .eq(ImGroup::getStatus, true)
                .orderByDesc(ImGroup::getUpdateTime);
        return imGroupMapper.selectList(wrapper);
    }
}