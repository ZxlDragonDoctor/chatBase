package com.zxl.chatbase.opencode.node.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PairingCodeVO {
    private String code;
    private LocalDateTime expiresAt;
}
