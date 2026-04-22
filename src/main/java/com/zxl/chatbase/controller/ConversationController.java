package com.zxl.chatbase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zxl.chatbase.feedback.service.IFeedbackStatsService;
import com.zxl.chatbase.kb.dto.FeedbackRequest;
import com.zxl.chatbase.kb.entity.KbConversation;
import com.zxl.chatbase.kb.entity.KbFaq;
import com.zxl.chatbase.kb.service.IKbConversationService;
import com.zxl.chatbase.kb.service.IKbFaqService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kb/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private final IKbConversationService conversationService;
    private final IKbFaqService faqService;
    private final IFeedbackStatsService feedbackStatsService;

    @GetMapping("/page")
    public Page<KbConversation> page(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "all") String channel,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return conversationService.pageList(userId, channel, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    public KbConversation getById(@PathVariable Long id) {
        return conversationService.getById(id);
    }

    @PostMapping("/feedback")
    public Map<String, Object> addFeedback(@RequestBody FeedbackRequest request) {
        if (request.getRating() == null) {
            return Map.of("success", false, "message", "无效的评分");
        }
        
        boolean success;
        if (request.getRating() == 1) {
            success = feedbackStatsService.recordThumbsUp(request.getSessionId(), request.getMessageIndex());
        } else if (request.getRating() == 0) {
            success = feedbackStatsService.recordThumbsDown(request.getSessionId(), request.getMessageIndex());
        } else {
            return Map.of("success", false, "message", "无效的评分值");
        }
        
        if (success) {
            return Map.of("success", true, "message", "反馈已记录", "feedback", request.getRating());
        } else {
            return Map.of("success", false, "message", "已反馈过，不可重复提交");
        }
    }

    @GetMapping("/feedback/status")
    public Map<Integer, Integer> getFeedbackStatus(@RequestParam String sessionId) {
        return feedbackStatsService.getSessionFeedbackStatus(sessionId);
    }

    @GetMapping("/faq/page")
    public Page<KbFaq> faqPage(
            @RequestParam(required = false) Long knowledgeBaseId,
            @RequestParam(required = false) String question,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return faqService.pageList(knowledgeBaseId, question, pageNum, pageSize);
    }

    @PostMapping("/faq")
    public boolean createFaq(@RequestBody KbFaq faq) {
        return faqService.createFaq(faq);
    }

    @PutMapping("/faq")
    public boolean updateFaq(@RequestBody KbFaq faq) {
        return faqService.updateFaq(faq);
    }

    @DeleteMapping("/faq/{id}")
    public boolean deleteFaq(@PathVariable Long id) {
        return faqService.deleteFaq(id);
    }

    @GetMapping("/faq/similar")
    public KbFaq findSimilar(@RequestParam String question) {
        return faqService.findSimilar(question);
    }

    @PostMapping("/faq/extract")
    public Map<String, Object> extractFaq(
            @RequestParam(defaultValue = "1") Long knowledgeBaseId,
            @RequestParam(defaultValue = "3") Integer minCount,
            @RequestParam(defaultValue = "30") Integer days) {
        int count = faqService.extractFaqFromConversations(knowledgeBaseId, minCount, days);
        return Map.of("success", true, "count", count, "message", "已提取 " + count + " 条FAQ");
    }

    @GetMapping("/faq/hot-questions")
    public List<Map<String, Object>> getHotQuestions(
            @RequestParam(defaultValue = "30") Integer days,
            @RequestParam(defaultValue = "20") Integer limit) {
        return faqService.getHotQuestions(days, limit);
    }

    @GetMapping("/faq/stats")
    public Map<String, Object> getFaqStats() {
        return faqService.getFaqStats();
    }
}
