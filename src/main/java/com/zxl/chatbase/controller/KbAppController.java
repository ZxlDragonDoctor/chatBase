package com.zxl.chatbase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zxl.chatbase.kb.entity.KbApp;
import com.zxl.chatbase.kb.service.IKbAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/kb/app")
@RequiredArgsConstructor
public class KbAppController {

    private final IKbAppService appService;

    @GetMapping("/list")
    public List<KbApp> list(@RequestAttribute(value = "currentUser", required = false) String userId) {
        return appService.listAll(userId);
    }

    @GetMapping("/page")
    public Page<KbApp> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestAttribute(value = "currentUser", required = false) String userId) {
        return appService.page(pageNum, pageSize, userId, name);
    }

    @GetMapping("/{id}")
    public KbApp getById(@PathVariable Long id) {
        return appService.getById(id);
    }

    @GetMapping("/default")
    public KbApp getDefault() {
        return appService.getDefaultApp();
    }

    @PostMapping
    public KbApp create(@RequestBody KbApp app,
                        @RequestAttribute(value = "currentUser", required = false) String userId) {
        return appService.create(app, userId);
    }

    @PutMapping
    public KbApp update(@RequestBody KbApp app,
                        @RequestAttribute(value = "currentUser", required = false) String userId) {
        return appService.update(app, userId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id,
                       @RequestAttribute(value = "currentUser", required = false) String userId) {
        appService.delete(id, userId);
    }

    @PostMapping("/verify")
    public KbApp verifyApiKey(@RequestBody Map<String, String> params) {
        String apiKey = params.get("apiKey");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("API Key不能为空");
        }
        return appService.verifyApiKey(apiKey);
    }

    @GetMapping("/{id}/info")
    public KbApp getAppInfo(@PathVariable Long id) {
        return appService.getAppInfo(id);
    }

    @PutMapping("/{id}/default")
    public void setDefault(@PathVariable Long id,
                           @RequestAttribute(value = "currentUser", required = false) String userId) {
        appService.setDefault(id, userId);
    }

    @GetMapping("/{id}/access")
    public boolean canAccess(@PathVariable Long id,
                             @RequestAttribute(value = "currentUser", required = false) String userId) {
        return appService.canUserAccess(id, userId);
    }

    @GetMapping("/{id}/groups")
    public List<?> getBoundGroups(@PathVariable Long id) {
        return appService.getBoundGroups(id);
    }
}