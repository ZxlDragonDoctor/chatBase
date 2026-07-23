package com.zxl.chatbase.wxroboot.webhook.entity.intelligentBot;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class MsgTypeStream implements Serializable {
    private String id;
    private boolean finish;
    private String content;
    private List<MixedMsgItem> msgItem;
}
