package com.zxl.chatbase.kb.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zxl.chatbase.kb.entity.KbConversation;
import com.zxl.chatbase.kb.entity.KbFaq;
import com.zxl.chatbase.kb.service.IKbConversationService;
import com.zxl.chatbase.kb.service.IKbFaqService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kb/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private final IKbConversationService conversationService;
    private final IKbFaqService faqService;

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
    public boolean addFeedback(
            @RequestParam Long conversationId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String feedbackType,
            @RequestParam(required = false) String content) {
        return conversationService.addFeedback(conversationId, rating, feedbackType, content);
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
}
