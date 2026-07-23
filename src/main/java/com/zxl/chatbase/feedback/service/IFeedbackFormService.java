package com.zxl.chatbase.feedback.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zxl.chatbase.kb.entity.KbFeedback;

import java.util.Map;

public interface IFeedbackFormService {

    Long submitFeedback(String userId, Long conversationId, Integer rating, String feedbackType, String content);

    Page<KbFeedback> getFeedbackPage(Integer status, Integer pageNum, Integer pageSize);

    Page<KbFeedback> getUserFeedbackPage(String userId, Integer pageNum, Integer pageSize);

    KbFeedback getFeedbackById(Long id);

    boolean replyFeedback(Long id, Long adminId, String reply);

    boolean updateStatus(Long id, Integer status);

    Map<String, Object> getFeedbackStats();
}