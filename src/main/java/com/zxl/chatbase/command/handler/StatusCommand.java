package com.zxl.chatbase.command.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zxl.chatbase.command.CommandHandler;
import com.zxl.chatbase.im.entity.ImGroup;
import com.zxl.chatbase.im.mapper.ImGroupMapper;
import com.zxl.chatbase.im.service.ImConversationService;
import com.zxl.chatbase.opencode.OpencodeService;
import com.zxl.chatbase.opencode.node.OpencodeNodeService;
import com.zxl.chatbase.kb.entity.KbApp;
import com.zxl.chatbase.kb.mapper.KbAppMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class StatusCommand implements CommandHandler {

    @Resource
    private ImConversationService imConversationService;
    @Resource
    private OpencodeService opencodeService;
    @Resource
    private OpencodeNodeService opencodeNodeService;
    @Resource
    private KbAppMapper kbAppMapper;
    @Resource
    private ImGroupMapper imGroupMapper;

    @Override
    public String name() { return "status"; }

    @Override
    public String[] aliases() { return new String[]{"状态"}; }

    @Override
    public String description() { return "显示机器人状态和会话信息"; }

    @Override
    public String execute(String args, String channel, String userId, String conversationId) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 机器人状态\n\n");
        sb.append("渠道: ").append(channel).append("\n");
        sb.append("用户: ").append(userId).append("\n");
        sb.append("会话: ").append(conversationId).append("\n\n");

        // opencode binding status
        boolean opencodeBound = imConversationService.isOpencodeBound(conversationId);
        sb.append("opencode绑定: ").append(opencodeBound ? "✅ 已绑定" : "❌ 未绑定").append("\n");
        Long nodeId = imConversationService.getOpencodeNodeId(conversationId);
        if (nodeId != null) {
            sb.append("本机节点: ").append(opencodeNodeService.getNodeStatus(nodeId)).append(" (ID: ").append(nodeId).append(")\n");
        }

        // app info
        Long appId = imConversationService.getAppIdForConversation(conversationId);
        if (appId != null && appId > 0) {
            KbApp app = kbAppMapper.selectById(appId);
            if (app != null) {
                sb.append("绑定应用: ").append(app.getName()).append(" (ID: ").append(appId).append(")\n");
            }
        } else {
            sb.append("绑定应用: 使用默认应用\n");
        }

        // opencode session status
        if (opencodeBound) {
            sb.append("opencode会话: ").append(opencodeService.hasCurrentSession(conversationId) ? "活跃" : "无").append("\n");
        }

        return sb.toString().trim();
    }
}
