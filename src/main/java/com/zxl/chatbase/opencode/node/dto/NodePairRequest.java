package com.zxl.chatbase.opencode.node.dto;

import lombok.Data;

@Data
public class NodePairRequest {
    private Long nodeId;
    private String nodeToken;
    private String approval;
}
