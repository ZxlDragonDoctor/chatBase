package com.zxl.chatbase.dify.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RetrieverResource {

    private Integer position;
    
    @JsonProperty("dataset_id")
    private String datasetId;
    
    @JsonProperty("dataset_name")
    private String datasetName;
    
    @JsonProperty("document_id")
    private String documentId;
    
    @JsonProperty("document_name")
    private String documentName;
    
    @JsonProperty("segment_id")
    private String segmentId;
    
    private Double score;
    
    private String content;
}