package com.zxl.chatbase.wroboot.webhook.entity.intelligentBot;

import lombok.Data;

import java.io.Serializable;

@Data
public class MsgTypeText implements Serializable {
    private String content;
}
