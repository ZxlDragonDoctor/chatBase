package com.zxl.chatbase.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 聊天相关配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "chat")
public class ChatProperties {

    /**
     * 单个会话最大轮数，超过后自动开启新会话
     */
    private long maxTurnsPerSession = 20;

    /**
     * 会话相关 key 的过期天数
     */
    private long sessionTtlDays = 7;

    /**
     * 群聊 @ 机器人限流（按 groupId+userId 维度）
     */
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class RateLimit {
        /**
         * 限流窗口（秒）
         */
        private long windowSeconds = 5;

        /**
         * 窗口内允许次数
         */
        private long maxRequests = 1;
    }
}

