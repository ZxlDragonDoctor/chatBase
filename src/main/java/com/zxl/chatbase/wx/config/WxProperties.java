package com.zxl.chatbase.wx.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wx.bot")
public class WxProperties {

    private boolean enable = false;

    private String botToken;

    private String baseUrl;

    private String botId;

    private String nickname = "微信机器人";

    private int pollIntervalSec = 35;

    private int reconnectDelaySec = 10;
}
