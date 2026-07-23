package com.zxl.chatbase.feedback.service.impl;

import com.zxl.chatbase.feedback.service.IFeedbackStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackStatsServiceImpl implements IFeedbackStatsService {

    private final StringRedisTemplate redisTemplate;

    private static final String THUMBS_UP_KEY = "feedback:thumbs_up:daily:";
    private static final String THUMBS_DOWN_KEY = "feedback:thumbs_down:daily:";
    private static final String SESSION_FEEDBACK = "feedback:session:";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public boolean recordThumbsUp(String sessionId, int messageIndex) {
        String feedbackKey = SESSION_FEEDBACK + sessionId + ":" + messageIndex;
        
        String existing = redisTemplate.opsForValue().get(feedbackKey);
        if (existing != null) {
            log.info("已反馈过: sessionId={}, index={}, existing={}", sessionId, messageIndex, existing);
            return false;
        }
        
        String today = LocalDate.now().format(DATE_FORMAT);
        String dailyKey = THUMBS_UP_KEY + today;
        
        redisTemplate.opsForValue().increment(dailyKey);
        redisTemplate.expire(dailyKey, 90, TimeUnit.DAYS);
        redisTemplate.opsForValue().set(feedbackKey, "1", 30, TimeUnit.DAYS);
        
        log.info("记录点赞: sessionId={}, index={}", sessionId, messageIndex);
        return true;
    }

    @Override
    public boolean recordThumbsDown(String sessionId, int messageIndex) {
        String feedbackKey = SESSION_FEEDBACK + sessionId + ":" + messageIndex;
        
        String existing = redisTemplate.opsForValue().get(feedbackKey);
        if (existing != null) {
            log.info("已反馈过: sessionId={}, index={}, existing={}", sessionId, messageIndex, existing);
            return false;
        }
        
        String today = LocalDate.now().format(DATE_FORMAT);
        String dailyKey = THUMBS_DOWN_KEY + today;
        
        redisTemplate.opsForValue().increment(dailyKey);
        redisTemplate.expire(dailyKey, 90, TimeUnit.DAYS);
        redisTemplate.opsForValue().set(feedbackKey, "0", 30, TimeUnit.DAYS);
        
        log.info("记录踩: sessionId={}, index={}", sessionId, messageIndex);
        return true;
    }

    @Override
    public Map<Integer, Integer> getSessionFeedbackStatus(String sessionId) {
        Map<Integer, Integer> result = new HashMap<>();
        
        String pattern = SESSION_FEEDBACK + sessionId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        
        if (keys != null) {
            for (String key : keys) {
                String[] parts = key.split(":");
                if (parts.length >= 3) {
                    try {
                        int messageIndex = Integer.parseInt(parts[parts.length - 1]);
                        String value = redisTemplate.opsForValue().get(key);
                        if (value != null) {
                            result.put(messageIndex, Integer.parseInt(value));
                        }
                    } catch (NumberFormatException e) {
                        log.warn("解析消息索引失败: {}", key);
                    }
                }
            }
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getDailyStats(int days) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Long> thumbsUpDaily = new HashMap<>();
        Map<String, Long> thumbsDownDaily = new HashMap<>();
        
        long totalUp = 0;
        long totalDown = 0;
        
        for (int i = 0; i < days; i++) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dateKey = date.format(DATE_FORMAT);
            String dateStr = date.toString();
            
            Long upCount = getLongValue(THUMBS_UP_KEY + dateKey);
            Long downCount = getLongValue(THUMBS_DOWN_KEY + dateKey);
            
            thumbsUpDaily.put(dateStr, upCount);
            thumbsDownDaily.put(dateStr, downCount);
            
            totalUp += upCount;
            totalDown += downCount;
        }
        
        result.put("thumbsUpDaily", thumbsUpDaily);
        result.put("thumbsDownDaily", thumbsDownDaily);
        result.put("totalThumbsUp", totalUp);
        result.put("totalThumbsDown", totalDown);
        result.put("positiveRate", calculateRate(totalUp, totalDown));
        
        return result;
    }

    @Override
    public Map<String, Object> getOverallStats() {
        Map<String, Object> result = new HashMap<>();
        
        result.put("totalThumbsUp", getThumbsUpCount(30));
        result.put("totalThumbsDown", getThumbsDownCount(30));
        result.put("positiveRate", getPositiveRate());
        
        return result;
    }

    @Override
    public long getThumbsUpCount(int days) {
        long total = 0;
        for (int i = 0; i < days; i++) {
            String dateKey = LocalDate.now().minusDays(i).format(DATE_FORMAT);
            total += getLongValue(THUMBS_UP_KEY + dateKey);
        }
        return total;
    }

    @Override
    public long getThumbsDownCount(int days) {
        long total = 0;
        for (int i = 0; i < days; i++) {
            String dateKey = LocalDate.now().minusDays(i).format(DATE_FORMAT);
            total += getLongValue(THUMBS_DOWN_KEY + dateKey);
        }
        return total;
    }

    @Override
    public double getPositiveRate() {
        long up = getThumbsUpCount(30);
        long down = getThumbsDownCount(30);
        return calculateRate(up, down);
    }

    private Long getLongValue(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }

    private double calculateRate(long up, long down) {
        long total = up + down;
        if (total == 0) return 100.0;
        return (up * 100.0) / total;
    }
}