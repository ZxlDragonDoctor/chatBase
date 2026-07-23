package com.zxl.chatbase.dify.model.response;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

@Data
public class DifyFileUploadResponse {

    private UUID id;

    private String name;

    private Integer size;

    private String extension;

    @JsonProperty("mime_type")
    private String mimeType;

    @JsonProperty("created_by")
    private UUID createdBy;

    @JsonProperty("created_at")
    private Long createdAt;
} 