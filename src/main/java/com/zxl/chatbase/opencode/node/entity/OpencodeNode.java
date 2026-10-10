package com.zxl.chatbase.opencode.node.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("opencode_node")
public class OpencodeNode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String nodeKey;
    private String name;
    private String platform;
    private String agentVersion;
    private String tokenHash;
    private String capabilities;
    private String approvedCapabilities;
    private String status;
    private String createdBy;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
