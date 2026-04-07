package com.zxl.chatbase.wroboot.webhook.entity.intelligentBot;

import lombok.Data;

import java.io.Serializable;

@Data
public class MixedMsgItem implements Serializable {
    private String msgtype;
    private MsgTypeText text;
    private MsgTypeImage image;

    // todo 判断msgtype的便捷方法定义
    boolean isItemTypeText() {
        return IntelligentBotMsg.MSG_TYPE_TEXT.equals(this.msgtype);
    }

    boolean isItemTypeImage() {
        return IntelligentBotMsg.MSG_TYPE_IMAGE.equals(this.msgtype);
    }
}
