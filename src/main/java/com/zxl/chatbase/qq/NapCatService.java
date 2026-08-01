package com.zxl.chatbase.qq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NapCatService {

    private static final long CREDENTIAL_TTL_MS = 10 * 60 * 1000L;

    private final QqBotProperties qqBotProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    private volatile String credential;
    private volatile long credentialExpireAt;

    /**
     * NapCat WebUI 认证（v4.17+）：
     * 1. POST /api/auth/login，body={hash: SHA256(webuiToken + ".napcat")}
     * 2. 返回 data.Credential，后续请求带 Authorization: Bearer <credential>
     */
    private synchronized String ensureCredential() {
        if (credential != null && System.currentTimeMillis() < credentialExpireAt) {
            return credential;
        }
        String token = qqBotProperties.getWebuiToken();
        if (token == null || token.isEmpty()) {
            log.warn("NapCat WebUI token 未配置，无法认证（请在配置中设置 webui-token）");
            return null;
        }
        try {
            String hash = sha256Hex(token + ".napcat");
            String url = qqBotProperties.getWebuiBaseUrl() + "/api/auth/login";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("hash", hash);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String respBody = resp.getBody();
            if (respBody == null) {
                return null;
            }
            JsonNode root = objectMapper.readTree(respBody);
            int code = root.path("code").asInt(-1);
            if (code != 0) {
                log.warn("NapCat WebUI 登录失败: code={}, message={}", code, root.path("message").asText());
                return null;
            }
            credential = root.path("data").path("Credential").asText(null);
            credentialExpireAt = System.currentTimeMillis() + CREDENTIAL_TTL_MS;
            return credential;
        } catch (Exception e) {
            log.error("NapCat WebUI 登录异常", e);
            return null;
        }
    }

    private static String sha256Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private Map<String, Object> postToNapCat(String path) {
        return postToNapCat(path, Collections.emptyMap());
    }

    private Map<String, Object> postToNapCat(String path, Map<String, Object> body) {
        try {
            String url = qqBotProperties.getWebuiBaseUrl() + "/api/QQLogin" + path;
            Map<String, Object> result = doPost(url, body);
            if (result != null && "Unauthorized".equals(result.get("error"))) {
                log.info("NapCat 凭证失效，重新登录后重试: path={}", path);
                credential = null;
                credentialExpireAt = 0;
                result = doPost(url, body);
            }
            return result;
        } catch (Exception e) {
            log.error("NapCat API 请求异常: path={}", path, e);
            Map<String, Object> err = new HashMap<>();
            err.put("error", "NapCat 连接失败: " + e.getMessage());
            return err;
        }
    }

    private Map<String, Object> doPost(String url, Map<String, Object> body) throws Exception {
        String cred = ensureCredential();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (cred != null) {
            headers.set("Authorization", "Bearer " + cred);
        }
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        String respBody = resp.getBody();
        if (respBody == null) {
            return null;
        }
        JsonNode root = objectMapper.readTree(respBody);
        int code = root.path("code").asInt(-1);
        if (code != 0) {
            String msg = root.path("message").asText("NapCat API error");
            log.warn("NapCat API 返回错误: url={}, code={}, msg={}", url, code, msg);
            Map<String, Object> err = new HashMap<>();
            err.put("error", msg);
            return err;
        }
        JsonNode dataNode = root.path("data");
        if (dataNode.isMissingNode() || dataNode.isNull()) {
            return new HashMap<>();
        }
        return objectMapper.convertValue(dataNode, Map.class);
    }

    public Map<String, Object> getQrCode() {
        Map<String, Object> result = postToNapCat("/GetQQLoginQrcode");
        if (result == null || result.containsKey("error")) return result;
        String qrcodeUrl = (String) result.get("qrcode");
        if (qrcodeUrl == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "获取 QQ 二维码失败");
            return err;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("qrcode_url", qrcodeUrl);
        return data;
    }

    public Map<String, Object> checkLoginStatus() {
        Map<String, Object> result = postToNapCat("/CheckLoginStatus");
        if (result == null || result.containsKey("error")) return result;
        Map<String, Object> data = new HashMap<>();
        data.put("isLogin", result.getOrDefault("isLogin", false));
        data.put("isOffline", result.getOrDefault("isOffline", false));
        data.put("loginError", result.get("loginError"));
        String qrcodeurl = (String) result.get("qrcodeurl");
        if (qrcodeurl != null) data.put("qrcode_url", qrcodeurl);
        return data;
    }

    public Map<String, Object> refreshQrCode() {
        return postToNapCat("/RefreshQRcode");
    }

    public Map<String, Object> getLoginInfo() {
        Map<String, Object> result = postToNapCat("/GetQQLoginInfo");
        if (result == null || result.containsKey("error")) return result;
        Map<String, Object> data = new HashMap<>();
        if (result.get("uin") != null) data.put("uin", result.get("uin"));
        if (result.get("nick") != null) data.put("nick", result.get("nick"));
        if (result.get("online") != null) data.put("online", result.get("online"));
        if (result.get("avatarUrl") != null) data.put("avatarUrl", result.get("avatarUrl"));
        return data;
    }

    public Map<String, Object> getQuickLoginList() {
        return postToNapCat("/GetQuickLoginList");
    }

    public boolean isAvailable() {
        try {
            Map<String, Object> result = postToNapCat("/GetQuickLoginList");
            return result != null && !result.containsKey("error");
        } catch (Exception e) {
            return false;
        }
    }
}
