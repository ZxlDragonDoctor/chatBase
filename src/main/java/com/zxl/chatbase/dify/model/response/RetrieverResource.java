package com.zxl.chatbase.dify.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 引用和归属分段信息
 */
@Data
public class RetrieverResource {
    
    /**
     * 位置
     */
    private Integer position;
    
    /**
     * 数据集ID
     */
    private String datasetId;
    
    /**
     * 数据集名称
     */
    private String datasetName;
    
    /**
     * 文档ID
     */
    private String documentId;
    
    /**
     * 文档名称
     */
    String documentName;
    
    /**
     * 分段ID
     */
    private String segmentId;
    
    /**
     * 得分
     */
    private Double score;
    
    /**
     * 内容
     */
    private String content;
}