package com.zxl.chatbase.controller;

import com.zxl.chatbase.im.dto.BotInfoVO;
import com.zxl.chatbase.im.service.BotManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bot")
@RequiredArgsConstructor
public class BotManageController {

    private final BotManageService botManageService;

    @GetMapping("/list")
    public List<BotInfoVO> listBots() {
        return botManageService.listBots();
    }
}
