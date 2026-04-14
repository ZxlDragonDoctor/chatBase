package com.zxl.chatbase.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kb_knowledge_base")
public class KbKnowledgeBase {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private Long categoryId;

    private String difyDatasetId;

    private String difyApiKey;

    private String sourceType;

    private String syncPlatform;

    private String syncGroupIds;

    private Boolean autoSync;

    private Integer syncInterval;

    private Integer docCount;

    private Integer chunkCount;

    private Boolean status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;
}
