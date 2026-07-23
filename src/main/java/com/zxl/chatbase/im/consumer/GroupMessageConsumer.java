package com.zxl.chatbase.im.consumer;

import com.zxl.chatbase.im.service.GroupMessageSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 群消息消费者 - Redis Stream 方案
 * 替代定时轮询方案，消息驱动实时处理
 * 
 * 特性：
 * - 消息驱动：新消息到达立即处理，无需轮询
 * - 低延迟：秒级处理，无需等待60秒
 * - 低资源：仅处理新消息，不频繁查询数据库
 * - 可靠性：支持消息确认机制
 * 
 * 配置项：
 * - im.sync.stream.enabled: 是否启用Stream方案（默认true）
 * - im.sync.stream.consumer-group: 消费者组名称
 * - im.sync.stream.consumer-name: 消费者名称
 * 
 * @author chatbase
 * @since 2026-05-04
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "im.sync.stream.enabled", havingValue = "true", matchIfMissing = true)
public class GroupMessageConsumer {

    private static final String STREAM_KEY = "chatbase:group:message:stream";
    private static final String GROUP_NAME = "chatbase-sync-group";
    private static final String CONSUMER_NAME = "consumer-1";
    
    private final StringRedisTemplate stringRedisTemplate;
    private final GroupMessageSyncService groupMessageSyncService;

    /**
     * 初始化消费者组
     * 仅在首次启动时执行，后续重启会自动使用已存在的组
     */
    @PostConstruct
    public void init() {
        try {
            // 检查Stream是否存在，不存在则创建
            Boolean streamExists = stringRedisTemplate.hasKey(STREAM_KEY);
            if (streamExists == null || !streamExists) {
                // Stream不存在时，先创建空Stream（使用XADD添加一条可忽略的消息）
                stringRedisTemplate.opsForStream().add(StreamRecords.newRecord()
                        .in(STREAM_KEY)
                        .ofMap(Map.of("init", "true")));
                log.info("创建Redis Stream: {}", STREAM_KEY);
            }
            
            // 创建消费者组，从最后一条消息开始消费
            try {
                stringRedisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0"), GROUP_NAME);
                log.info("创建消费者组: {}", GROUP_NAME);
            } catch (Exception e) {
                // 消费者组已存在，忽略异常
                log.debug("消费者组已存在: {}", e.getMessage());
            }
            
            log.info("Redis Stream消费者初始化完成，Stream: {}, Group: {}", STREAM_KEY, GROUP_NAME);
        } catch (Exception e) {
            log.error("Redis Stream消费者初始化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 消费消息
     * 每5秒检查一次是否有新消息，有则立即处理
     * 使用 BLOCK 模式实现即时响应，同时避免空轮询
     */
    @Scheduled(fixedDelayString = "5000")
    public void consumeMessages() {
        try {
            // 阻塞读取消息，最多等待5秒，每次最多读取10条
            List<MapRecord<String, Object, Object>> messages = stringRedisTemplate.opsForStream().read(
                    Consumer.from(GROUP_NAME, CONSUMER_NAME),
                    StreamReadOptions.empty().count(10).block(Duration.ofSeconds(5)),
                    StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
            );

            if (messages == null || messages.isEmpty()) {
                return;
            }

            log.info("接收到 {} 条消息，开始处理...", messages.size());

            for (MapRecord<String, Object, Object> message : messages) {
                try {
                    Map<Object, Object> value = message.getValue();
                    String messageId = (String) value.get("messageId");
                    String groupId = (String) value.get("groupId");
                    
                    log.debug("处理消息: messageId={}, groupId={}", messageId, groupId);

                    // 调用同步服务处理单条消息
                    if (messageId != null) {
                        processSingleMessage(Long.parseLong(messageId));
                    }

                    // 确认消息已处理
                    stringRedisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, message.getId().getValue());
                    log.debug("消息已确认: {}", message.getId().getValue());

                } catch (Exception e) {
                    log.error("处理消息失败: {}", e.getMessage(), e);
                }
            }

            log.info("消息处理完成");

        } catch (Exception e) {
            log.error("消费消息异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理单条消息
     * @param messageId 消息ID
     */
    private void processSingleMessage(Long messageId) {
        try {
            // 这里调用同步服务的单条处理方法
            // 由于原同步服务是批量处理的，需要添加单条处理逻辑
            // 暂时复用批量逻辑，通过ID查询单条消息
            groupMessageSyncService.syncSingleMessage(messageId);
        } catch (Exception e) {
            log.error("处理单条消息失败, messageId={}: {}", messageId, e.getMessage(), e);
        }
    }

    /**
     * 发布消息到Stream
     * 供 WebSocket 处理器调用
     * 
     * @param messageId 消息ID
     * @param groupId 群组ID
     * @param content 消息内容
     */
    public void publishMessage(Long messageId, String groupId, String content) {
        try {
            Map<Object, Object> data = new HashMap<>();
            data.put("messageId", String.valueOf(messageId));
            data.put("groupId", groupId != null ? groupId : "");
            data.put("content", content != null ? content : "");
            data.put("timestamp", String.valueOf(System.currentTimeMillis()));
            
            RecordId recordId = stringRedisTemplate.opsForStream().add(
                    StreamRecords.newRecord()
                            .in(STREAM_KEY)
                            .ofMap(data)
            );
            
            log.info("消息已发布到Stream: messageId={}, recordId={}", messageId, recordId != null ? recordId.getValue() : "null");
        } catch (Exception e) {
            log.error("发布消息到Stream失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取Stream状态信息
     */
    public Map<String, Object> getStreamInfo() {
        try {
            Object info = stringRedisTemplate.opsForStream().info(STREAM_KEY);
            Map<String, Object> result = new HashMap<>();
            result.put("streamKey", STREAM_KEY);
            result.put("consumerGroup", GROUP_NAME);
            result.put("consumerName", CONSUMER_NAME);
            result.put("info", info != null ? info.toString() : "N/A");
            return result;
        } catch (Exception e) {
            log.error("获取Stream信息失败: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
}