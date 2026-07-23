package com.zxl.chatbase.wx.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxl.chatbase.wx.model.WxInboundMessage;
import com.zxl.chatbase.wx.model.WxMediaInfo;
import com.zxl.chatbase.wx.model.WxOutboundMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class WxIlinkUtil {

    private final ObjectMapper objectMapper;
    private final RestTemplate pollingRestTemplate;
    private final RestTemplate downloadRestTemplate;

    public WxIlinkUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.pollingRestTemplate = createRestTemplate(10_000, 45_000);
        this.downloadRestTemplate = createRestTemplate(10_000, 30_000);
    }

    private static RestTemplate createRestTemplate(int connectTimeout, int readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }

    public HttpHeaders buildHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("AuthorizationType", "ilink_bot_token");
        headers.set("Authorization", "Bearer " + token);
        headers.set("X-WECHAT-UIN", generateWechatUin());
        return headers;
    }

    private static String generateWechatUin() {
        long random = new Random().nextInt() & 0xFFFFFFFFL;
        return Base64.getEncoder().encodeToString(String.valueOf(random).getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    private <T> T parseJsonResponse(String json, Class<T> valueType) {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (Exception e) {
            log.error("JSON 解析失败: {}", json, e);
            return null;
        }
    }

    public List<WxInboundMessage> getUpdates(String baseUrl, String token, String getUpdatesBuf) {
        String url = baseUrl + "/ilink/bot/getupdates";
        HttpHeaders headers = buildHeaders(token);

        Map<String, Object> body = new HashMap<>();
        if (getUpdatesBuf != null && !getUpdatesBuf.isEmpty()) {
            body.put("get_updates_buf", getUpdatesBuf);
        }

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = pollingRestTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, String.class);
            String responseBody = response.getBody();
            if (responseBody == null || responseBody.trim().isEmpty() || "{}".equals(responseBody.trim())) {
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(responseBody);
            int ret = root.path("ret").asInt(0);
            if (ret != 0) {
                String errMsg = root.path("err_msg").asText();
                log.warn("getUpdates 返回错误: ret={}, err_msg={}", ret, errMsg);
                return Collections.emptyList();
            }

            JsonNode msgsNode = root.path("msgs");
            if (msgsNode.isMissingNode() || !msgsNode.isArray()) {
                return Collections.emptyList();
            }

            List<WxInboundMessage> messages = new ArrayList<>();
            for (JsonNode msgNode : msgsNode) {
                WxInboundMessage msg = objectMapper.treeToValue(msgNode, WxInboundMessage.class);
                if (msg != null && msg.getMsgId() != null) {
                    messages.add(msg);
                }
            }

            return messages;
        } catch (Exception e) {
            log.warn("getUpdates 请求异常: {}", e.getMessage());
            return null;
        }
    }

    public String getUpdatesBuf(String baseUrl, String token, String getUpdatesBuf) {
        String url = baseUrl + "/ilink/bot/getupdates";
        HttpHeaders headers = buildHeaders(token);

        Map<String, Object> body = new HashMap<>();
        if (getUpdatesBuf != null && !getUpdatesBuf.isEmpty()) {
            body.put("get_updates_buf", getUpdatesBuf);
        }

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = pollingRestTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, String.class);
            String responseBody = response.getBody();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                return getUpdatesBuf;
            }

            JsonNode root = objectMapper.readTree(responseBody);
            int ret = root.path("ret").asInt(0);
            if (ret != 0) {
                return getUpdatesBuf;
            }

            return root.path("get_updates_buf").asText(getUpdatesBuf);
        } catch (Exception e) {
            log.warn("getUpdatesBuf 获取失败: {}", e.getMessage());
            return getUpdatesBuf;
        }
    }

    public int sendMessage(String baseUrl, String token, WxOutboundMessage message) {
        String url = baseUrl + "/ilink/bot/sendmessage";
        HttpHeaders headers = buildHeaders(token);

        try {
            String jsonBody = objectMapper.writeValueAsString(message);
            log.debug("发送消息: {}", jsonBody);

            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<String> response = pollingRestTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, String.class);
            String responseBody = response.getBody();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                return 0;
            }

            JsonNode root = objectMapper.readTree(responseBody);
            int ret = root.path("ret").asInt(0);
            if (ret != 0) {
                String errMsg = root.path("err_msg").asText();
                log.warn("sendMessage 返回错误: ret={}, err_msg={}", ret, errMsg);
            }
            return ret;
        } catch (Exception e) {
            log.error("sendMessage 请求异常", e);
            return -1;
        }
    }

    public WxMediaInfo getUploadUrl(String baseUrl, String token, String msgId, String mediaKey) {
        String url = baseUrl + "/ilink/bot/getuploadurl";
        HttpHeaders headers = buildHeaders(token);

        Map<String, Object> body = new HashMap<>();
        body.put("msg_id", msgId);
        if (mediaKey != null) {
            body.put("media_key", mediaKey);
        }

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = pollingRestTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, String.class);
            String responseBody = response.getBody();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                return null;
            }

            JsonNode root = objectMapper.readTree(responseBody);
            int ret = root.path("ret").asInt(0);
            if (ret != 0) {
                log.warn("getUploadUrl 返回错误: ret={}", ret);
                return null;
            }

            return objectMapper.treeToValue(root, WxMediaInfo.class);
        } catch (Exception e) {
            log.error("getUploadUrl 请求异常", e);
            return null;
        }
    }

    public byte[] downloadFromCdn(String cdnUrl) {
        try {
            ResponseEntity<byte[]> response = downloadRestTemplate.exchange(
                    URI.create(cdnUrl), HttpMethod.GET, null, byte[].class);
            return response.getBody();
        } catch (Exception e) {
            log.error("CDN 文件下载失败: {}", cdnUrl, e);
            return null;
        }
    }
}
