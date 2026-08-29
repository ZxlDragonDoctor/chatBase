package com.zxl.chatbase.command.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zxl.chatbase.command.CommandHandler;
import com.zxl.chatbase.kb.entity.KbConversation;
import com.zxl.chatbase.kb.service.IKbConversationService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
public class StatsCommand implements CommandHandler {

    @Resource
    private IKbConversationService kbConversationService;

    @Override
    public String name() { return "stats"; }

    @Override
    public String[] aliases() { return new String[]{"统计"}; }

    @Override
    public String description() { return "查看个人使用统计"; }

    @Override
    public String execute(String args, String channel, String userId, String conversationId) {
        // Total conversations
        LambdaQueryWrapper<KbConversation> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(KbConversation::getUserId, userId)
                .eq(KbConversation::getChannel, channel);
        long totalConversations = kbConversationService.count(totalWrapper);

        // Today's conversations
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        LambdaQueryWrapper<KbConversation> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(KbConversation::getUserId, userId)
                .eq(KbConversation::getChannel, channel)
                .between(KbConversation::getCreateTime, todayStart, todayEnd);
        long todayConversations = kbConversationService.count(todayWrapper);

        StringBuilder sb = new StringBuilder();
        sb.append("📊 使用统计\n\n");
        sb.append("渠道: ").append(channel).append("\n");
        sb.append("用户: ").append(userId).append("\n\n");
        sb.append("总对话数: ").append(totalConversations).append("\n");
        sb.append("今日对话数: ").append(todayConversations).append("\n");

        return sb.toString().trim();
    }
}
