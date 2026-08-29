package com.zxl.chatbase.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 机器人命令分发器
 * 负责注册所有命令处理器，解析 /xxx 前缀并路由到对应 handler
 */
@Slf4j
@Component
public class BotCommandDispatcher {

    @Resource
    private List<CommandHandler> handlers;

    private final Map<String, CommandHandler> commandMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (CommandHandler handler : handlers) {
            commandMap.put(handler.name().toLowerCase(), handler);
            for (String alias : handler.aliases()) {
                commandMap.put(alias.toLowerCase(), handler);
            }
            log.info("注册机器人命令: /{} (别名: {})", handler.name(), String.join(", ", handler.aliases()));
        }
        log.info("机器人命令注册完成，共 {} 个命令", commandMap.size());
    }

    /**
     * 判断文本是否为命令（以 / 开头）
     */
    public boolean isCommand(String text) {
        return text != null && text.trim().startsWith("/");
    }

    /**
     * 获取所有已注册的命令处理器（去重，按名称）
     */
    public List<CommandHandler> getRegisteredHandlers() {
        Map<String, CommandHandler> unique = new HashMap<>();
        for (CommandHandler h : commandMap.values()) {
            unique.putIfAbsent(h.name().toLowerCase(), h);
        }
        return new ArrayList<>(unique.values());
    }

    /**
     * 执行命令，返回回复文本；如果不是命令则返回 null
     */
    public String dispatch(String text, String channel, String userId, String conversationId) {
        if (!isCommand(text)) {
            return null;
        }
        String trimmed = text.trim();
        // 去掉 / 前缀，取第一个空格前的部分作为命令名
        String withoutSlash = trimmed.substring(1);
        String cmdName;
        String args;
        int spaceIdx = withoutSlash.indexOf(' ');
        if (spaceIdx >= 0) {
            cmdName = withoutSlash.substring(0, spaceIdx).trim();
            args = withoutSlash.substring(spaceIdx + 1).trim();
        } else {
            cmdName = withoutSlash.trim();
            args = "";
        }

        CommandHandler handler = commandMap.get(cmdName.toLowerCase());
        if (handler == null) {
            return "未知命令: /" + cmdName + "\n发送 /help 查看可用命令列表";
        }

        try {
            log.info("执行机器人命令: /{} args=[{}] channel={} userId={}", cmdName, args, channel, userId);
            return handler.execute(args, channel, userId, conversationId);
        } catch (Exception e) {
            log.error("命令执行异常: /{} args=[{}]", cmdName, args, e);
            return "命令执行出错: " + e.getMessage();
        }
    }
}
