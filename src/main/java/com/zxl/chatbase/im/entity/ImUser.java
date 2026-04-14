package com.zxl.chatbase.im.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_user")
public class ImUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String platform;

    private String userId;

    private String nickname;

    private String avatar;

    private String role;

    private String groupId;

    private LocalDateTime lastMessageTime;

    private Integer messageCount;

    private Boolean status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}