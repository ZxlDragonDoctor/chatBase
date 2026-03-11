package com.zxl.chatbase.qq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * QQ 机器人相关配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "qq.bot")
public class QqBotProperties {

    /**
     * 是否启用 QQ 机器人
     */
    private boolean enable = false;

    /**
     * WebSocket 端口（当前示例使用应用本身端口，仅作占位）
     */
    private int wsPort = 8081;

    /**
     * NapCat / OneBot 访问的 token（如果配置了认证）
     */
    private String accessToken;

    /**
     * 本地文件保存路径
     */
    private String fileSavePath;

    /**
     * 机器人自身 QQ 号
     */
    private long selfId;

    /**
     * NapCat HTTP 请求的 Base URL
     */
    private String httpBaseUrl;
}

