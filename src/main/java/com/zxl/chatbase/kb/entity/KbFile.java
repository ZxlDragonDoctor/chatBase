package com.zxl.chatbase.kb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kb_file")
public class KbFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileName;

    private String filePath;

    private Long fileSize;

    private String fileType;

    private String fileExt;

    private String bucket;

    private String source;

    private String sourceId;

    private String uploadUserId;

    private String uploadGroupId;

    private String difyFileId;

    private Boolean status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}