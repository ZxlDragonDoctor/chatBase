package com.zxl.chatbase.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kb_category")
public class KbCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String name;

    private String icon;

    private Integer sortOrder;

    private String description;

    private Boolean status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;
}
