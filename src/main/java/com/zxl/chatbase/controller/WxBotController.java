package com.zxl.chatbase.controller;

import com.zxl.chatbase.wx.service.WxIlinkService;
import com.zxl.chatbase.wx.util.WxIlinkUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/wx/bot")
@RequiredArgsConstructor
public class WxBotController {

    private static final String ILINK_API_BASE = "https://ilinkai.weixin.qq.com";

    private final WxIlinkService wxIlinkService;
    private final WxIlinkUtil wxIlinkUtil;

    @GetMapping("/qrcode")
    public Map<String, Object> getQrCode() {
        Map<String, Object> result = wxIlinkUtil.getBotQrCode(ILINK_API_BASE);
        if (result == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "获取二维码失败");
            return err;
        }
        return result;
    }

    @GetMapping("/qrcode/status")
    public Map<String, Object> getQrCodeStatus(@RequestParam String qrcode) {
        Map<String, Object> result = wxIlinkUtil.getQrCodeStatus(ILINK_API_BASE, qrcode);
        if (result == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "查询扫码状态失败");
            return err;
        }
        String status = (String) result.get("status");
        if ("confirmed".equals(status)) {
            String baseUrl = (String) result.get("baseurl");
            String botToken = (String) result.get("bot_token");
            String nickname = (String) result.get("nickname");
            if (baseUrl != null && botToken != null) {
                if (baseUrl.isEmpty()) baseUrl = ILINK_API_BASE;
                wxIlinkService.login(baseUrl, botToken, nickname != null ? nickname : "微信机器人");
                log.info("微信 ilink 扫码登录成功: nickname={}", nickname);
            }
        }
        return result;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("online", wxIlinkService.isOnline());
        status.put("nickname", wxIlinkService.getNickname());
        return status;
    }

    @PostMapping("/disconnect")
    public Map<String, Object> disconnect() {
        wxIlinkService.logout();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }
}
