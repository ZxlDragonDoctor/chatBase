package com.zxl.chatbase.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zxl.chatbase.im.entity.GroupMessage;
import com.zxl.chatbase.im.mapper.GroupMessageMapper;
import com.zxl.chatbase.kb.entity.KbConversation;
import com.zxl.chatbase.kb.entity.KbKeyword;
import com.zxl.chatbase.kb.mapper.KbConversationMapper;
import com.zxl.chatbase.kb.mapper.KbKeywordMapper;
import com.zxl.chatbase.kb.service.IKbKeywordService;
import com.zxl.chatbase.statistics.dto.KeywordHotVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KbKeywordServiceImpl implements IKbKeywordService {

    private final KbKeywordMapper keywordMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final KbConversationMapper conversationMapper;

    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]{2,8}");
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]{3,12}");
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "是", "在", "了", "和", "与", "或", "有", "这", "那", "我", "你", "他", "她", "它",
            "们", "就", "也", "都", "而", "及", "着", "把", "被", "给", "让", "到", "从", "向", "往",
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could", "should",
            "can", "may", "might", "must", "shall", "to", "of", "in", "for", "on", "with",
            "at", "by", "from", "as", "into", "through", "during", "before", "after",
            "above", "below", "between", "under", "again", "further", "then", "once",
            "here", "there", "when", "where", "why", "how", "all", "each", "few", "more",
            "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same",
            "what", "which", "who", "whom", "this", "that", "these", "those", "and", "but"
    );

    @Override
    public void extractAndSaveKeywords(String text, String source) {
        if (text == null || text.isBlank()) {
            return;
        }

        Set<String> keywords = new HashSet<>();

        Matcher chineseMatcher = CHINESE_PATTERN.matcher(text);
        while (chineseMatcher.find()) {
            String word = chineseMatcher.group();
            if (!STOP_WORDS.contains(word)) {
                keywords.add(word);
            }
        }

        Matcher englishMatcher = ENGLISH_PATTERN.matcher(text);
        while (englishMatcher.find()) {
            String word = englishMatcher.group().toLowerCase();
            if (!STOP_WORDS.contains(word)) {
                keywords.add(word);
            }
        }

        for (String keyword : keywords) {
            try {
                keywordMapper.incrementCount(keyword, source);
            } catch (Exception e) {
                log.warn("保存关键词失败: {}", keyword, e);
            }
        }
    }

    @Override
    public KeywordHotVO getKeywordCloud(String source, Integer days, Integer limit) {
        QueryWrapper<KbKeyword> wrapper = new QueryWrapper<>();

        if (source != null && !source.equals("all")) {
            wrapper.eq("source", source);
        }

        if (days != null && days > 0) {
            LocalDateTime startTime = LocalDate.now().minusDays(days).atStartOfDay();
            wrapper.ge("last_seen_time", startTime);
        }

        wrapper.orderByDesc("count")
                .last("LIMIT " + (limit != null && limit > 0 ? limit : 50));

        List<KbKeyword> keywords = keywordMapper.selectList(wrapper);

        List<KeywordHotVO.KeywordItem> items = new ArrayList<>();
        int rank = 1;
        int maxCount = keywords.isEmpty() ? 1 : keywords.get(0).getCount();

        for (KbKeyword kw : keywords) {
            KeywordHotVO.KeywordItem item = new KeywordHotVO.KeywordItem();
            item.setKeyword(kw.getKeyword());
            item.setCount(kw.getCount());
            item.setRank(rank++);
            items.add(item);
        }

        KeywordHotVO vo = new KeywordHotVO();
        vo.setKeywords(items);
        vo.setPlatform(source);
        return vo;
    }

    @Override
    public List<String> getTopKeywords(String source, Integer limit) {
        QueryWrapper<KbKeyword> wrapper = new QueryWrapper<>();

        if (source != null && !source.equals("all")) {
            wrapper.eq("source", source);
        }

        wrapper.orderByDesc("count")
                .last("LIMIT " + (limit != null && limit > 0 ? limit : 20));

        return keywordMapper.selectList(wrapper).stream()
                .map(KbKeyword::getKeyword)
                .collect(Collectors.toList());
    }

    @Override
    public void cleanOldKeywords(int retentionDays) {
        LocalDateTime cutoffTime = LocalDate.now().minusDays(retentionDays).atStartOfDay();

        QueryWrapper<KbKeyword> wrapper = new QueryWrapper<>();
        wrapper.lt("last_seen_time", cutoffTime);

        int deleted = keywordMapper.delete(wrapper);
        log.info("清理过期关键词: {} 条", deleted);
    }

    @Override
    public int batchExtractFromMessages(String platform, int days) {
        QueryWrapper<GroupMessage> wrapper = new QueryWrapper<>();
        wrapper.select("raw_message", "platform");
        wrapper.isNotNull("raw_message").ne("raw_message", "");
        
        if (days > 0) {
            LocalDateTime startTime = LocalDate.now().minusDays(days).atStartOfDay();
            wrapper.ge("message_time", startTime);
        }
        
        if (platform != null && !platform.equals("all")) {
            wrapper.eq("platform", platform);
        }

        wrapper.orderByDesc("message_time").last("LIMIT 5000");

        List<GroupMessage> messages = groupMessageMapper.selectList(wrapper);
        
        int count = 0;
        for (GroupMessage msg : messages) {
            if (msg.getRawMessage() != null && !msg.getRawMessage().isBlank()) {
                String source = msg.getPlatform() != null ? msg.getPlatform() : "im";
                extractAndSaveKeywords(msg.getRawMessage(), source);
                count++;
            }
        }
        
        log.info("批量提取群聊关键词完成: 处理 {} 条消息", count);
        return count;
    }

    @Override
    public int batchExtractFromConversations(int days) {
        QueryWrapper<KbConversation> wrapper = new QueryWrapper<>();
        wrapper.select("query");
        wrapper.isNotNull("query").ne("query", "");
        
        if (days > 0) {
            LocalDateTime startTime = LocalDate.now().minusDays(days).atStartOfDay();
            wrapper.ge("create_time", startTime);
        }

        wrapper.orderByDesc("create_time").last("LIMIT 5000");

        List<KbConversation> conversations = conversationMapper.selectList(wrapper);
        
        int count = 0;
        for (KbConversation conv : conversations) {
            if (conv.getQuery() != null && !conv.getQuery().isBlank()) {
                extractAndSaveKeywords(conv.getQuery(), "web");
                count++;
            }
        }
        
        log.info("批量提取Web对话关键词完成: 处理 {} 条对话", count);
        return count;
    }

    @Override
    public int syncLatestKeywords(int limit) {
        int totalCount = 0;
        
        QueryWrapper<GroupMessage> msgWrapper = new QueryWrapper<>();
        msgWrapper.select("raw_message", "platform");
        msgWrapper.isNotNull("raw_message").ne("raw_message", "");
        msgWrapper.orderByDesc("message_time").last("LIMIT " + limit);

        List<GroupMessage> messages = groupMessageMapper.selectList(msgWrapper);
        for (GroupMessage msg : messages) {
            if (msg.getRawMessage() != null && !msg.getRawMessage().isBlank()) {
                String source = msg.getPlatform() != null ? msg.getPlatform() : "im";
                extractAndSaveKeywords(msg.getRawMessage(), source);
                totalCount++;
            }
        }
        
        QueryWrapper<KbConversation> convWrapper = new QueryWrapper<>();
        convWrapper.select("query");
        convWrapper.isNotNull("query").ne("query", "");
        convWrapper.orderByDesc("create_time").last("LIMIT " + limit);

        List<KbConversation> conversations = conversationMapper.selectList(convWrapper);
        for (KbConversation conv : conversations) {
            if (conv.getQuery() != null && !conv.getQuery().isBlank()) {
                extractAndSaveKeywords(conv.getQuery(), "web");
                totalCount++;
            }
        }
        
        log.info("同步关键词完成: 群聊 {} 条, Web对话 {} 条", messages.size(), conversations.size());
        return totalCount;
    }
}