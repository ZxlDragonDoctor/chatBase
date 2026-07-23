package com.zxl.chatbase.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kb_document")
public class KbDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long knowledgeBaseId;

    private String title;

    private String content;

    private String fileUrl;

    private String fileName;

    private Long fileSize;

    private String fileType;

    private String difyDocumentId;

    private String difyStatus;

    private Integer difyChunkCount;

    private String source;

    private String sourceMessageId;

    private Integer syncStatus;

    private LocalDateTime syncTime;

    private String syncError;

    private String tagList;

    private Boolean status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;
}
