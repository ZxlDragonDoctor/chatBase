package com.zxl.chatbase.command;

/**
 * 机器人命令处理接口
 * 每个命令实现此接口，注册到 BotCommandDispatcher
 */
public interface CommandHandler {

    /**
     * 主命令名（不含 / 前缀），如 "help"
     */
    String name();

    /**
     * 别名列表（不含 / 前缀），如 ["帮助"]
     */
    String[] aliases();

    /**
     * 命令说明，用于 /help 展示
     */
    String description();

    /**
     * 执行命令并返回回复文本
     *
     * @param args          命令参数（去掉命令名后的部分，已 trim）
     * @param channel       渠道标识（wecom / wx / qq）
     * @param userId        用户ID
     * @param conversationId 会话ID
     * @return 回复文本
     */
    String execute(String args, String channel, String userId, String conversationId);
}
