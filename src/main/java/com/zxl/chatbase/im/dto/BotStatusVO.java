package com.zxl.chatbase.im.dto;

import lombok.Data;

@Data
public class BotStatusVO {

    @Data
    public static class QqBotVO {
        private boolean enabled;
        private long selfId;
        private int wsPort;
        private boolean httpConfigured;
        /** 仅用于本机排查，可为空 */
        private String httpBaseUrlPreview;
    }

    @Data
    public static class WeComBotVO {
        /** 相对应用的回调路径（完整 URL = 公网域名 + context-path + 此路径） */
        private String callbackPath;
        private String note;
    }

    private QqBotVO qq;
    private WeComBotVO wecom;
}
