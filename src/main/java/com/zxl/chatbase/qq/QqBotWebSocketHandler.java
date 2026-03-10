package com.zxl.chatbase.qq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxl.chatbase.chat.ChatService;
import com.zxl.chatbase.im.entity.GroupMessage;
import com.zxl.chatbase.im.mapper.GroupMessageMapper;
import com.zxl.chatbase.dify.model.response.DifyChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

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

        // 1. 无论是否 @ 机器人，先采集消息到数据库
        saveGroupMessage(messageId, groupId, userId, rawMessage, messageType, time);

        // 2. 仅当 @ 机器人时才触发问答
        if (!isAtSelf(rawMessage)) {
            return;
        }

        String query = stripAt(rawMessage);
        if (!StringUtils.hasText(query)) {
            return;
        }

        log.info("处理群聊消息: groupId={}, userId={}, query={}", groupId, userId, query);

        DifyChatResponse resp = chatService.chat(
                "im",
                String.valueOf(userId),
                String.valueOf(groupId),
                query
        );

        String answer = resp != null ? resp.getAnswer() : "【系统错误】暂时无法回答，请稍后再试";
        sendGroupMessage(session, groupId, answer);
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
        } catch (Exception e) {
            log.error("保存群消息失败", e);
        }
    }

    /**
     * 向 OneBot 发送 send_group_msg 动作
     */
    private void sendGroupMessage(WebSocketSession session, long groupId, String text) {
        try {
            JsonNode root = objectMapper.createObjectNode()
                    .put("action", "send_group_msg")
                    .putObject("params")
                    .put("group_id", groupId)
                    .put("message", text);

            String json = objectMapper.writeValueAsString(root);
            session.sendMessage(new TextMessage(json));
            log.info("已向群 {} 发送消息", groupId);
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

