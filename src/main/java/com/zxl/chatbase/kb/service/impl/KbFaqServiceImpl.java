package com.zxl.chatbase.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxl.chatbase.kb.entity.KbConversation;
import com.zxl.chatbase.kb.entity.KbFaq;
import com.zxl.chatbase.kb.mapper.KbConversationMapper;
import com.zxl.chatbase.kb.mapper.KbFaqMapper;
import com.zxl.chatbase.kb.service.IKbFaqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KbFaqServiceImpl extends ServiceImpl<KbFaqMapper, KbFaq> implements IKbFaqService {

    private final KbConversationMapper conversationMapper;

    @Override
    public Page<KbFaq> pageList(Long knowledgeBaseId, String question, Integer pageNum, Integer pageSize) {
        Page<KbFaq> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<KbFaq> wrapper = new LambdaQueryWrapper<>();
        if (knowledgeBaseId != null) {
            wrapper.eq(KbFaq::getKnowledgeBaseId, knowledgeBaseId);
        }
        if (question != null && !question.isBlank()) {
            wrapper.like(KbFaq::getQuestion, question);
        }
        wrapper.orderByDesc(KbFaq::getHitCount);
        return page(page, wrapper);
    }

    @Override
    @Transactional
    public boolean createFaq(KbFaq faq) {
        faq.setCreateTime(LocalDateTime.now());
        faq.setUpdateTime(LocalDateTime.now());
        faq.setHitCount(0);
        faq.setStatus(true);
        faq.setPriority(0);
        return save(faq);
    }

    @Override
    public boolean updateFaq(KbFaq faq) {
        faq.setUpdateTime(LocalDateTime.now());
        return updateById(faq);
    }

    @Override
    public boolean deleteFaq(Long id) {
        return removeById(id);
    }

    @Override
    public KbFaq findSimilar(String question) {
        LambdaQueryWrapper<KbFaq> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbFaq::getStatus, true);
        wrapper.and(w -> w
                .like(KbFaq::getQuestion, question)
                .or()
                .like(KbFaq::getKeywords, question)
        );
        wrapper.orderByDesc(KbFaq::getPriority);
        wrapper.orderByDesc(KbFaq::getHitCount);
        wrapper.last("LIMIT 1");
        
        List<KbFaq> list = list(wrapper);
        if (!list.isEmpty()) {
            KbFaq faq = list.get(0);
            lambdaUpdate().eq(KbFaq::getId, faq.getId())
                    .setSql("hit_count = hit_count + 1")
                    .update();
            return faq;
        }
        return null;
    }

    @Override
    @Transactional
    public int extractFaqFromConversations(Long knowledgeBaseId, int minCount, int days) {
        LambdaQueryWrapper<KbConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(KbConversation::getQuery, KbConversation::getAnswer, KbConversation::getStatus);
        wrapper.eq(KbConversation::getStatus, true);
        wrapper.isNotNull(KbConversation::getQuery).ne(KbConversation::getQuery, "");
        wrapper.isNotNull(KbConversation::getAnswer).ne(KbConversation::getAnswer, "");
        
        if (days > 0) {
            LocalDateTime startTime = LocalDate.now().minusDays(days).atStartOfDay();
            wrapper.ge(KbConversation::getCreateTime, startTime);
        }
        
        wrapper.last("LIMIT 5000");
        
        List<KbConversation> conversations = conversationMapper.selectList(wrapper);
        
        Map<String, List<KbConversation>> questionGroups = conversations.stream()
                .collect(Collectors.groupingBy(c -> normalizeQuestion(c.getQuery())));
        
        int count = 0;
        for (Map.Entry<String, List<KbConversation>> entry : questionGroups.entrySet()) {
            String normalizedQuestion = entry.getKey();
            List<KbConversation> group = entry.getValue();
            
            if (group.size() < minCount) {
                continue;
            }
            
            KbConversation best = group.stream()
                    .max(Comparator.comparingInt(c -> c.getAnswer().length()))
                    .orElse(null);
            
            if (best == null) continue;
            
            LambdaQueryWrapper<KbFaq> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(KbFaq::getQuestion, normalizedQuestion);
            if (knowledgeBaseId != null) {
                existWrapper.eq(KbFaq::getKnowledgeBaseId, knowledgeBaseId);
            }
            
            if (count(existWrapper) > 0) {
                continue;
            }
            
            KbFaq faq = new KbFaq();
            faq.setKnowledgeBaseId(knowledgeBaseId != null ? knowledgeBaseId : 1L);
            faq.setQuestion(normalizedQuestion);
            faq.setAnswer(best.getAnswer());
            faq.setHitCount(group.size());
            faq.setStatus(true);
            faq.setPriority(group.size());
            faq.setCreateTime(LocalDateTime.now());
            faq.setUpdateTime(LocalDateTime.now());
            
            String keywords = extractKeywords(normalizedQuestion);
            faq.setKeywords(keywords);
            
            save(faq);
            count++;
        }
        
        log.info("从历史对话提取FAQ完成: 提取 {} 条, 最小频次 {}", count, minCount);
        return count;
    }

    @Override
    public List<Map<String, Object>> getHotQuestions(int days, int limit) {
        LambdaQueryWrapper<KbConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(KbConversation::getQuery);
        wrapper.eq(KbConversation::getStatus, true);
        wrapper.isNotNull(KbConversation::getQuery).ne(KbConversation::getQuery, "");
        
        if (days > 0) {
            LocalDateTime startTime = LocalDate.now().minusDays(days).atStartOfDay();
            wrapper.ge(KbConversation::getCreateTime, startTime);
        }
        
        wrapper.last("LIMIT 1000");
        
        List<KbConversation> conversations = conversationMapper.selectList(wrapper);
        
        Map<String, Long> questionCounts = conversations.stream()
                .collect(Collectors.groupingBy(
                        c -> normalizeQuestion(c.getQuery()),
                        Collectors.counting()
                ));
        
        return questionCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> Map.<String, Object>of(
                        "question", e.getKey(),
                        "count", e.getValue()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getFaqStats() {
        Map<String, Object> stats = new HashMap<>();
        
        Long total = count(new LambdaQueryWrapper<>());
        Long active = count(new LambdaQueryWrapper<KbFaq>().eq(KbFaq::getStatus, true));
        Long inactive = count(new LambdaQueryWrapper<KbFaq>().eq(KbFaq::getStatus, false));
        
        List<KbFaq> topFaqs = list(new LambdaQueryWrapper<KbFaq>()
                .orderByDesc(KbFaq::getHitCount)
                .last("LIMIT 10"));
        
        stats.put("total", total);
        stats.put("active", active);
        stats.put("inactive", inactive);
        stats.put("topFaqs", topFaqs);
        
        return stats;
    }

    private String normalizeQuestion(String question) {
        if (question == null) return "";
        String normalized = question.trim().toLowerCase();
        normalized = normalized.replaceAll("[\\s　]+", " ");
        normalized = normalized.replaceAll("[？?！!。，,.；;：:\"\"''【】\\[\\]()（）]", "");
        if (normalized.length() > 100) {
            normalized = normalized.substring(0, 100);
        }
        return normalized;
    }

    private String extractKeywords(String question) {
        if (question == null || question.isBlank()) return null;
        
        Set<String> keywords = new HashSet<>();
        Matcher matcher = CHINESE_PATTERN.matcher(question);
        
        while (matcher.find()) {
            String word = matcher.group();
            if (!STOP_WORDS.contains(word)) {
                keywords.add(word);
            }
        }
        
        return keywords.stream().limit(5).collect(Collectors.joining(","));
    }

    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]{2,4}");
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "是", "在", "了", "和", "与", "或", "有", "这", "那", "我", "你", "他", "她",
            "们", "就", "也", "都", "而", "及", "着", "把", "被", "给", "让", "到", "从", "向",
            "什么", "怎么", "如何", "为什么", "哪里", "哪个", "多少", "可以", "能", "会"
    );
}
