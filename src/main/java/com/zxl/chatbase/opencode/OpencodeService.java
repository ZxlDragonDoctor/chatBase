package com.zxl.chatbase.opencode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxl.chatbase.config.OpencodeProperties;
import com.zxl.chatbase.kb.service.IKbConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * 本地 opencode serve 集成服务
 *
 * 通过 opencode serve 的 HTTP API 与本机 opencode 交互：
 * - conversationId(IM单聊) 与 opencode sessionId 的映射保存在 Redis，保证同一私聊会话连续对话
 * - 创建会话 -> 发送消息 -> 轮询消息获取回复
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpencodeService {

    private static final String SESSION_KEY_PREFIX = "opencode:session:";
    private static final String PROMPT_KEY_PREFIX = "opencode:prompt:";

    private final OpencodeProperties properties;
    private final RestTemplate restTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final IKbConversationService kbConversationService;

    /**
     * 发送消息给 opencode 并返回回复文本
     *
     * @param conversationId IM 单聊会话ID（如 single:qq:xxx）
     * @param query          用户消息
     * @param userId         平台用户ID
     * @param channel        平台标识（qq/wecom/wx）
     */
    public String chat(String conversationId, String query, String userId, String channel) {
        if (!properties.isEnabled()) {
            return "【本地opencode未启用】请联系管理员在服务器上配置并启动 opencode serve 服务。";
        }
        if (!StringUtils.hasText(query)) {
            return "请输入要执行的任务或问题。";
        }

        String sessionId = getOrCreateSession(conversationId);
        long promptSentAt = System.currentTimeMillis();
        sendPrompt(sessionId, query);
        String answer = pollAnswer(sessionId, promptSentAt);
        if (!StringUtils.hasText(answer)) {
            answer = "【opencode未返回结果】可能是执行超时或出错，请检查本地 opencode 状态。";
        }

        saveConversation(conversationId, userId, channel, query, answer);
        return answer;
    }

    private String getOrCreateSession(String conversationId) {
        String cacheKey = SESSION_KEY_PREFIX + conversationId;
        String sessionId = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(sessionId)) {
            return sessionId;
        }

        try {
            HttpHeaders headers = buildHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String createBody = "{}";
            if (StringUtils.hasText(properties.getDefaultDirectory())) {
                createBody = "{\"directory\":\"" + escapeJson(properties.getDefaultDirectory()) + "\",\"agent\":\"" + escapeJson(properties.getDefaultAgent()) + "\"}";
            } else {
                createBody = "{\"agent\":\"" + escapeJson(properties.getDefaultAgent()) + "\"}";
            }
            HttpEntity<String> request = new HttpEntity<>(createBody, headers);

            String url = properties.getBaseUrl() + "/api/session";
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode data = root.path("data");
            sessionId = data.path("id").asText(null);
            if (!StringUtils.hasText(sessionId)) {
                sessionId = data.asText(null);
            }
            if (!StringUtils.hasText(sessionId)) {
                log.error("opencode 创建会话失败: resp={}", resp.getBody());
                return null;
            }
            stringRedisTemplate.opsForValue().set(cacheKey, sessionId, Duration.ofDays(7));
            log.info("opencode 创建会话成功: conversationId={}, opencodeSession={}", conversationId, sessionId);
            return sessionId;
        } catch (Exception e) {
            log.error("opencode 创建会话异常: conversationId={}", conversationId, e);
            return null;
        }
    }

    private void sendPrompt(String sessionId, String query) {
        if (!StringUtils.hasText(sessionId)) {
            return;
        }
        try {
            String url = properties.getBaseUrl() + "/api/session/" + sessionId + "/prompt";
            String body = "{\"prompt\":{\"text\":\"" + escapeJson(query) + "\"}}";
            HttpHeaders headers = buildHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            log.info("opencode 消息已发送: sessionId={}, status={}", sessionId, resp.getStatusCodeValue());
        } catch (Exception e) {
            log.error("opencode 发送消息异常: sessionId={}", sessionId, e);
        }
    }

    /**
     * 轮询消息，直到出现晚于 prompt 发送时刻的 assistant 文本回复
     */
    private String pollAnswer(String sessionId, long promptSentAt) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }
        long deadline = System.currentTimeMillis() + properties.getTimeoutSeconds() * 1000L;
        long pollInterval = 2000;
        try {
            while (System.currentTimeMillis() < deadline) {
                String answer = fetchLatestAssistantAnswer(sessionId, promptSentAt);
                if (answer != null) {
                    return answer;
                }
                Thread.sleep(pollInterval);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    private String fetchLatestAssistantAnswer(String sessionId, long promptSentAt) {
        try {
            String url = properties.getBaseUrl() + "/api/session/" + sessionId + "/message";
            HttpEntity<String> entity = new HttpEntity<>(buildHeaders());
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                return null;
            }
            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode data = root.isArray() ? root : root.path("data");

            String fallbackText = null;
            long fallbackCreated = -1;
            String lastText = null;
            long lastCreated = -1;
            if (data.isArray()) {
                for (JsonNode msg : data) {
                    if (!"assistant".equals(msg.path("type").asText())) {
                        continue;
                    }
                    long created = msg.path("time").path("created").asLong(0);
                    JsonNode content = msg.path("content");
                    String text = null;
                    if (content.isArray()) {
                        StringBuilder sb = new StringBuilder();
                        for (JsonNode part : content) {
                            if ("text".equals(part.path("type").asText())) {
                                String t = part.path("text").asText("");
                                if (StringUtils.hasText(t)) {
                                    sb.append(t);
                                }
                            }
                        }
                        if (sb.length() > 0) {
                            text = sb.toString();
                        }
                    }
                    if (text == null) {
                        continue;
                    }
                    // 记录所有含文本的 assistant 消息中最新的一条作为兜底
                    if (created > fallbackCreated) {
                        fallbackCreated = created;
                        fallbackText = text;
                    }
                    // 优先返回晚于 prompt 发送时刻的回复
                    if (created > promptSentAt && created > lastCreated) {
                        lastCreated = created;
                        lastText = text;
                    }
                }
            }
            if (lastText != null) {
                return lastText;
            }
            return fallbackText;
        } catch (Exception e) {
            log.warn("opencode 轮询消息异常: sessionId={}", sessionId, e);
            return null;
        }
    }

    private void saveConversation(String conversationId, String userId, String channel, String query, String answer) {
        try {
            kbConversationService.saveConversationWithCostAndApp(
                    java.util.UUID.randomUUID().toString(),
                    userId,
                    channel,
                    conversationId,
                    query,
                    answer,
                    -1L,
                    "本地opencode",
                    null, null, null, null, null,
                    0,
                    true,
                    null
            );
        } catch (Exception e) {
            log.warn("保存 opencode 会话记录失败: conversationId={}", conversationId, e);
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.hasText(properties.getPassword())) {
            String credentials = properties.getUsername() + ":" + properties.getPassword();
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        }
        return headers;
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
