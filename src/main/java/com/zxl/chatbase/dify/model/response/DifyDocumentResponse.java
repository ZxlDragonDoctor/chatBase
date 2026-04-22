package com.zxl.chatbase.dify.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class DifyDocumentResponse {

    private String id;

    private String name;

    @JsonProperty("data_source_type")
    private String dataSourceType;

    @JsonProperty("word_count")
    private Integer wordCount;

    @JsonProperty("hit_count")
    private Integer hitCount;

    @JsonProperty("indexing_status")
    private String indexingStatus;

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("disabled_at")
    private Long disabledAt;

    @JsonProperty("archived")
    private Boolean archived;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;

    @Data
    public static class DifyDocumentListResponse {
        private List<DifyDocumentResponse> data;
        private Integer limit;
        private Integer offset;
        private Integer total;
    }
}