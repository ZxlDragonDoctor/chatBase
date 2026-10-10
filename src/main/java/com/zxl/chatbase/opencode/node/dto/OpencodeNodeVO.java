package com.zxl.chatbase.opencode.node.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OpencodeNodeVO {
    private Long id;
    private String name;
    private String platform;
    private String agentVersion;
    private String status;
    private boolean online;
    private boolean capabilitiesPending;
    private LocalDateTime lastSeenAt;
    private List<String> capabilities;
    private List<String> approvedCapabilities;
}
