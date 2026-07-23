package com.zxl.chatbase.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kb_app")
public class KbApp {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String icon;

    private String difyApiKey;

    private String difyAppName;

    private String difyAppMode;

    private Long categoryId;

    private Boolean isDefault;

    private Boolean isPublic;

    private String createBy;

    private Boolean status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}