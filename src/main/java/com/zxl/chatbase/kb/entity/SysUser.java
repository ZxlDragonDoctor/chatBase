package com.zxl.chatbase.kb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String avatar;

    private String email;

    private String phone;

    private Boolean status;

    private String role;

    private LocalDateTime lastLoginTime;

    private String lastLoginIp;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Boolean isDeleted;
}
