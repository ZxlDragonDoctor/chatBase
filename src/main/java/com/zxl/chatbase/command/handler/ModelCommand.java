package com.zxl.chatbase.command.handler;

import com.zxl.chatbase.command.CommandHandler;
import com.zxl.chatbase.opencode.OpencodeService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class ModelCommand implements CommandHandler {

    @Resource
    private OpencodeService opencodeService;

    @Override
    public String name() { return "model"; }

    @Override
    public String[] aliases() { return new String[]{"模型"}; }

    @Override
    public String description() { return "查看/切换 opencode 模型"; }

    @Override
    public String execute(String args, String channel, String userId, String conversationId) {
        String trimmed = args == null ? "" : args.trim();

        if (trimmed.isEmpty()) {
            String current = opencodeService.getModel(conversationId);
            if (current == null) {
                return "当前模型：默认（opencode.json 配置）\n"
                        + "用法：/model <模型ID>\n"
                        + "示例：/model mimo-v2.5-free\n"
                        + "      /model deepseek-v4-flash-free\n"
                        + "重置：/model default";
            }
            return "当前模型：" + current + "\n"
                    + "切换：/model <模型ID>\n"
                    + "重置：/model default";
        }

        if ("default".equalsIgnoreCase(trimmed) || "默认".equals(trimmed)) {
            opencodeService.deleteModel(conversationId);
            return "已重置为默认模型（opencode.json 配置）";
        }

        if ("list".equalsIgnoreCase(trimmed) || "列表".equals(trimmed)) {
            return "可用模型：\n"
                    + "• mimo-v2.5-free — MiMo V2.5 Free\n"
                    + "• deepseek-v4-flash-free — DeepSeek V4 Flash Free\n"
                    + "• 自定义：/model <providerID:modelID>\n"
                    + "  例：/model opencode:mimo-v2.5-free";
        }

        String[] parsed = OpencodeService.parseModel(trimmed);
        if (parsed == null) {
            return "格式错误，请输入模型ID\n"
                    + "示例：/model mimo-v2.5-free";
        }

        opencodeService.setModel(conversationId, parsed[0], parsed[1]);
        return "模型已切换为：" + parsed[0] + ":" + parsed[1] + "\n"
                + "（当前会话后续消息将使用新模型）\n"
                + "如需全新会话，请发送 /new";
    }
}
