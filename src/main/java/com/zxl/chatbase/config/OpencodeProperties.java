package com.zxl.chatbase.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 本地 opencode serve 集成配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "opencode")
public class OpencodeProperties {

    /**
     * opencode serve 服务地址（服务器经 frp 隧道访问本机）
     */
    private String baseUrl = "http://127.0.0.1:4096";

    /**
     * opencode serve 密码（OPENCODE_SERVER_PASSWORD），走 HTTP Basic Auth，用户名默认 opencode
     */
    private String password;

    /**
     * Basic Auth 用户名（默认 opencode）
     */
    private String username = "opencode";

    /**
     * 默认工作目录（创建会话时指定，即本机项目根目录）
     */
    private String defaultDirectory;

    /**
     * 默认 agent 名称
     */
    private String defaultAgent = "build";

    /**
     * 等待回复的最大时间（秒）
     */
    private int timeoutSeconds = 300;

    /**
     * 是否启用 opencode 集成（未配置时提示不可用）
     */
    private boolean enabled = false;
}
