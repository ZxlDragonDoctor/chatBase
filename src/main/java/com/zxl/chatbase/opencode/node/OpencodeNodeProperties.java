package com.zxl.chatbase.opencode.node;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "opencode.node")
public class OpencodeNodeProperties {
    private boolean enabled = true;
    private int requestTimeoutSeconds = 320;
    private int heartbeatSeconds = 30;
    private int offlineAfterSeconds = 90;
    private int pairingTtlSeconds = 600;
}
