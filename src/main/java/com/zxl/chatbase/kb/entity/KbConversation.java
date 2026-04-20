package com.zxl.chatbase.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kb_conversation")
public class KbConversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private String conversationId;

    private String userId;

    private String userNickname;

    private String channel;

    private String groupId;

    private Long knowledgeBaseId;

    private String query;

    private String answer;

    private String difyResponseId;

    private Integer tokens;

    private Integer latencyMs;

    private String sourceDocuments;

    private Boolean status;

    private String errorMessage;

    private LocalDateTime createTime;
}
