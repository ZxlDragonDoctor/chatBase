package com.zxl.chatbase.qq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxl.chatbase.chat.service.ChatService;
import com.zxl.chatbase.config.ChatProperties;
import com.zxl.chatbase.im.entity.ImGroup;
import com.zxl.chatbase.im.entity.ImUser;
import com.zxl.chatbase.im.mapper.GroupMessageMapper;
import com.zxl.chatbase.im.mapper.ImGroupMapper;
import com.zxl.chatbase.im.mapper.ImUserMapper;
import com.zxl.chatbase.dify.model.response.DifyChatResponse;
import com.zxl.chatbase.im.service.GroupMessageSyncService;
import com.zxl.chatbase.kb.entity.KbApp;
import com.zxl.chatbase.kb.mapper.KbAppMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * QQ / OneBot WebSocket 事件处理
 *
 * 适配 NapCat / go-cqhttp 的 OneBot v11 事件：
 * - 收到群消息时，如果 @ 了机器人，则调用 ChatService，并通过 OneBot 的 send_group_msg 动作回复
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QqBotWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final QqBotProperties qqBotProperties;
    private final GroupMessageMapper groupMessageMapper;
    private final GroupMessageSyncService groupMessageSyncService;
    private final ImGroupMapper imGroupMapper;
    private final ImUserMapper imUserMapper;
    private final KbAppMapper kbAppMapper;
    private final RestTemplate restTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ChatProperties chatProperties;
    private final ThreadPoolExecutor threadPool;

    private static final String RATE_KEY_PREFIX = "chat:rate:im:";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("QQ Bot WebSocket 已连接, id={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("收到 QQ Bot 消息: {}", payload);

        JsonNode root = objectMapper.readTree(payload);
        String postType = root.path("post_type").asText();

        // 只处理消息事件
        if (!"message".equals(postType)) {
            return;
        }

        // 只开放群聊功能
        String messageType = root.path("message_type").asText();
        if (!"group".equals(messageType)) {
            return;
        }

        String messageId = root.path("messageId").asText();
        long time = root.path("time").asLong(0L);
        String groupId = root.path("group_id").asText();
        String userId = root.path("user_id").asText();
        String rawMessage = root.path("raw_message").asText(root.path("message").asText(""));

        String msgType = "text";
        String fileUrl = null;
        String fileName = null;

        JsonNode messageNode = root.path("message");
        if (messageNode.isArray()) {
            for (JsonNode seg : messageNode) {
                String segType = seg.path("type").asText();
                if ("image".equals(segType)) {
                    msgType = "image";
                    fileUrl = seg.path("data").path("url").asText(null);
                    fileName = seg.path("data").path("file").asText(null);
                    log.info("解析到图片消息: groupId={}, url={}", groupId, fileUrl);
                    break;
                } else if ("file".equals(segType)) {
                    msgType = "file";
                    fileUrl = seg.path("data").path("url").asText(null);
                    fileName = seg.path("data").path("file").asText(null);
                    log.info("解析到文件消息: groupId={}, url={}, name={}", groupId, fileUrl, fileName);
                    break;
                }
            }
        }

        final String finalMsgType = msgType;
        final String finalFileUrl = fileUrl;
        final String finalFileName = fileName;

        // 1. 无论是否 @ 机器人，先采集消息到数据库（异步写入，避免阻塞消息处理）
        CompletableFuture.runAsync(
                () -> {
                    groupMessageSyncService.saveGroupMessage("qq", messageId, groupId, userId, rawMessage, finalMsgType, time, finalFileUrl, finalFileName);
                    syncGroupAndUser(groupId, userId, root);
                },
                threadPool
        );

        // 2. 仅当 @ 机器人时才触发问答
        if (!isAtSelf(rawMessage)) {
            return;
        }

        String query = stripAt(rawMessage);
        if (!StringUtils.hasText(query)) {
            return;
        }

        // 3. 限流：避免频繁 @ 机器人打爆 Dify
        if (isRateLimited(groupId, userId)) {
            sendGroupMessage(session, groupId, "请求太频繁，请稍后再试");
            return;
        }

        log.info("处理群聊消息: groupId={}, userId={}, query={}", groupId, userId, query);
        
        // 获取群组绑定的应用
        Long appId = getAppIdForGroup(groupId);
        log.info("群组应用绑定: groupId={}, appId={}", groupId, appId);
        
        // 4. 异步回答，避免阻塞 WebSocket 消息线程
        CompletableFuture.supplyAsync(() -> chatService.chat(
                        appId,
                        "im",
                        String.valueOf(userId),
                        String.valueOf(groupId),
                        query
                ), threadPool)
                .orTimeout(160, TimeUnit.SECONDS)
                .exceptionally(e -> {
                    log.error("聊天任务执行失败，groupId={}, userId={}", groupId, userId, e);
                    DifyChatResponse fallback = new DifyChatResponse();
                    fallback.setAnswer("【系统繁忙】回答超时，请稍后再试");
                    return fallback;
                })
                .thenAccept(resp -> {
                    String answer = resp != null ? resp.getAnswer() : "【系统错误】暂时无法回答，请稍后再试";
                    // 过滤AI思考过程（支持多行和多个块）
                    answer = filterThinkingContent(answer);
                    sendGroupMessage(session, groupId, answer);
                });
    }


    /**
     * 向 OneBot 发送群消息
     *
     * 部分实现（例如 NapCat 的 OneBot 适配）对 action 名比较严格，
     * 这里采用兼容性更好的通用接口 send_msg，显式指定 message_type=group。
     */
    private void sendGroupMessage(WebSocketSession session, String groupId, String text) {
        try {
            String baseUrl = qqBotProperties.getHttpBaseUrl();
            if (!StringUtils.hasText(baseUrl)) {
                log.warn("未配置 qq.bot.http-base-url，无法通过 HTTP 向群发送消息");
                return;
            }
            String url = baseUrl.endsWith("/") ? baseUrl + "send_msg" : baseUrl + "/send_msg";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String accessToken = qqBotProperties.getAccessToken();
            if (StringUtils.hasText(accessToken)) {
                headers.set("Authorization", "Bearer " + accessToken);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("message_type", "group");
            body.put("group_id", groupId);
            body.put("message", buildMarkdownMessage(text));

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            log.info("调用 OneBot HTTP 接口发送群消息(Markdown), url={}", url);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) restTemplate.postForObject(url, requestEntity, Map.class);
            log.info("OneBot HTTP send_msg 响应: {}", response);

            if (response != null && "failed".equals(response.get("status"))) {
                log.warn("Markdown消息发送失败，尝试降级为纯文本发送, groupId={}, retcode={}", 
                        groupId, response.get("retcode"));
                
                body.put("message", text);
                requestEntity = new HttpEntity<>(body, headers);
                response = (Map<String, Object>) restTemplate.postForObject(url, requestEntity, Map.class);
                log.info("纯文本消息发送响应: {}", response);
            }
        } catch (Exception e) {
            log.error("发送群消息失败, groupId={}, text={}", groupId, text, e);
        }
    }

    /**
     * 过滤AI思考过程内容，避免暴露给群聊用户
     * 支持多行和多个 <think>...</think> 块
     */
    private String filterThinkingContent(String text) {
        if (text == null) return "";
        // 使用正则表达式过滤 <think>...</think> 内容（支持多行）
        String filtered = text.replaceAll("(?s)<think>.*?</think>", "").trim();
        return filtered.isEmpty() ? text : filtered;
    }

    /**
     * 构建双层嵌套 Markdown 消息（NapCat 支持）
     * 外层 node -> 内层 node -> markdown
     */
    private List<Map<String, Object>> buildMarkdownMessage(String text) {
        List<Map<String, Object>> messageList = new ArrayList<>();
        
        Map<String, Object> outerNode = new HashMap<>();
        outerNode.put("type", "node");
        
        Map<String, Object> outerData = new HashMap<>();
        List<Map<String, Object>> content = new ArrayList<>();
        
        Map<String, Object> innerNode = new HashMap<>();
        innerNode.put("type", "node");
        
        Map<String, Object> innerData = new HashMap<>();
        innerData.put("nickname", "ChatBase");
        innerData.put("user_id", String.valueOf(qqBotProperties.getSelfId()));
        
        List<Map<String, Object>> innerContent = new ArrayList<>();
        Map<String, Object> markdown = new HashMap<>();
        markdown.put("type", "markdown");
        Map<String, Object> mdData = new HashMap<>();
        mdData.put("content", text);
        markdown.put("data", mdData);
        innerContent.add(markdown);
        
        innerData.put("content", innerContent);
        innerNode.put("data", innerData);
        content.add(innerNode);
        
        outerData.put("content", content);
        outerNode.put("data", outerData);
        messageList.add(outerNode);
        
        return messageList;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("QQ Bot WebSocket 已关闭, id={}, status={}", session.getId(), status);
    }

    private boolean isAtSelf(String msg) {
        if (!StringUtils.hasText(msg)) {
            return false;
        }
        // OneBot CQ 码格式: [CQ:at,qq=123456]
        String cqAt = "[CQ:at,qq=" + qqBotProperties.getSelfId() + "]";
        return msg.contains(cqAt);
    }

    private String stripAt(String msg) {
        if (!StringUtils.hasText(msg)) {
            return msg;
        }
        String cqAt = "[CQ:at,qq=" + qqBotProperties.getSelfId() + "]";
        String result = msg.replace(cqAt, "").trim();
        // 兼容直接 @昵称 文本
        return result.replaceAll("^@\\S+\\s*", "").trim();
    }

    /**
     * 获取群组绑定的应用ID
     * 优先使用群组绑定的应用，如果没有则使用默认应用
     */
    private Long getAppIdForGroup(String groupId) {
        try {
            LambdaQueryWrapper<ImGroup> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ImGroup::getGroupId, groupId)
                    .eq(ImGroup::getStatus, true);
            ImGroup group = imGroupMapper.selectOne(wrapper);
            
            if (group != null && group.getAppId() != null) {
                KbApp app = kbAppMapper.selectById(group.getAppId());
                if (app != null && app.getStatus()) {
                    return app.getId();
                }
            }
            
            LambdaQueryWrapper<KbApp> appWrapper = new LambdaQueryWrapper<>();
            appWrapper.eq(KbApp::getStatus, true)
                    .eq(KbApp::getIsDefault, true)
                    .last("LIMIT 1");
            KbApp defaultApp = kbAppMapper.selectOne(appWrapper);
            return defaultApp != null ? defaultApp.getId() : null;
        } catch (Exception e) {
            log.error("获取群组应用失败: groupId={}", groupId, e);
            return null;
        }
    }

    private boolean isRateLimited(String groupId, String userId) {
        ChatProperties.RateLimit rl = chatProperties.getRateLimit();
        long windowSeconds = Math.max(1, rl.getWindowSeconds());
        long maxRequests = Math.max(1, rl.getMaxRequests());

        String key = RATE_KEY_PREFIX + groupId + ":" + userId;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }
        return count != null && count > maxRequests;
    }

    private void syncGroupAndUser(String groupId, String userId, JsonNode root) {
        try {
            syncGroup(groupId, root);
            syncUser(userId, groupId, root);
        } catch (Exception e) {
            log.error("同步群组/用户信息失败: groupId={}, userId={}", groupId, userId, e);
        }
    }

    private void syncGroup(String groupId, JsonNode root) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ImGroup> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ImGroup>()
                        .eq(ImGroup::getPlatform, "qq")
                        .eq(ImGroup::getGroupId, groupId);
        ImGroup imGroup = imGroupMapper.selectOne(wrapper);
        if (imGroup == null) {
            imGroup = new ImGroup();
            imGroup.setPlatform("qq");
            imGroup.setGroupId(groupId);
            imGroup.setStatus(true);
            imGroup.setAutoReply(true);
            imGroup.setMemberCount(0);
        }
        JsonNode groupNameNode = root.path("group_name");
        if (groupNameNode.isTextual()) {
            imGroup.setGroupName(groupNameNode.asText());
        }
        JsonNode senderNode = root.path("sender");
        if (senderNode.isObject()) {
            JsonNode roleNode = senderNode.path("role");
            if (roleNode.isTextual() && "owner".equals(roleNode.asText())) {
                JsonNode userIdNode = senderNode.path("user_id");
                if (userIdNode.isTextual()) {
                    imGroup.setOwnerId(userIdNode.asText());
                }
            }
        }
        if (imGroup.getId() == null) {
            imGroupMapper.insert(imGroup);
        } else {
            imGroupMapper.updateById(imGroup);
        }
    }

    private void syncUser(String userId, String groupId, JsonNode root) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ImUser> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ImUser>()
                        .eq(ImUser::getPlatform, "qq")
                        .eq(ImUser::getUserId, userId);
        ImUser imUser = imUserMapper.selectOne(wrapper);
        if (imUser == null) {
            imUser = new ImUser();
            imUser.setPlatform("qq");
            imUser.setUserId(userId);
            imUser.setStatus(true);
            imUser.setRole("member");
            imUser.setMessageCount(0);
        }
        imUser.setGroupId(groupId);
        JsonNode senderNode = root.path("sender");
        if (senderNode.isObject()) {
            JsonNode nicknameNode = senderNode.path("nickname");
            if (nicknameNode.isTextual()) {
                imUser.setNickname(nicknameNode.asText());
            }
            JsonNode cardNode = senderNode.path("card");
            if (cardNode.isTextual() && cardNode.asText().length() > 0) {
                imUser.setNickname(cardNode.asText());
            }
            JsonNode roleNode = senderNode.path("role");
            if (roleNode.isTextual()) {
                imUser.setRole(roleNode.asText());
            }
        }
        imUser.setLastMessageTime(java.time.LocalDateTime.now());
        imUser.setMessageCount(imUser.getMessageCount() + 1);
        if (imUser.getId() == null) {
            imUserMapper.insert(imUser);
        } else {
            imUserMapper.updateById(imUser);
        }
    }
}

