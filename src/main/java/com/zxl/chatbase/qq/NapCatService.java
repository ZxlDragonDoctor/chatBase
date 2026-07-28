package com.zxl.chatbase.qq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NapCatService {

    private final QqBotProperties qqBotProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    private String buildUrl(String path) {
        String base = qqBotProperties.getWebuiBaseUrl();
        String token = qqBotProperties.getWebuiToken();
        String url = base + "/api/QQLogin" + path;
        if (token != null && !token.isEmpty()) {
            url += (url.contains("?") ? "&" : "?") + "token=" + token;
        }
        return url;
    }

    private Map<String, Object> postToNapCat(String path) {
        return postToNapCat(path, Collections.emptyMap());
    }

    private Map<String, Object> postToNapCat(String path, Map<String, Object> body) {
        try {
            String url = buildUrl(path);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String respBody = resp.getBody();
            if (respBody == null) return null;
            JsonNode root = objectMapper.readTree(respBody);
            int retCode = root.path("retcode").asInt(-1);
            if (retCode != 0) {
                String msg = root.path("msg").asText("NapCat API error");
                log.warn("NapCat API 返回错误: path={}, retcode={}, msg={}", path, retCode, msg);
                Map<String, Object> err = new HashMap<>();
                err.put("error", msg);
                return err;
            }
            JsonNode dataNode = root.path("data");
            if (dataNode.isMissingNode() || dataNode.isNull()) {
                return new HashMap<>();
            }
            return objectMapper.convertValue(dataNode, Map.class);
        } catch (Exception e) {
            log.error("NapCat API 请求异常: path={}", path, e);
            Map<String, Object> err = new HashMap<>();
            err.put("error", "NapCat 连接失败: " + e.getMessage());
            return err;
        }
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
            String base = qqBotProperties.getWebuiBaseUrl();
            String token = qqBotProperties.getWebuiToken();
            String url = base + "/api/QQLogin/GetQuickLoginList";
            if (token != null && !token.isEmpty()) {
                url += "?token=" + token;
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>("{}", headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Object> getQrCodeImage() {
        try {
            Map<String, Object> qrResult = getQrCode();
            if (qrResult == null || qrResult.containsKey("error")) return qrResult;
            String qrcodeUrl = (String) qrResult.get("qrcode_url");
            if (qrcodeUrl == null) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "二维码 URL 为空");
                return err;
            }
            ResponseEntity<byte[]> imageResp = restTemplate.exchange(
                    qrcodeUrl, HttpMethod.GET, null, byte[].class);
            byte[] imageData = imageResp.getBody();
            if (imageData == null || imageData.length == 0) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "下载二维码图片失败");
                return err;
            }
            String base64 = java.util.Base64.getEncoder().encodeToString(imageData);
            Map<String, Object> data = new HashMap<>();
            data.put("qrcode_img", "data:image/png;base64," + base64);
            return data;
        } catch (Exception e) {
            log.error("获取 QQ 二维码图片失败", e);
            Map<String, Object> err = new HashMap<>();
            err.put("error", "获取二维码图片失败: " + e.getMessage());
            return err;
        }
    }
}
