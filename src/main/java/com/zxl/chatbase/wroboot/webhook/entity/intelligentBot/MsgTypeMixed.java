package com.zxl.chatbase.wroboot.webhook.entity.intelligentBot;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class MsgTypeMixed implements Serializable {
    private List<MixedMsgItem> msgItem;
}
