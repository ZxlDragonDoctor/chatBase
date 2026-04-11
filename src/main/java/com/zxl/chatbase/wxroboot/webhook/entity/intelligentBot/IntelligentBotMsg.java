package com.zxl.chatbase.wxroboot.webhook.entity.intelligentBot;

import lombok.Data;

import java.io.Serializable;

@Data
public class IntelligentBotMsg implements Serializable {
    private String msgid;

    private String aibotid;

    private String chatid;

    private String chattype;

    private MsgFrom from;

    private String msgtype;

    private  String responseUrl; //企微群聊回复链接，有限时间为一个小时

    private MsgTypeText text;
    private MsgTypeImage image;
    private MsgTypeMixed mixed;
    private MsgTypeStream stream;


    public static final String CHAT_TYPE_GROUP = "group";
    public static final String CHAT_TYPE_SINGLE = "single";

    public static final String MSG_TYPE_TEXT = "text";
    public static final String MSG_TYPE_IMAGE = "image";
    public static final String MSG_TYPE_MIXED = "mixed";
    public static final String MSG_TYPE_STREAM = "stream";

    public boolean isFromSingle() {
        return CHAT_TYPE_SINGLE.equals(this.chattype);
    }

    public boolean isFromGroup() {
        return CHAT_TYPE_GROUP.equals(this.chattype);
    }

    // TODO 其它常亮定义，其它便捷方法定义
    public boolean isMsgText() {
        return MSG_TYPE_TEXT.equals(this.msgtype);
    }

    public boolean isMsgImage() {
        return MSG_TYPE_IMAGE.equals(this.msgtype);
    }

    public boolean isMsgMixed() {
        return MSG_TYPE_MIXED.equals(this.msgtype);
    }

    public boolean isMsgStream() {
        return MSG_TYPE_STREAM.equals(this.msgtype);
    }

    public boolean isValidMessage() {
        if(this.msgid == null || this.msgid.isEmpty()) {
            return false;
        }

        if(this.aibotid == null || this.aibotid.isEmpty()) {
            return false;
        }

        if(this.chatid == null || this.chatid.isEmpty()) {
            return false;
        }

        if(this.chattype == null || this.chattype.isEmpty()) {
            return false;
        }

        if(!this.from.isValidMsgFrom()) {
            return false;
        }

        return true;
    }

}
