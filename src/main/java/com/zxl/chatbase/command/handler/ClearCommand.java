package com.zxl.chatbase.command.handler;

import com.zxl.chatbase.command.CommandHandler;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class ClearCommand implements CommandHandler {

    private static final String OPCODE_SESSION_PREFIX = "opencode:session:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String name() { return "clear"; }

    @Override
    public String[] aliases() { return new String[]{"清空"}; }

    @Override
    public String description() { return "清空当前会话上下文"; }

    @Override
    public String execute(String args, String channel, String userId, String conversationId) {
        // Clear opencode session
        stringRedisTemplate.delete(OPCODE_SESSION_PREFIX + conversationId);

        // Clear any other session-related keys for this conversation
        // (could extend to clear Dify conversation ID if needed)

        return "🧹 会话上下文已清空，下次对话将重新开始。";
    }
}
