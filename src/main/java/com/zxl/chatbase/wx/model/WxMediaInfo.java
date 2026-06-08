package com.zxl.chatbase.wx.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WxMediaInfo {

    @JsonProperty("cdn_url")
    private String cdnUrl;

    @JsonProperty("aes_key")
    private String aesKey;

    @JsonProperty("file_size")
    private Long fileSize;

    @JsonProperty("file_name")
    private String fileName;

    private String md5;
}
