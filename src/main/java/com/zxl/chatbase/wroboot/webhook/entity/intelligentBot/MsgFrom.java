package com.zxl.chatbase.wroboot.webhook.entity.intelligentBot;

import lombok.Data;

import java.io.Serializable;

@Data
public class MsgFrom implements Serializable {
    private String userid;

    public boolean isValidMsgFrom() {
        if(userid == null || userid.isEmpty()) {
            return false;
        }
        return true;
    }
}
