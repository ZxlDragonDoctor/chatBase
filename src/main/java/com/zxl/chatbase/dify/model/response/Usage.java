package com.zxl.chatbase.dify.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Usage {
    
    @JsonProperty("prompt_tokens")
    private Integer promptTokens;

    @JsonProperty("prompt_unit_price")
    private String promptUnitPrice;

    @JsonProperty("prompt_price_unit")
    private String promptPriceUnit;

    @JsonProperty("completion_tokens")
    private Integer completionTokens;
    
    @JsonProperty("completion_unit_price")
    private String completionUnitPrice;

    @JsonProperty("prompt_price")
    private String promptPrice;

    @JsonProperty("completion_price_unit")
    private String completionPriceUnit;
    
    @JsonProperty("completion_price")
    private String completionPrice;

    @JsonProperty("total_tokens")
    private Integer totalTokens;
    
    @JsonProperty("total_price")
    private String totalPrice;

    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("latency")
    private Double latency;
}