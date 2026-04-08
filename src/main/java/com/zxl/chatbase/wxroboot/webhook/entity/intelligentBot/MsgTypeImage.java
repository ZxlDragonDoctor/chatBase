package com.zxl.chatbase.wxroboot.webhook.entity.intelligentBot;

import lombok.Data;

import java.io.Serializable;

@Data
public class MsgTypeImage implements Serializable {
    private String url;
    private String base64;
    private String md5;
}
