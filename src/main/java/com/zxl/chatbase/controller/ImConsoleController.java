package com.zxl.chatbase.controller;

import com.zxl.chatbase.im.dto.ConsoleOverviewVO;
import com.zxl.chatbase.im.dto.GroupMessagePageVO;
import com.zxl.chatbase.im.dto.GroupSummaryVO;
import com.zxl.chatbase.im.service.ImConsoleService;
import com.zxl.chatbase.im.service.ImGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Web 控制台：QQ / 企微群采集概览、消息分页（只读）
 */
@RestController
@RequestMapping("/api/console")
@RequiredArgsConstructor
public class ImConsoleController {

    private final ImConsoleService imConsoleService;
    private final ImGroupService imGroupService;

    @GetMapping("/overview")
    public ConsoleOverviewVO overview() {
        return imConsoleService.overview();
    }

    /**
     * @param platform 可选：qq / wx / wecom / all
     */
    @GetMapping("/groups")
    public List<GroupSummaryVO> groups(@RequestParam(defaultValue = "all") String platform) {
        return imConsoleService.listGroups(platform);
    }

    @GetMapping("/messages")
    public GroupMessagePageVO messages(
            @RequestParam(required = false) String groupId,
            @RequestParam(defaultValue = "all") String platform,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(required = false) String keyword) {
        return imConsoleService.pageMessages(platform, groupId, page, size, keyword);
    }

    @PutMapping("/groups/{id}/app")
    public void bindApp(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long appId = body.get("appId") != null ? Long.valueOf(body.get("appId").toString()) : null;
        String appName = body.get("appName") != null ? body.get("appName").toString() : null;
        imGroupService.bindApp(id, appId, appName);
    }

    @DeleteMapping("/groups/{id}/app")
    public void unbindApp(@PathVariable Long id) {
        imGroupService.unbindApp(id);
    }
}
