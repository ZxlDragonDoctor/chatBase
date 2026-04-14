package com.zxl.chatbase.chat.service.impl;

import com.zxl.chatbase.chat.service.CleanupService;
import com.zxl.chatbase.kb.entity.KbConversation;
import com.zxl.chatbase.kb.mapper.KbConversationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleanupServiceImpl implements CleanupService {

    private final StringRedisTemplate redisTemplate;
    private final KbConversationMapper kbConversationMapper;

    @Value("${chat.cleanup.conversationDays:30}")
    private int conversationDays;

    @Value("${chat.cleanup.messageDays:90}")
    private int messageDays;

    @Override
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredConversations() {
        log.info("开始清理过期会话数据...");
        
        cleanupRedisConversations();
        cleanupDatabaseConversations();
        
        log.info("会话清理完成");
    }

    @Override
    @Scheduled(cron = "0 30 4 * * ?")
    public void cleanupOldMessages() {
        log.info("开始清理 {} 天前的消息...", messageDays);
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(messageDays);
        
        log.info("消息清理完成");
    }

    private void cleanupRedisConversations() {
        String pattern = "chat:conversation:*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                try {
                    Long ttl = redisTemplate.getExpire(key);
                    if (ttl != null && ttl == -1) {
                        Long expireAtSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                        if (expireAtSeconds == null || expireAtSeconds < 0) {
                            redisTemplate.delete(key);
                        }
                    }
                } catch (Exception e) {
                    log.warn("清理Redis key失败: {}", key);
                }
            }
            log.info("Redis会话清理完成，共清理 {} 条", keys.size());
        }
    }

    private void cleanupDatabaseConversations() {
        if (conversationDays <= 0) {
            return;
        }
        LocalDateTime cutoffTime = LocalDateTime.now().plusDays(-conversationDays);
        
        int deleted = kbConversationMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KbConversation>()
                        .lt(KbConversation::getCreateTime, cutoffTime)
        );
        log.info("数据库会话清理完成，共删除 {} 条", deleted);
    }

    public int getConversationDays() {
        return conversationDays;
    }

    public int getMessageDays() {
        return messageDays;
    }
}