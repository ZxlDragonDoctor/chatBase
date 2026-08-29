package com.zxl.chatbase.command.handler;

import com.zxl.chatbase.command.CommandHandler;
import com.zxl.chatbase.opencode.OpencodeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class NewCommand implements CommandHandler {

    private static final String OPCODE_SESSION_PREFIX = "opencode:session:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String name() { return "new"; }

    @Override
    public String[] aliases() { return new String[]{"重置"}; }

    @Override
    public String description() { return "重置会话，开启全新对话上下文"; }

    @Override
    public String execute(String args, String channel, String userId, String conversationId) {
        // Clear opencode session if bound
        stringRedisTemplate.delete(OPCODE_SESSION_PREFIX + conversationId);
        return "✅ 已重置会话，下一次消息将开启全新对话上下文。";
    }
}
