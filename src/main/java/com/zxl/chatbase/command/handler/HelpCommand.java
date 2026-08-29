package com.zxl.chatbase.command.handler;

import com.zxl.chatbase.command.BotCommandDispatcher;
import com.zxl.chatbase.command.CommandHandler;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;
import java.util.TreeSet;

@Component
public class HelpCommand implements CommandHandler {

    @Resource
    private BotCommandDispatcher dispatcher;

    @Override
    public String name() { return "help"; }

    @Override
    public String[] aliases() { return new String[]{"帮助"}; }

    @Override
    public String description() { return "显示可用命令列表"; }

    @Override
    public String execute(String args, String channel, String userId, String conversationId) {
        // Collect unique commands from the dispatcher's registered handlers
        Set<String> seen = new TreeSet<>();
        StringBuilder sb = new StringBuilder();
        sb.append("📋 可用命令列表\n\n");

        for (CommandHandler handler : dispatcher.getRegisteredHandlers()) {
            if (seen.add(handler.name())) {
                sb.append("/").append(handler.name());
                for (String alias : handler.aliases()) {
                    sb.append(" /").append(alias);
                }
                sb.append(" - ").append(handler.description()).append("\n");
            }
        }
        sb.append("\n提示: 命令不区分大小写");
        return sb.toString().trim();
    }
}
