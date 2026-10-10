package com.zxl.chatbase.opencode.bridge;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * opencode 反向桥接配置
 *
 * 桥接模式下，开发者本机运行 {@code bridge/opencode-bridge.mjs}，
 * 主动外连服务器 WebSocket（无需 frp / 内网穿透），服务器通过该连接
 * 反向调用本机 opencode serve。
 */
@Data
@Component
@ConfigurationProperties(prefix = "opencode.bridge")
public class OpencodeBridgeProperties {

    /** 是否启用桥接接收端（服务器侧） */
    private boolean enabled = true;

    /**
     * 桥接鉴权 token。本机 agent 与服务器必须一致。
     * 为空时拒绝所有桥接连接（安全默认）。
     */
    private String token;

    /**
     * transport 模式：
     * - auto：优先走已连接的桥接，无桥接时回退直连 HTTP（baseUrl）
     * - bridge：仅走桥接，未连接时直接报错
     * - direct：仅走直连 HTTP（等价于旧 frp / 同机方案）
     */
    private String transport = "auto";

    /** 单次桥接请求超时（秒），应不小于 opencode.timeout-seconds */
    private int requestTimeoutSeconds = 320;

    /** 心跳间隔（秒），用于检测死连接 */
    private int heartbeatSeconds = 30;
}
