package com.zxl.chatbase.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("kb_faq")
public class KbFaq {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long knowledgeBaseId;

    private Long categoryId;

    private String question;

    private String answer;

    private String keywords;

    private Integer hitCount;

    private BigDecimal satisfaction;

    private String similarQuestions;

    private Boolean status;

    private Integer priority;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;
}
