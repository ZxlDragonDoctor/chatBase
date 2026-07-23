package com.zxl.chatbase.feedback.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zxl.chatbase.feedback.service.IFeedbackFormService;
import com.zxl.chatbase.kb.entity.KbFeedback;
import com.zxl.chatbase.kb.mapper.KbFeedbackMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackFormServiceImpl implements IFeedbackFormService {

    private final KbFeedbackMapper feedbackMapper;

    @Override
    @Transactional
    public Long submitFeedback(String userId, Long conversationId, Integer rating, String feedbackType, String content) {
        KbFeedback feedback = new KbFeedback();
        feedback.setUserId(userId);
        if (conversationId != null && conversationId > 0) {
            feedback.setConversationId(conversationId);
        }
        feedback.setRating(rating);
        feedback.setFeedbackType(feedbackType);
        feedback.setFeedbackContent(content);
        feedback.setStatus(false);
        feedback.setCreateTime(LocalDateTime.now());
        
        feedbackMapper.insert(feedback);
        log.info("用户反馈已提交: userId={}, conversationId={}, type={}, rating={}", userId, conversationId, feedbackType, rating);
        
        return feedback.getId();
    }

    @Override
    public Page<KbFeedback> getFeedbackPage(Integer status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<KbFeedback> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null) {
            wrapper.eq(KbFeedback::getStatus, status == 1);
        }
        
        wrapper.orderByDesc(KbFeedback::getCreateTime);
        
        return feedbackMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Page<KbFeedback> getUserFeedbackPage(String userId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<KbFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbFeedback::getUserId, userId);
        wrapper.orderByDesc(KbFeedback::getCreateTime);
        
        return feedbackMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public KbFeedback getFeedbackById(Long id) {
        return feedbackMapper.selectById(id);
    }

    @Override
    @Transactional
    public boolean replyFeedback(Long id, Long adminId, String reply) {
        KbFeedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            return false;
        }
        
        feedback.setAdminReply(reply);
        feedback.setAdminId(adminId);
        feedback.setReplyTime(LocalDateTime.now());
        feedback.setStatus(true);
        feedback.setUpdateTime(LocalDateTime.now());
        
        return feedbackMapper.updateById(feedback) > 0;
    }

    @Override
    @Transactional
    public boolean updateStatus(Long id, Integer status) {
        KbFeedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            return false;
        }
        
        feedback.setStatus(status == 1);
        feedback.setUpdateTime(LocalDateTime.now());
        
        return feedbackMapper.updateById(feedback) > 0;
    }

    @Override
    public Map<String, Object> getFeedbackStats() {
        Map<String, Object> stats = new HashMap<>();
        
        Long total = feedbackMapper.selectCount(new LambdaQueryWrapper<>());
        Long pending = feedbackMapper.selectCount(new LambdaQueryWrapper<KbFeedback>().eq(KbFeedback::getStatus, false));
        Long processed = feedbackMapper.selectCount(new LambdaQueryWrapper<KbFeedback>().eq(KbFeedback::getStatus, true));
        
        List<KbFeedback> allFeedbacks = feedbackMapper.selectList(new LambdaQueryWrapper<>());
        
        double avgRating = 0;
        if (!allFeedbacks.isEmpty()) {
            avgRating = allFeedbacks.stream()
                    .mapToInt(f -> f.getRating() != null ? f.getRating() : 3)
                    .average()
                    .orElse(3.0);
        }
        
        Map<String, Long> typeCounts = new HashMap<>();
        for (KbFeedback f : allFeedbacks) {
            String type = f.getFeedbackType() != null ? f.getFeedbackType() : "other";
            typeCounts.merge(type, 1L, Long::sum);
        }
        
        stats.put("total", total);
        stats.put("pending", pending);
        stats.put("processed", processed);
        stats.put("avgRating", avgRating);
        stats.put("typeCounts", typeCounts);
        
        return stats;
    }
}