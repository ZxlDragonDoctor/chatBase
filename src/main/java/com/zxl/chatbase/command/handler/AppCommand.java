package com.zxl.chatbase.command.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zxl.chatbase.command.CommandHandler;
import com.zxl.chatbase.im.entity.ImGroup;
import com.zxl.chatbase.im.mapper.ImGroupMapper;
import com.zxl.chatbase.im.service.ImConversationService;
import com.zxl.chatbase.kb.entity.KbApp;
import com.zxl.chatbase.kb.mapper.KbAppMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class AppCommand implements CommandHandler {

    @Resource
    private ImConversationService imConversationService;
    @Resource
    private KbAppMapper kbAppMapper;

    @Override
    public String name() { return "app"; }

    @Override
    public String[] aliases() { return new String[]{"应用"}; }

    @Override
    public String description() { return "查看当前绑定的应用信息"; }

    @Override
    public String execute(String args, String channel, String userId, String conversationId) {
        // 先检查 opencode 绑定
        if (imConversationService.isOpencodeBound(conversationId)) {
            return "📱 当前绑定: 🖥️ 本地opencode（远程编码代理）\n会话: " + conversationId;
        }

        Long appId = imConversationService.getAppIdForConversation(conversationId);

        if (appId == null || appId <= 0) {
            // Try to get default app
            LambdaQueryWrapper<KbApp> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(KbApp::getStatus, true)
                    .eq(KbApp::getIsDefault, true)
                    .last("LIMIT 1");
            KbApp defaultApp = kbAppMapper.selectOne(wrapper);
            if (defaultApp != null) {
                return "📱 当前使用默认应用:\n" +
                        "名称: " + defaultApp.getName() + "\n" +
                        "ID: " + defaultApp.getId() + "\n" +
                        "状态: " + (defaultApp.getStatus() ? "启用" : "停用");
            }
            return "📱 当前未绑定任何应用";
        }

        KbApp app = kbAppMapper.selectById(appId);
        if (app == null) {
            return "📱 绑定的应用不存在 (ID: " + appId + ")";
        }

        return "📱 当前绑定应用:\n" +
                "名称: " + app.getName() + "\n" +
                "ID: " + app.getId() + "\n" +
                "状态: " + (app.getStatus() ? "启用" : "停用") +
                (app.getIsDefault() ? " (默认)" : "");
    }
}
