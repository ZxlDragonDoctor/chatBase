package com.zxl.chatbase.controller;

import com.zxl.chatbase.im.dto.ConsoleOverviewVO;
import com.zxl.chatbase.im.dto.ConversationSummaryVO;
import com.zxl.chatbase.im.dto.GroupMessagePageVO;
import com.zxl.chatbase.im.dto.GroupSummaryVO;
import com.zxl.chatbase.im.service.ImConsoleService;
import com.zxl.chatbase.im.service.ImConversationService;
import com.zxl.chatbase.im.service.ImGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
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
    private final ImConversationService imConversationService;

    @GetMapping("/overview")
    public ConsoleOverviewVO overview(@RequestAttribute("currentUser") String userId) {
        return imConsoleService.overview(userId);
    }

    /**
     * @param platform 可选：qq / wx / wecom / all
     */
    @GetMapping("/groups")
    public List<GroupSummaryVO> groups(
            @RequestParam(defaultValue = "all") String platform,
            @RequestParam(required = false, defaultValue = "all") String scope,
            @RequestAttribute("currentUser") String userId) {
        return imConsoleService.listGroups(platform, userId, scope);
    }

    @GetMapping("/messages")
    public GroupMessagePageVO messages(
            @RequestParam(required = false) String groupId,
            @RequestParam(defaultValue = "all") String platform,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(required = false) String keyword,
            @RequestAttribute("currentUser") String userId) {
        return imConsoleService.pageMessages(platform, groupId, page, size, keyword, userId);
    }

    @GetMapping("/conversations")
    public List<ConversationSummaryVO> conversations(@RequestAttribute("currentUser") String userId) {
        return imConsoleService.listConversations(userId);
    }

    @GetMapping("/conversations/messages")
    public GroupMessagePageVO privateMessages(
            @RequestParam String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(required = false) String keyword,
            @RequestAttribute("currentUser") String userId) {
        return imConsoleService.pagePrivateMessages(conversationId, page, size, keyword, userId);
    }

    @PutMapping("/conversations/{id}/app")
    public void bindConversationApp(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @RequestAttribute("currentUser") String userId) {
        Long appId = body.get("appId") != null ? Long.valueOf(body.get("appId").toString()) : null;
        String appName = body.get("appName") != null ? body.get("appName").toString() : null;
        imConversationService.bindApp(id, appId, appName, userId);
    }

    @DeleteMapping("/conversations/{id}/app")
    public void unbindConversationApp(
            @PathVariable Long id,
            @RequestAttribute("currentUser") String userId) {
        imConversationService.unbindApp(id, userId);
    }

    @PutMapping("/groups/{id}/app")
    public void bindApp(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @RequestAttribute("currentUser") String userId) {
        Long appId = body.get("appId") != null ? Long.valueOf(body.get("appId").toString()) : null;
        String appName = body.get("appName") != null ? body.get("appName").toString() : null;
        imGroupService.bindApp(id, appId, appName, userId);
    }

    @DeleteMapping("/groups/{id}/app")
    public void unbindApp(
            @PathVariable Long id,
            @RequestAttribute("currentUser") String userId) {
        imGroupService.unbindApp(id, userId);
    }

    @PostMapping("/groups/{id}/assign")
    public void assignGroup(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @RequestAttribute("currentUser") String userId) {
        String targetUser = body.get("userId").toString();
        imGroupService.assignGroup(id, targetUser, userId);
    }
}
