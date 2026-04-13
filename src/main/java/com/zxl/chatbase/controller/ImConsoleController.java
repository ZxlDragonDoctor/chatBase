package com.zxl.chatbase.controller;

import com.zxl.chatbase.im.dto.ConsoleOverviewVO;
import com.zxl.chatbase.im.dto.GroupMessagePageVO;
import com.zxl.chatbase.im.dto.GroupSummaryVO;
import com.zxl.chatbase.im.service.ImConsoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Web 控制台：QQ / 企微群采集概览、消息分页（只读）
 */
@RestController
@RequestMapping("/api/console")
@RequiredArgsConstructor
public class ImConsoleController {

    private final ImConsoleService imConsoleService;

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
            @RequestParam(defaultValue = "30") int size) {
        return imConsoleService.pageMessages(platform, groupId, page, size);
    }
}
