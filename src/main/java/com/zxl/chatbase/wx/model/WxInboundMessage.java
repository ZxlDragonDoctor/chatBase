package com.zxl.chatbase.wx.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class WxInboundMessage {

    @JsonProperty("message_id")
    private String msgId;

    @JsonProperty("from_user_id")
    private String fromUserId;

    @JsonProperty("group_id")
    private String fromGroupId;

    @JsonProperty("context_token")
    private String contextToken;

    @JsonProperty("message_type")
    private Integer messageType;

    private Long timestamp;

    @JsonProperty("item_list")
    private List<Item> itemList;

    @Data
    public static class Item {
        private Integer type;
        @JsonProperty("text_item")
        private TextItem textItem;
        @JsonProperty("image_item")
        private MediaItem imageItem;
        @JsonProperty("file_item")
        private MediaItem fileItem;
    }

    @Data
    public static class TextItem {
        private String text;
    }

    @Data
    public static class MediaItem {
        @JsonProperty("cdn_url")
        private String cdnUrl;
        @JsonProperty("aes_key")
        private String aesKey;
        @JsonProperty("file_size")
        private Long fileSize;
        private String md5;
        @JsonProperty("file_name")
        private String fileName;
        @JsonProperty("media_key")
        private String mediaKey;
    }

    public boolean isGroupChat() {
        return fromGroupId != null && !fromGroupId.isEmpty();
    }

    public boolean isPrivateChat() {
        return !isGroupChat();
    }

    public boolean isText() {
        return itemList != null && itemList.stream().anyMatch(i -> i.type != null && i.type == 1);
    }

    public boolean isImage() {
        return itemList != null && itemList.stream().anyMatch(i -> i.type != null && i.type == 3);
    }

    public boolean isFile() {
        return itemList != null && itemList.stream().anyMatch(i -> i.type != null && i.type == 7);
    }

    public String getTextContent() {
        if (itemList == null) return "";
        return itemList.stream()
                .filter(i -> i.type != null && i.type == 1 && i.textItem != null)
                .map(i -> i.textItem.getText())
                .findFirst().orElse("");
    }

    public MediaItem getFirstMedia() {
        if (itemList == null) return null;
        return itemList.stream()
                .filter(i -> (i.type != null && (i.type == 3 || i.type == 7))
                        && (i.imageItem != null || i.fileItem != null))
                .findFirst()
                .map(i -> i.type == 3 ? i.imageItem : i.fileItem)
                .orElse(null);
    }

    public String getContentType() {
        if (isImage()) return "image";
        if (isFile()) return "file";
        return "text";
    }
}
