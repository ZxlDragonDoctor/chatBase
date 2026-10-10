package com.zxl.chatbase.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redisTemplate;
    private static final String TOKEN_PREFIX = "token:";
    private static final Duration TOKEN_EXPIRE = Duration.ofDays(7);

    public String createToken(String username) {
        String token = UUID.randomUUID().toString();
        String key = TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(key, username, TOKEN_EXPIRE);
        log.info("创建token: username={}, tokenPrefix={}", username, maskToken(token));
        return token;
    }

    public String getUsernameByToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String key = TOKEN_PREFIX + token;
        return redisTemplate.opsForValue().get(key);
    }

    public boolean validateToken(String token) {
        return getUsernameByToken(token) != null;
    }

    public void removeToken(String token) {
        if (token != null && !token.isBlank()) {
            String key = TOKEN_PREFIX + token;
            redisTemplate.delete(key);
            log.info("删除token: tokenPrefix={}", maskToken(token));
        }
    }

    public void refreshToken(String token) {
        if (token != null && !token.isBlank()) {
            String key = TOKEN_PREFIX + token;
            redisTemplate.expire(key, TOKEN_EXPIRE);
        }
    }

    /** 日志脱敏：只保留前 8 位 */
    private static String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 8) + "****";
    }
}