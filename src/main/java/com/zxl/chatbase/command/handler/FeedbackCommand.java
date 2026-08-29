package com.zxl.chatbase.command.handler;

import com.zxl.chatbase.command.CommandHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FeedbackCommand implements CommandHandler {

    @Override
    public String name() { return "feedback"; }

    @Override
    public String[] aliases() { return new String[]{"反馈"}; }

    @Override
    public String description() { return "提交反馈意见 (格式: /feedback 内容)"; }

    @Override
    public String execute(String args, String channel, String userId, String conversationId) {
        if (args == null || args.trim().isEmpty()) {
            return "📝 请提供反馈内容\n格式: /feedback 您的意见或建议";
        }

        String content = args.trim();
        log.info("收到用户反馈: channel={}, userId={}, conversationId={}, content={}",
                channel, userId, conversationId, content);

        return "✅ 感谢您的反馈！我们会认真处理。\n内容: " + content;
    }
}
