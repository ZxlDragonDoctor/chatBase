package com.zxl.chatbase.dify.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class DifyDatasetResponse {

    private String id;

    private String name;

    private String description;

    private String permission;

    @JsonProperty("data_source_type")
    private String dataSourceType;

    @JsonProperty("indexing_technique")
    private String indexingTechnique;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("document_count")
    private Integer documentCount;

    @JsonProperty("word_count")
    private Integer wordCount;

    @Data
    public static class DifyDatasetListResponse {
        private List<DifyDatasetResponse> data;
        private Integer limit;
        private Integer offset;
        private Integer total;
    }
}