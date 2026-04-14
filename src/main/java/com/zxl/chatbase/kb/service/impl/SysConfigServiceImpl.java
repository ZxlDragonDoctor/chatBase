package com.zxl.chatbase.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zxl.chatbase.kb.entity.SysConfig;
import com.zxl.chatbase.kb.mapper.SysConfigMapper;
import com.zxl.chatbase.kb.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper sysConfigMapper;

    private final Map<String, String> configCache = new ConcurrentHashMap<>();

    @Value("${difyApp.timeOut:90}")
    private int defaultTimeout;

    @Value("${chat.maxTurns:20}")
    private int defaultMaxTurns;

    @Override
    public String getConfigValue(String key, String defaultValue) {
        String value = getFromCache(key);
        if (value != null) {
            return value;
        }
        return defaultValue;
    }

    @Override
    public Integer getConfigValueInt(String key, Integer defaultValue) {
        String value = getConfigValue(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("配置项 {} 值不是有效数字: {}", key, value);
            return defaultValue;
        }
    }

    @Override
    public Boolean getConfigValueBoolean(String key, Boolean defaultValue) {
        String value = getConfigValue(key, null);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    @Override
    public Map<String, String> getConfigGroup(String group) {
        List<SysConfig> configs = sysConfigMapper.selectList(
                new LambdaQueryWrapper<SysConfig>()
                        .eq(SysConfig::getConfigGroup, group)
                        .eq(SysConfig::getStatus, true)
                        .orderByAsc(SysConfig::getSortOrder)
        );
        return configs.stream()
                .collect(Collectors.toMap(SysConfig::getConfigKey, SysConfig::getConfigValue));
    }

    @Override
    public void setConfigValue(String key, String value, String type) {
        SysConfig config = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key)
        );
        if (config == null) {
            config = new SysConfig();
            config.setConfigKey(key);
            config.setConfigType(type != null ? type : "string");
            config.setConfigGroup("manual");
            config.setStatus(true);
            config.setSortOrder(0);
        }
        config.setConfigValue(value);
        if (config.getId() == null) {
            sysConfigMapper.insert(config);
        } else {
            sysConfigMapper.updateById(config);
        }
        configCache.put(key, value);
    }

    @Override
    public List<Map<String, Object>> listAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<SysConfig> configs = sysConfigMapper.selectList(
                new LambdaQueryWrapper<SysConfig>()
                        .eq(SysConfig::getStatus, true)
                        .orderByAsc(SysConfig::getSortOrder)
        );
        for (SysConfig config : configs) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", config.getId());
            map.put("key", config.getConfigKey());
            map.put("value", config.getConfigValue());
            map.put("type", config.getConfigType());
            map.put("desc", config.getConfigDesc());
            result.add(map);
        }
        return result;
    }

    private String getFromCache(String key) {
        if (configCache.containsKey(key)) {
            return configCache.get(key);
        }
        try {
            SysConfig config = sysConfigMapper.selectOne(
                    new LambdaQueryWrapper<SysConfig>()
                            .eq(SysConfig::getConfigKey, key)
                            .eq(SysConfig::getStatus, true)
            );
            if (config != null) {
                configCache.put(key, config.getConfigValue());
                return config.getConfigValue();
            }
        } catch (Exception e) {
            log.warn("从数据库读取配置失败: {}", key);
        }
        return null;
    }

    public int getDefaultTimeout() {
        return defaultTimeout;
    }

    public int getDefaultMaxTurns() {
        return defaultMaxTurns;
    }
}