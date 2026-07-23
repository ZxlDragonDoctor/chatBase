package com.zxl.chatbase.kb.dto;

import lombok.Data;

@Data
public class FeedbackRequest {
    private String sessionId;
    private Integer messageIndex;
    private Integer rating;
    private String feedbackType;
    private String feedbackContent;
}