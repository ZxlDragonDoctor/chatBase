package com.zxl.chatbase.opencode.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class OpencodeNodeCommandMapper {
    private static final Pattern SESSION = Pattern.compile("^/api/session/(?!status$)([^/]+)$");
    private static final Pattern SESSION_PROMPT = Pattern.compile("^/api/session/([^/]+)/prompt$");
    private static final Pattern SESSION_MESSAGES = Pattern.compile("^/api/session/([^/]+)/message$");
    private static final Pattern SESSION_QUESTIONS = Pattern.compile("^/api/session/([^/]+)/question$");
    private static final Pattern QUESTION_REPLY = Pattern.compile("^/api/session/([^/]+)/question/([^/]+)/reply$");

    private final ObjectMapper objectMapper;

    public MappedCommand map(HttpMethod method, String path, String body) {
        if (method == null || !StringUtils.hasText(path) || path.contains("?") || path.contains("#") || path.contains("\\")) {
            throw new IllegalArgumentException("不支持的 OpenCode 节点请求");
        }
        JsonNode params = parseBody(body);
        if (method == HttpMethod.GET && "/global/health".equals(path)) {
            return new MappedCommand(OpencodeNodeCommand.HEALTH.value(), objectMapper.createObjectNode());
        }
        if (method == HttpMethod.GET && "/api/session".equals(path)) {
            return new MappedCommand(OpencodeNodeCommand.SESSION_LIST.value(), objectMapper.createObjectNode());
        }
        if (method == HttpMethod.POST && "/api/session".equals(path)) {
            return new MappedCommand(OpencodeNodeCommand.SESSION_CREATE.value(), params);
        }
        if (method == HttpMethod.GET) {
            MappedCommand match = pathMatch(path, SESSION, OpencodeNodeCommand.SESSION_GET, "sessionId", params);
            if (match != null) return match;
            match = pathMatch(path, SESSION_MESSAGES, OpencodeNodeCommand.MESSAGE_LIST, "sessionId", params);
            if (match != null) return match;
            match = pathMatch(path, SESSION_QUESTIONS, OpencodeNodeCommand.QUESTION_LIST, "sessionId", params);
            if (match != null) return match;
        }
        if (method == HttpMethod.POST) {
            Matcher prompt = SESSION_PROMPT.matcher(path);
            if (prompt.matches()) {
                params = params.deepCopy();
                ((com.fasterxml.jackson.databind.node.ObjectNode) params).put("sessionId", decode(prompt.group(1)));
                return new MappedCommand(OpencodeNodeCommand.PROMPT_SUBMIT.value(), params);
            }
            Matcher reply = QUESTION_REPLY.matcher(path);
            if (reply.matches()) {
                params = params.deepCopy();
                com.fasterxml.jackson.databind.node.ObjectNode object = (com.fasterxml.jackson.databind.node.ObjectNode) params;
                object.put("sessionId", decode(reply.group(1)));
                object.put("requestId", decode(reply.group(2)));
                return new MappedCommand(OpencodeNodeCommand.QUESTION_REPLY.value(), params);
            }
        }
        throw new IllegalArgumentException("该 OpenCode API 未开放给节点通道");
    }

    private MappedCommand pathMatch(String path, Pattern pattern, OpencodeNodeCommand command, String field, JsonNode params) {
        Matcher matcher = pattern.matcher(path);
        if (!matcher.matches()) return null;
        com.fasterxml.jackson.databind.node.ObjectNode object = params.deepCopy().isObject()
                ? (com.fasterxml.jackson.databind.node.ObjectNode) params.deepCopy()
                : objectMapper.createObjectNode();
        object.put(field, decode(matcher.group(1)));
        return new MappedCommand(command.value(), object);
    }

    private JsonNode parseBody(String body) {
        if (!StringUtils.hasText(body)) return objectMapper.createObjectNode();
        try {
            JsonNode parsed = objectMapper.readTree(body);
            if (parsed == null || !parsed.isObject()) throw new IllegalArgumentException("OpenCode 请求正文必须是 JSON 对象");
            return parsed;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("OpenCode 请求正文不是有效 JSON", e);
        }
    }

    private String decode(String value) {
        String decoded = URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
        if (decoded.isBlank() || decoded.equals(".") || decoded.equals("..") || decoded.contains("/") || decoded.contains("\\")) {
            throw new IllegalArgumentException("OpenCode 会话标识无效");
        }
        return decoded;
    }

    public static final class MappedCommand {
        private final String command;
        private final JsonNode params;

        public MappedCommand(String command, JsonNode params) {
            this.command = command;
            this.params = params;
        }

        public String getCommand() { return command; }
        public JsonNode getParams() { return params; }
    }
}
