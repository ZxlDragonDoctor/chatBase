package com.zxl.chatbase.controller;

import com.zxl.chatbase.qq.NapCatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/qq-bot")
@RequiredArgsConstructor
public class QqBotController {

    private final NapCatService napCatService;

    @GetMapping("/qrcode")
    public Map<String, Object> getQrCode() {
        return napCatService.getQrCode();
    }

    @GetMapping("/qrcode/status")
    public Map<String, Object> getQrCodeStatus() {
        return napCatService.checkLoginStatus();
    }

    @PostMapping("/qrcode/refresh")
    public Map<String, Object> refreshQrCode() {
        return napCatService.refreshQrCode();
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> info = napCatService.getLoginInfo();
        if (info == null || info.containsKey("error")) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("online", false);
            return fallback;
        }
        Map<String, Object> status = new HashMap<>();
        status.put("online", info.getOrDefault("online", false));
        status.put("uin", info.get("uin"));
        status.put("nick", info.get("nick"));
        return status;
    }

    @GetMapping("/available")
    public Map<String, Object> isAvailable() {
        Map<String, Object> result = new HashMap<>();
        result.put("available", napCatService.isAvailable());
        return result;
    }
}
