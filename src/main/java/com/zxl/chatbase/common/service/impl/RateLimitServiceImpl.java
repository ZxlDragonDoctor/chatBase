package com.zxl.chatbase.common.service.impl;

import com.zxl.chatbase.common.RateLimitException;
import com.zxl.chatbase.common.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private final StringRedisTemplate redisTemplate;

    private static final String RATE_LIMIT_KEY = "rate_limit:%s:%s:%s";
    private static final int DEFAULT_PER_USER_LIMIT = 10;
    private static final int DEFAULT_PER_GROUP_LIMIT = 30;
    private static final int WINDOW_SECONDS = 60;

    @Override
    public void checkRateLimit(String channel, String userId, String groupId) {
        String key = buildKey(channel, userId, groupId);
        try {
            String countStr = redisTemplate.opsForValue().get(key);
            int limit = getLimit(channel, userId, groupId);
            if (countStr != null) {
                int count = Integer.parseInt(countStr);
                if (count >= limit) {
                    throw new RateLimitException(
                            "请求过于频繁，请" + (WINDOW_SECONDS - getElapsedSeconds(key)) + "秒后重试",
                            WINDOW_SECONDS - getElapsedSeconds(key)
                    );
                }
            }
        } catch (RateLimitException e) {
            throw e;
        } catch (Exception e) {
            log.warn("限流检查失败，允许通过: {}", e.getMessage());
        }
    }

    @Override
    public void recordRequest(String channel, String userId, String groupId) {
        String key = buildKey(channel, userId, groupId);
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS));
            }
        } catch (Exception e) {
            log.warn("记录限流失败: {}", e.getMessage());
        }
    }

    @Override
    public void reset(String channel, String userId, String groupId) {
        String key = buildKey(channel, userId, groupId);
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("重置限流失败: {}", e.getMessage());
        }
    }

    private String buildKey(String channel, String userId, String groupId) {
        String compositeId = groupId != null ? groupId + ":" + userId : userId;
        return String.format(RATE_LIMIT_KEY, channel, compositeId, "window");
    }

    private int getLimit(String channel, String userId, String groupId) {
        if ("im".equals(channel) || "qq".equals(channel)) {
            return groupId != null ? DEFAULT_PER_GROUP_LIMIT : DEFAULT_PER_USER_LIMIT;
        }
        return DEFAULT_PER_USER_LIMIT;
    }

    private long getElapsedSeconds(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key);
            return ttl != null ? WINDOW_SECONDS - ttl : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}