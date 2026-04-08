package com.zxl.chatbase.wxroboot.webhook.entity.intelligentBot;

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
