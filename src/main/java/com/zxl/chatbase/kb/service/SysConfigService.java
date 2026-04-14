package com.zxl.chatbase.kb.service;

import java.util.List;
import java.util.Map;

public interface SysConfigService {

    String getConfigValue(String key, String defaultValue);

    Integer getConfigValueInt(String key, Integer defaultValue);

    Boolean getConfigValueBoolean(String key, Boolean defaultValue);

    Map<String, String> getConfigGroup(String group);

    void setConfigValue(String key, String value, String type);

    List<Map<String, Object>> listAll();
}