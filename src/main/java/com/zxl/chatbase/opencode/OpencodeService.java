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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    /** 思考内容单条最大长度（字符） */
    private static final int MAX_REASONING_LEN = 600;
    /** 工具输出单条最大长度（字符） */
    private static final int MAX_TOOL_OUTPUT_LEN = 500;
    /** 返回给 IM 的完整回复总长度上限（字符），超出按头尾截断 */
    private static final int MAX_TOTAL_LEN = 6000;

    /**
     * 每个会话的串行锁：同一 conversationId 的消息必须按顺序处理，
     * 避免并发向同一 opencode session 发多个 prompt 导致回答错乱/丢失
     */
    private final ConcurrentHashMap<String, Object> conversationLocks = new ConcurrentHashMap<>();

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

        Object lock = conversationLocks.computeIfAbsent(conversationId, k -> new Object());
        synchronized (lock) {
            return doChat(conversationId, query, userId, channel);
        }
    }

    private String doChat(String conversationId, String query, String userId, String channel) {
        String sessionId = getOrCreateSession(conversationId);
        if (!StringUtils.hasText(sessionId)) {
            return "【opencode未返回结果】可能是执行超时或出错，请检查本地 opencode 状态。";
        }
        String baselineId = getLatestMessageId(sessionId);
        sendPrompt(sessionId, query);
        String answer = pollAnswer(sessionId, baselineId);
        if (!StringUtils.hasText(answer)) {
            answer = "【opencode 已执行完任务，但未生成文本回复】可能是任务过复杂或模型没有输出总结，请换个问法再试，或稍后重发。";
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
     * 轮询消息，直到 agent 完成（出现 finish=stop）或超时。
     *
     * 每次轮询都取本次 prompt 之后新增的所有 assistant 内容（思考/工具/中间文本/最终回复）
     * 拼接后作为当前累计回复。只要 agent 未完成就继续轮询，避免中间文本触发提前返回
     * 而导致最终完整回复丢失。
     *
     * 若 agent 已完成但全程无任何文本内容，则返回 null，不空等到超时。
     *
     * @param baselineId 发送 prompt 前最新一条消息的 id，用于区分本次 prompt 前后的消息
     */
    private String pollAnswer(String sessionId, String baselineId) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }
        long deadline = System.currentTimeMillis() + properties.getTimeoutSeconds() * 1000L;
        long pollInterval = 2000;
        String accumulated = null;
        try {
            while (System.currentTimeMillis() < deadline) {
                PollResult result = fetchLatestAssistantAnswer(sessionId, baselineId);
                if (StringUtils.hasText(result.answer)) {
                    accumulated = result.answer;
                }
                if (result.finished) {
                    log.info("opencode agent 已完成，结束轮询: sessionId={}, hasAnswer={}",
                            sessionId, StringUtils.hasText(accumulated));
                    return capTotal(accumulated, MAX_TOTAL_LEN);
                }
                Thread.sleep(pollInterval);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (StringUtils.hasText(accumulated)) {
            log.info("opencode 轮询达到超时，返回已累计内容: sessionId={}", sessionId);
            return capTotal(accumulated, MAX_TOTAL_LEN);
        }
        return null;
    }

    /**
     * 拉取会话消息，返回本次 prompt 之后新增的 assistant 完整内容（思考/工具/文本）。
     *
     * opencode 的 message 数组按时间倒序排列（最新消息在 index 0，最旧在尾部），
     * 且数组有上限（约 50 条，超出后旧消息会被挤掉），因此不能依赖数组长度差判断新增消息。
     * 改为记录发送 prompt 前最新一条消息的 id 作为基线（数组 index 0），轮询时从头部
     * 一直遍历到基线 id 为止，中间出现的 assistant 内容即为本次回复，避免串味/重复历史。
     */
    private PollResult fetchLatestAssistantAnswer(String sessionId, String baselineId) {
        try {
            String url = properties.getBaseUrl() + "/api/session/" + sessionId + "/message";
            HttpEntity<String> entity = new HttpEntity<>(buildHeaders());
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                return new PollResult(null, false);
            }
            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode data = root.isArray() ? root : root.path("data");

            String lastText = null;
            boolean finished = false;
            if (data.isArray()) {
                // 数组按时间倒序排列（最新消息在 index 0，最旧在数组尾部）。
                // 从头部（最新）开始收集，遇到基线 id（本次 prompt 前的最新消息）即停止，
                // 只收集比基线新的消息；收集到的顺序为倒序，最后反转成时间正序再拼接。
                List<String> collected = new ArrayList<>();
                for (int i = 0; i < data.size(); i++) {
                    JsonNode msg = data.get(i);
                    if (StringUtils.hasText(baselineId)
                            && baselineId.equals(msg.path("id").asText())) {
                        break;
                    }
                    if (!"assistant".equals(msg.path("type").asText())) {
                        continue;
                    }
                    if ("stop".equals(msg.path("finish").asText())) {
                        finished = true;
                    }
                    JsonNode content = msg.path("content");
                    StringBuilder sb = new StringBuilder();
                    if (content.isArray()) {
                        for (JsonNode part : content) {
                            String partType = part.path("type").asText();
                            if ("text".equals(partType) || "reasoning".equals(partType)) {
                                String t = part.path("text").asText("");
                                if (StringUtils.hasText(t)) {
                                    if (sb.length() > 0) {
                                        sb.append("\n\n");
                                    }
                                    if ("reasoning".equals(partType)) {
                                        sb.append("【思考】").append(truncate(t, MAX_REASONING_LEN));
                                    } else {
                                        sb.append(t);
                                    }
                                }
                            } else if ("tool".equals(partType)) {
                                String toolName = part.path("name").asText("");
                                String toolText = collectToolOutput(part);
                                if (sb.length() > 0) {
                                    sb.append("\n\n");
                                }
                                sb.append("【工具】").append(toolName);
                                if (StringUtils.hasText(toolText)) {
                                    sb.append("\n").append(truncate(toolText, MAX_TOOL_OUTPUT_LEN));
                                }
                            }
                        }
                    }
                    if (sb.length() > 0) {
                        collected.add(sb.toString());
                    }
                }
                // collected 为倒序（最新在前），反转成正序后拼接
                Collections.reverse(collected);
                if (!collected.isEmpty()) {
                    lastText = String.join("\n\n", collected);
                }
            }
            return new PollResult(lastText, finished);
        } catch (Exception e) {
            log.warn("opencode 轮询消息异常: sessionId={}", sessionId, e);
            return new PollResult(null, false);
        }
    }

    /**
     * 收集 tool 调用结果中的文本输出
     */
    private String collectToolOutput(JsonNode part) {
        try {
            JsonNode state = part.path("state");
            JsonNode content = state.path("content");
            StringBuilder sb = new StringBuilder();
            if (content.isArray()) {
                for (JsonNode item : content) {
                    if ("text".equals(item.path("type").asText())) {
                        String t = item.path("text").asText("");
                        if (StringUtils.hasText(t)) {
                            if (sb.length() > 0) {
                                sb.append("\n");
                            }
                            sb.append(t);
                        }
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "\n…(已截断)";
    }

    /**
     * 超出总长度上限时，保留头部（思考/过程）与尾部（最终回复）各半，
     * 中间用省略标记衔接，确保用户能看到思考流程的同时收到最终答案。
     */
    private static String capTotal(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) {
            return text;
        }
        int half = maxLen / 2;
        return text.substring(0, half)
                + "\n\n…(中间内容过多已省略，共 "
                + text.length() + " 字)…\n\n"
                + text.substring(text.length() - half);
    }

    /**
     * 轮询结果：answer 为回复文本，finished 表示 agent 已完成（finish=stop）
     */
    private static class PollResult {
        private final String answer;
        private final boolean finished;

        PollResult(String answer, boolean finished) {
            this.answer = answer;
            this.finished = finished;
        }
    }

    /**
     * 获取当前会话最新一条消息的 id（用于建立消息 id 基线）
     */
    private String getLatestMessageId(String sessionId) {
        try {
            String url = properties.getBaseUrl() + "/api/session/" + sessionId + "/message";
            HttpEntity<String> entity = new HttpEntity<>(buildHeaders());
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                return null;
            }
            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode data = root.isArray() ? root : root.path("data");
            if (data.isArray() && data.size() > 0) {
                return data.get(0).path("id").asText(null);
            }
            return null;
        } catch (Exception e) {
            log.warn("opencode 获取最新消息id异常: sessionId={}", sessionId, e);
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
