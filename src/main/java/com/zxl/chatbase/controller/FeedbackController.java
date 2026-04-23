package com.zxl.chatbase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zxl.chatbase.feedback.service.IFeedbackFormService;
import com.zxl.chatbase.kb.entity.KbFeedback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final IFeedbackFormService feedbackFormService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    @PostMapping("/submit")
    public Map<String, Object> submitFeedback(
            @RequestParam String userId,
            @RequestParam(required = false) Long conversationId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String feedbackType,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String contact) {
        
        if (contact != null && !contact.isBlank()) {
            if (!isValidContact(contact)) {
                return Map.of("success", false, "message", "联系方式格式不正确，请输入有效的邮箱或手机号");
            }
        }
        
        String finalContent = content;
        if (contact != null && !contact.isBlank()) {
            finalContent = (content != null ? content : "") + "\n\n联系方式: " + contact;
        }
        
        Long id = feedbackFormService.submitFeedback(userId, conversationId, rating, feedbackType, finalContent);
        
        return Map.of("success", true, "id", id, "message", "反馈已提交，感谢您的宝贵意见");
    }

    private boolean isValidContact(String contact) {
        String trimmed = contact.trim();
        if (EMAIL_PATTERN.matcher(trimmed).matches()) {
            return true;
        }
        if (PHONE_PATTERN.matcher(trimmed).matches()) {
            return true;
        }
        log.warn("联系方式格式校验失败: {}", contact);
        return false;
    }

    @GetMapping("/page")
    public Page<KbFeedback> getFeedbackPage(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return feedbackFormService.getFeedbackPage(status, pageNum, pageSize);
    }

    @GetMapping("/user/{userId}")
    public Page<KbFeedback> getUserFeedbackPage(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return feedbackFormService.getUserFeedbackPage(userId, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    public KbFeedback getFeedbackById(@PathVariable Long id) {
        return feedbackFormService.getFeedbackById(id);
    }

    @PostMapping("/{id}/reply")
    public Map<String, Object> replyFeedback(
            @PathVariable Long id,
            @RequestParam Long adminId,
            @RequestParam String reply) {
        
        boolean success = feedbackFormService.replyFeedback(id, adminId, reply);
        
        return Map.of(
                "success", success,
                "message", success ? "回复成功" : "回复失败"
        );
    }

    @PutMapping("/{id}/status")
    public Map<String, Object> updateStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        
        boolean success = feedbackFormService.updateStatus(id, status);
        
        return Map.of(
                "success", success,
                "message", success ? "状态更新成功" : "状态更新失败"
        );
    }

    @GetMapping("/stats")
    public Map<String, Object> getFeedbackStats() {
        return feedbackFormService.getFeedbackStats();
    }
}