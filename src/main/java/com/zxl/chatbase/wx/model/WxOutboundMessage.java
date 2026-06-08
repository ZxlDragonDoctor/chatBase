package com.zxl.chatbase.wx.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class WxOutboundMessage {

    private Msg msg;

    @JsonProperty("base_info")
    private BaseInfo baseInfo;

    @Data
    @Builder
    public static class Msg {
        @JsonProperty("from_user_id")
        private String fromUserId;

        @JsonProperty("to_user_id")
        private String toUserId;

        @JsonProperty("client_id")
        private String clientId;

        @JsonProperty("message_type")
        private int messageType;

        @JsonProperty("message_state")
        private int messageState;

        @JsonProperty("context_token")
        private String contextToken;

        @JsonProperty("item_list")
        private List<Item> itemList;
    }

    @Data
    @Builder
    public static class Item {
        private int type;

        @JsonProperty("text_item")
        private TextItem textItem;
    }

    @Data
    @Builder
    public static class TextItem {
        private String text;
    }

    @Data
    @Builder
    public static class BaseInfo {
        @JsonProperty("channel_version")
        private String channelVersion;
    }

    public static WxOutboundMessage createTextMessage(String toUserId, String contextToken, String text) {
        String clientId = "bot-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return WxOutboundMessage.builder()
                .msg(Msg.builder()
                        .fromUserId("")
                        .toUserId(toUserId)
                        .clientId(clientId)
                        .messageType(2)
                        .messageState(2)
                        .contextToken(contextToken)
                        .itemList(Collections.singletonList(
                                Item.builder()
                                        .type(1)
                                        .textItem(TextItem.builder().text(text).build())
                                        .build()
                        ))
                        .build())
                .baseInfo(BaseInfo.builder().channelVersion("1.0.3").build())
                .build();
    }
}
