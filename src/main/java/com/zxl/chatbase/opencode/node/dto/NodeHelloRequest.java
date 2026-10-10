package com.zxl.chatbase.opencode.node.dto;

import lombok.Data;

import java.util.List;

@Data
public class NodeHelloRequest {
    private String protocolVersion;
    private String pairingCode;
    private String nodeKey;
    private String name;
    private String platform;
    private String agentVersion;
    private List<String> capabilities;
}
