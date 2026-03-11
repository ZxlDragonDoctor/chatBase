package com.zxl.chatbase.qq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxl.chatbase.chat.ChatService;
import com.zxl.chatbase.dify.config.ThreadPoolConfig;
import com.zxl.chatbase.im.entity.GroupMessage;
import com.zxl.chatbase.im.mapper.GroupMessageMapper;
import com.zxl.chatbase.dify.model.response.DifyChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * QQ / OneBot WebSocket 事件处理
 *
 * 适配 NapCat / go-cqhttp 的 OneBot v11 事件：
 * - 收到群消息时，如果 @ 了机器人，则调用 ChatService，并通过 OneBot 的 send_msg 动作回复
 *  TODO：NapCat 消息只实现了ws反向喜协议不支持上报事件，上报消息，采用Http接口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QqBotWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final QqBotProperties qqBotProperties;
    private final GroupMessageMapper groupMessageMapper;
    private final RestTemplate restTemplate;
    private final ThreadPoolExecutor threadPool = ThreadPoolConfig.createCustomThreadPool();


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

        String messageType = root.path("message_type").asText();
        if (!"group".equals(messageType)) {
            return;
        }

        String messageId = root.path("message_id").asText();
        long time = root.path("time").asLong(0L);
        long groupId = root.path("group_id").asLong();
        long userId = root.path("user_id").asLong();
        String rawMessage = root.path("raw_message").asText(root.path("message").asText(""));

        // 1. 无论是否 @ 机器人，先采集消息到数据库，异步写入
        CompletableFuture.runAsync(() ->
                saveGroupMessage(messageId, groupId, userId, rawMessage, messageType, time)
                ,threadPool);


        // 2. 仅当 @ 机器人时才触发问答
        if (!isAtSelf(rawMessage)) {
            return;
        }

        String query = stripAt(rawMessage);
        if (!StringUtils.hasText(query)) {
            return;
        }

        log.info("处理群聊消息: groupId={}, userId={}, query={}", groupId, userId, query);

       // 异步回答，防止阻塞大模型回答其他用户消息
        CompletableFuture<DifyChatResponse> completableFuture = CompletableFuture.supplyAsync(() -> {
           return  chatService.chat(
                   "im",
                   String.valueOf(userId),
                   String.valueOf(groupId),
                   query
           );
       },threadPool)
        .orTimeout(30, TimeUnit.SECONDS)
        .exceptionally((e)->{
                    log.error("聊天任务执行失败，groupId={}, userId={}", groupId, userId, e);
                    return null;
                });

        completableFuture.thenAccept((resp)->{
            String answer = resp != null ? resp.getAnswer() : "【系统错误】暂时无法回答，请稍后再试";
            sendGroupMessage(session, groupId, answer);
        });

    }


    private void saveGroupMessage(String messageId, long groupId, long userId,
                                  String rawMessage, String messageType, long time) {
        try {
            GroupMessage gm = new GroupMessage();
            gm.setPlatform("qq");
            gm.setGroupId(String.valueOf(groupId));
            gm.setUserId(String.valueOf(userId));
            gm.setMessageId(messageId);
            gm.setMessageType(messageType);
            gm.setRawMessage(rawMessage);
            if (time > 0) {
                gm.setMessageTime(java.time.LocalDateTime.ofEpochSecond(
                        time, 0, java.time.ZoneOffset.ofHours(8)));
            } else {
                gm.setMessageTime(java.time.LocalDateTime.now());
            }
            gm.setCreateTime(java.time.LocalDateTime.now());
            groupMessageMapper.insert(gm);
            log.info("群消息写库成功");
        } catch (Exception e) {
            log.error("保存群消息失败", e);
        }
    }

    /**
     * 向 OneBot 发送群消息
     *
     * 部分实现（例如 NapCat 的 OneBot 适配）对 action 名比较严格，
     * 这里采用兼容性更好的通用接口 send_msg，显式指定 message_type=group。
     */
    private void sendGroupMessage(WebSocketSession session, long groupId, String text) {
        try {
            String baseUrl = qqBotProperties.getHttpBaseUrl();
            if (!StringUtils.hasText(baseUrl)) {
                log.warn("未配置 qq.bot.http-base-url，无法通过 HTTP 向群发送消息");
                return;
            }
            String url = baseUrl.endsWith("/") ? baseUrl + "send_msg" : baseUrl + "/send_msg";

            Map<String, Object> body = new HashMap<>();
            body.put("message_type", "group");
            body.put("group_id", groupId);
            body.put("message", text);

            log.info("调用 OneBot HTTP 接口发送群消息, url={}, body={}", url, body);
            Map response = restTemplate.postForObject(url, body, Map.class);
            log.info("OneBot HTTP send_msg 响应: {}", response);
        } catch (Exception e) {
            log.error("发送群消息失败", e);
        }
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
}

