package com.zxl.chatbase.command.handler;

import com.zxl.chatbase.command.CommandHandler;
import com.zxl.chatbase.opencode.OpencodeService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class SessionCommand implements CommandHandler {

    @Resource
    private OpencodeService opencodeService;

    @Override
    public String name() { return "session"; }

    @Override
    public String[] aliases() { return new String[]{"会话", "切换会话"}; }

    @Override
    public String description() { return "查看/切换opencode会话（/session list 或 /session <ID>）"; }

    @Override
    public String execute(String args, String channel, String userId, String conversationId) {
        if (!opencodeService.isEnabled()) {
            return "【本地opencode未启用】";
        }

        String trimmed = args == null ? "" : args.trim();

        // /session 或 /session list —— 列出会话
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("list") || trimmed.equalsIgnoreCase("列表")) {
            return opencodeService.listSessions(conversationId);
        }

        // /session <id> —— 切换会话
        return opencodeService.switchSession(conversationId, trimmed);
    }
}
