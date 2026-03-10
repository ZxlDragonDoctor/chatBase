package com.zxl.chatbase.dify.model.response;

import lombok.Data;

/**
 * 模型用量信息
 */
@Data
public class Usage {
    
    /**
     * 提示词tokens数量
     */
    private Integer promptTokens;

    /**
     * 提示词单价
     */
    private String promptUnitPrice;

    /**
     * 提示词价格单位
     */
    private String promptPriceUnit;

    /**
     * 完成tokens数量
     */
    private Integer completionTokens;
    
    /**
     * 完成词单价
     */
    private String completionUnitPrice;

    /**
     * 提示词总价
     */
    private String promptPrice;

    /**
     * 完成词价格单位
     */
    private String completionPriceUnit;
    
    /**
     * 完成词总价
     */
    private String completionPrice;

    /**
     * 总tokens数量
     */
    private Integer totalTokens;
    
    /**
     * 总价
     */
    private String totalPrice;
    
    /**
     * 货币类型
     */
    private String currency;
    
    /**
     * 延迟时间
     */
    private Double latency;
}