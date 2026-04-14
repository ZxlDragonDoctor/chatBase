package com.zxl.chatbase.controller;

import com.zxl.chatbase.wxroboot.webhook.service.IntelligentRobotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("intellrobot")
@Slf4j
public class IntelligentRobotController {
    //TODO 路由地址规范，需要修改前端api中的路由
    @Resource
    private IntelligentRobotService intelligentRobotService;

    @GetMapping("callback/handle")
    public String verifyUrl(@RequestParam("msg_signature") String msgSignature,
                            @RequestParam("timestamp") String timestamp,
                            @RequestParam("nonce") String nonce,
                            @RequestParam("echostr") String echoStr
    ) {
        return intelligentRobotService.verifyUrl(msgSignature, timestamp, nonce, echoStr);
    }

//Urldecode
    @PostMapping("/callback/handle") // 路劲与get检验URL相同
    public Map<String, String> handleMessage(@RequestParam("msg_signature") String msgSignature,
                                @RequestParam("timestamp") String timestamp,
                                @RequestParam("nonce") String nonce,
                                @RequestBody String postData
    ) {
        return Map.of("encrypt", intelligentRobotService.handleMessage(msgSignature, timestamp, nonce, postData));
    }
}
