package com.zxl.chatbase.kb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kb_keyword")
public class KbKeyword {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String keyword;

    private String source;

    private Integer count;

    private LocalDateTime lastSeenTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}