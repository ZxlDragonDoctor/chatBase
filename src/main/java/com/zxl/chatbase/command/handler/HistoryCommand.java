package com.zxl.chatbase.command.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zxl.chatbase.command.CommandHandler;
import com.zxl.chatbase.kb.entity.KbConversation;
import com.zxl.chatbase.kb.service.IKbConversationService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
public class HistoryCommand implements CommandHandler {

    @Resource
    private IKbConversationService kbConversationService;

    @Override
    public String name() { return "history"; }

    @Override
    public String[] aliases() { return new String[]{"历史"}; }

    @Override
    public String description() { return "查看最近对话记录 (默认5条)"; }

    @Override
    public String execute(String args, String channel, String userId, String conversationId) {
        int limit = 5;
        if (args != null && !args.isEmpty()) {
            try {
                limit = Math.min(Math.max(Integer.parseInt(args.trim()), 1), 20);
            } catch (NumberFormatException ignored) {}
        }

        LambdaQueryWrapper<KbConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbConversation::getUserId, userId)
                .eq(KbConversation::getChannel, channel)
                .orderByDesc(KbConversation::getCreateTime)
                .last("LIMIT " + limit);

        List<KbConversation> conversations = kbConversationService.list(wrapper);

        if (conversations.isEmpty()) {
            return "📭 暂无对话记录";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📜 最近 ").append(conversations.size()).append(" 条对话:\n\n");
        int idx = 1;
        for (KbConversation c : conversations) {
            String query = c.getQuery();
            if (query != null && query.length() > 50) {
                query = query.substring(0, 50) + "...";
            }
            sb.append(idx++).append(". ").append(query != null ? query : "(空)").append("\n");
        }
        return sb.toString().trim();
    }
}
