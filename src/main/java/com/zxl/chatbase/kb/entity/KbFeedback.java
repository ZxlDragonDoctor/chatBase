package com.zxl.chatbase.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kb_feedback")
public class KbFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    private String userId;

    private Integer rating;

    private String feedbackType;

    private String feedbackContent;

    private String adminReply;

    private Long adminId;

    private LocalDateTime replyTime;

    private Boolean status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
