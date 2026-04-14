package com.zxl.chatbase.im.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_group")
public class ImGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String platform;

    private String groupId;

    private String groupName;

    private Integer memberCount;

    private String ownerId;

    private String robotId;

    private Boolean autoReply;

    private Long kbId;

    private Boolean status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}