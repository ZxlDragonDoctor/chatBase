package com.zxl.chatbase.wxroboot.webhook.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxl.chatbase.command.BotCommandDispatcher;
import com.zxl.chatbase.chat.service.ChatService;
import com.zxl.chatbase.common.MonitorException;
import com.zxl.chatbase.dify.model.response.DifyChatResponse;
import com.zxl.chatbase.dify.server.DifyService;
import com.zxl.chatbase.im.entity.ImGroup;
import com.zxl.chatbase.im.mapper.ImGroupMapper;
import com.zxl.chatbase.im.service.GroupMessageSyncService;
import com.zxl.chatbase.im.service.ImConversationService;
import com.zxl.chatbase.im.service.ImGroupService;
import com.zxl.chatbase.im.service.ImUserService;
import com.zxl.chatbase.kb.entity.KbApp;
import com.zxl.chatbase.kb.mapper.KbAppMapper;
import com.zxl.chatbase.opencode.OpencodeService;
import com.zxl.chatbase.wxroboot.webhook.config.WXBizJsonMsgCryptConfig;
import com.zxl.chatbase.wxroboot.webhook.entity.DutyChatGroup;
import com.zxl.chatbase.wxroboot.webhook.entity.intelligentBot.IntelligentBotMsg;
import com.zxl.chatbase.wxroboot.webhook.service.IntelligentRobotService;
import com.zxl.chatbase.wxroboot.webhook.mapper.IntelligentRobotMapper;
import com.zxl.chatbase.wxroboot.webhook.util.WeChatUtil;
import com.zxl.chatbase.wxroboot.webhook.util.aes.WXBizJsonMsgCrypt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Service
public class IntelligentRobotServiceImpl extends ServiceImpl<IntelligentRobotMapper,
        DutyChatGroup> implements IntelligentRobotService {
    @Resource
    private IntelligentRobotMapper intelligentRobotMapper;

    @Autowired(required = false)
    private WXBizJsonMsgCrypt wxBizJsonMsgCrypt;
    @Resource
    private  WXBizJsonMsgCryptConfig wxBizJsonMsgCryptConfig;

    @Resource
    private ChatService chatService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ThreadPoolExecutor threadPool;
    @Resource
    private GroupMessageSyncService groupMessageSyncService;
    @Resource
    private ImGroupService imGroupService;
    @Resource
    private ImUserService imUserService;
    @Resource
    private ImGroupMapper imGroupMapper;
    @Resource
    private KbAppMapper kbAppMapper;
    @Resource
    private DifyService difyService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ImConversationService imConversationService;
    @Resource
    private OpencodeService opencodeService;
    @Resource
    private BotCommandDispatcher botCommandDispatcher;

    @Override
    public String verifyUrl(String msgSignature, String timestamp, String nonce, String echoStr) {
        if (wxBizJsonMsgCrypt == null) {
            throw MonitorException.build("企业微信未配置，无法验证URL");
        }
        String sEchoStr; //需要返回的明文
        try {
            sEchoStr = wxBizJsonMsgCrypt.VerifyURL(msgSignature, timestamp, nonce, echoStr);
            return sEchoStr;
        } catch (Exception e) {
            log.info("-->" + e.getMessage());
            throw MonitorException.build(e.getMessage());
        }
    }


    /**
     * 核心方法，接收企微消息（支持群聊和单聊）
     * 使用 Redis 分布式锁实现幂等处理，快速返回空响应，异步发送回复
     */
    @Override
    public String handleMessage(String msgSignature, String timestamp, String nonce, String postData) {
        if (msgSignature == null || timestamp == null || nonce == null || postData == null) {
            log.error("缺少必要参数");
            return buildNullReturnString(timestamp, nonce);
        }
        log.info("-->收到消息解密前，msg_signature={}, timestamp={}, nonce={}, data={}", msgSignature, timestamp, nonce, postData);

        try {
            String msgStr = decryptData(msgSignature, timestamp, nonce, postData);
            log.info("-->收到消息解密后，msg{}", msgStr);
            IntelligentBotMsg msg = objectMapper.readValue(msgStr, IntelligentBotMsg.class);

            if (!msg.isValidMessage()) {
                log.info("-->msg无效");
                return buildNullReturnString(timestamp, nonce);
            }

            String msgId = msg.getMsgid();
            String lockKey = "wecom:msg:" + msgId;

            Boolean locked = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", 5, TimeUnit.MINUTES);

            if (locked == null || !locked) {
                log.info("企微消息已处理或正在处理，跳过: msgId={}", msgId);
                return buildNullReturnString(timestamp, nonce);
            }

            boolean isGroup = msg.isFromGroup();
            String chatId = msg.getChatid();
            String userId = msg.getFrom().getUserid();

            log.info("企微消息开始处理: msgId={}, isGroup={}, chatId={}, userId={}", msgId, isGroup, chatId, userId);

            final String msgType = msg.getMsgtype();
            final String fileUrl;
            final String fileName;
            String rawMessage;

            if ("image".equals(msgType) && msg.getImage() != null) {
                fileUrl = msg.getImage().getUrl();
                fileName = "image_" + System.currentTimeMillis();
                rawMessage = "[图片消息]";
                log.info("企微图片消息: chatId={}, fileUrl={}", chatId, fileUrl);
            } else if ("text".equals(msgType) && msg.getText() != null) {
                fileUrl = null;
                fileName = null;
                rawMessage = msg.getText().getContent();
            } else if ("mixed".equals(msgType) || "stream".equals(msgType)) {
                fileUrl = null;
                fileName = null;
                rawMessage = msg.getText() != null ? msg.getText().getContent() : "[混合消息]";
            } else {
                fileUrl = null;
                fileName = null;
                rawMessage = "";
            }

            if (msg.isMsgImage() || msg.isMsgStream() || msg.isMsgText() || msg.isMsgMixed()) {
                String finalQuery;
                if (isGroup) {
                    finalQuery = rawMessage.replace("@".concat(wxBizJsonMsgCryptConfig.getBotName()), "");
                } else {
                    finalQuery = rawMessage;
                }

                final boolean finalIsGroup = isGroup;
                final String channel = "wecom";
                final String convId = isGroup ? chatId : "single:wecom:" + userId;

                // 命令检测：以 / 开头的消息优先走命令处理
                if (botCommandDispatcher.isCommand(finalQuery)) {
                    CompletableFuture.runAsync(() -> {
                        try {
                            String cmdReply = botCommandDispatcher.dispatch(finalQuery, channel, userId, convId);
                            if (cmdReply != null && msg.getResponse_url() != null && !msg.getResponse_url().isEmpty()) {
                                WeChatUtil.sendMarkdown(msg.getResponse_url(), cmdReply);
                                log.info("企微命令回复发送成功: userId={}, cmd={}", userId, finalQuery);
                            }
                        } catch (Exception e) {
                            log.error("企微命令处理失败: userId={}, cmd={}", userId, finalQuery, e);
                        }
                    }, threadPool);
                    return buildNullReturnString(timestamp, nonce);
                }

                CompletableFuture.runAsync(() -> {
                    try {
                        if (finalIsGroup) {
                            handleWeComGroupMessage(msg, msgId, chatId, userId, finalQuery, msgType, fileUrl, fileName);
                        } else {
                            handleWeComSingleMessage(msg, msgId, userId, finalQuery, msgType);
                        }
                    } catch (Exception e) {
                        log.error("企微消息处理失败: msgId={}, chatId={}", msgId, chatId, e);
                    }
                }, threadPool);
            }

            return buildNullReturnString(timestamp, nonce);

        } catch (Exception e) {
            log.error("-->企微消息处理异常: {}", e.getMessage(), e);
            return buildNullReturnString(timestamp, nonce);
        }
    }

    private void handleWeComGroupMessage(IntelligentBotMsg msg, String msgId, String chatId,
                                          String userId, String query, String msgType,
                                          String fileUrl, String fileName) {
        groupMessageSyncService.saveGroupMessage(
                "wecom", msgId, chatId, userId,
                query, msgType, System.currentTimeMillis() / 1000,
                fileUrl, fileName
        );

        imGroupService.getOrCreateGroup("wecom", chatId, null);
        imUserService.getOrCreateUser("wecom", userId, chatId, userId);

        Long appId = getAppIdForGroup(chatId);
        log.info("企微群组应用绑定: chatId={}, appId={}", chatId, appId);

        DifyChatResponse difyChatResponse = chatService.chat(
                appId, "wecom", userId, chatId, query
        );

        String replyContent = decodeUnicode(difyChatResponse.getAnswer());
        log.info("企微群聊回复生成完成: chatId={}, reply={}", chatId, replyContent);
        replyContent = filterThinkingContent(replyContent);

        if (msg.getResponse_url() != null && !msg.getResponse_url().isEmpty()) {
            WeChatUtil.sendMarkdown(msg.getResponse_url(), replyContent);
            log.info("企微群聊回复发送成功: chatId={}", chatId);
        }
    }

    private void handleWeComSingleMessage(IntelligentBotMsg msg, String msgId, String userId,
                                           String query, String msgType) {
        String conversationId = "single:wecom:" + userId;
        String nickname = userId;

        groupMessageSyncService.savePrivateMessage(
                "wecom", msgId, userId,
                query, msgType, System.currentTimeMillis() / 1000,
                conversationId, null, null
        );

        imConversationService.getOrCreateConversation("wecom", userId, nickname, null);
        imConversationService.updateLastMessage(conversationId, query, userId, "wecom");
        imUserService.getOrCreateUser("wecom", userId, null, nickname);

        String replyContent;
        // 会话绑定本地 opencode 时走 opencode 通道
        if (imConversationService.isOpencodeBound(conversationId)) {
            replyContent = opencodeService.chat(conversationId, query, userId, "wecom");
            log.info("企微单聊opencode回复生成完成: userId={}, reply={}", userId, replyContent);
            if (msg.getResponse_url() != null && !msg.getResponse_url().isEmpty()) {
                WeChatUtil.sendMarkdown(msg.getResponse_url(), replyContent);
                log.info("企微单聊opencode回复发送成功: userId={}", userId);
            }
            return;
        }

        Long appId = imConversationService.getAppIdForConversation(conversationId);
        log.info("企微单聊: userId={}, appId={}", userId, appId);

        DifyChatResponse difyChatResponse = chatService.chat(
                appId, "wecom", userId, conversationId, query
        );

        replyContent = decodeUnicode(difyChatResponse.getAnswer());
        log.info("企微单聊回复生成完成: userId={}, reply={}", userId, replyContent);
        replyContent = filterThinkingContent(replyContent);

        if (msg.getResponse_url() != null && !msg.getResponse_url().isEmpty()) {
            WeChatUtil.sendMarkdown(msg.getResponse_url(), replyContent);
            log.info("企微单聊回复发送成功: userId={}", userId);
        }
    }


    //解密
    private String decryptData(String msgSignature, String timestamp, String nonce, String postData) {
        if (wxBizJsonMsgCrypt == null) {
            throw MonitorException.build("企业微信未配置，无法解密消息");
        }
        try {
            return wxBizJsonMsgCrypt.DecryptMsg(msgSignature, timestamp, nonce, postData);
        } catch (Exception e) {
            log.info("-->" + e.getMessage());
            throw MonitorException.build(e.getMessage());
        }
    }

    //加密
    private String encryptData(String timestamp, String nonce, String reply) {
        if (wxBizJsonMsgCrypt == null) {
            throw MonitorException.build("企业微信未配置，无法加密消息");
        }
        try {
            String sEncryptMsg = wxBizJsonMsgCrypt.EncryptMsg(reply, timestamp, nonce);
            return sEncryptMsg;
            // TODO:
            // HttpUtils.SetResponse(sEncryptMsg);
        } catch (Exception e) {
            log.info("-->" + e.getMessage());
            throw MonitorException.build(e.getMessage());
        }
    }

    // 解码 Unicode 转义
    public static String decodeUnicode(String str) {
        StringBuilder sb = new StringBuilder();
        int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (c == '\\' && i + 1 < len && str.charAt(i + 1) == 'u') {
                String hex = str.substring(i + 2, i + 6);
                sb.append((char) Integer.parseInt(hex, 16));
                i += 5;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 过滤AI思考过程内容，避免暴露给群聊用户
     * 支持多行和多个 <think>...</think> 块
     */
    private String filterThinkingContent(String text) {
        if (text == null) return "";
        // 使用正则表达式过滤 <think>...</think> 内容（支持多行）
        String filtered = text.replaceAll("(?s)<think>.*?</think>", "").trim();
        return filtered.isEmpty() ? text : filtered;
    }

    private String buildNullReturnString(String timestamp, String nonce) {
        String reply = "{\n" +
                "  \"msgtype\": \"text\",\n" +
                "  \"text\": {\n" +
                "    \"content\": \"\"\n" +
                "  }\n" +
                "}";
        return encryptData(timestamp, nonce, reply);
    }

    private String buildReturnString(String reply_content, String timestamp, String nonce) {
        String reply = "{\n" +
                "    \"msgtype\": \"stream\",\n" +
                "    \"stream\": {\n" +
                "        \"id\": \"STREAMID\",\n" +
                "        \"finish\": true,\n" +
                "        \"content\": \"" + reply_content + "\"\n" +
                "    }\n" +
                "}";

        return encryptData(timestamp, nonce, reply);
    }

    /**
     * 获取群组绑定的应用ID
     * 优先使用群组绑定的应用，如果没有则使用默认应用
     */
    private Long getAppIdForGroup(String groupId) {
        try {
            LambdaQueryWrapper<ImGroup> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ImGroup::getGroupId, groupId)
                    .eq(ImGroup::getStatus, true);
            ImGroup group = imGroupMapper.selectOne(wrapper);
            
            if (group != null && group.getAppId() != null) {
                KbApp app = kbAppMapper.selectById(group.getAppId());
                if (app != null && app.getStatus()) {
                    return app.getId();
                }
            }
            
            LambdaQueryWrapper<KbApp> appWrapper = new LambdaQueryWrapper<>();
            appWrapper.eq(KbApp::getStatus, true)
                    .eq(KbApp::getIsDefault, true)
                    .last("LIMIT 1");
            KbApp defaultApp = kbAppMapper.selectOne(appWrapper);
            return defaultApp != null ? defaultApp.getId() : null;
        } catch (Exception e) {
            log.error("获取企微群组应用失败: groupId={}", groupId, e);
            return null;
        }
    }

    private Long getDefaultAppId() {
        try {
            LambdaQueryWrapper<KbApp> appWrapper = new LambdaQueryWrapper<>();
            appWrapper.eq(KbApp::getStatus, true)
                    .eq(KbApp::getIsDefault, true)
                    .last("LIMIT 1");
            KbApp defaultApp = kbAppMapper.selectOne(appWrapper);
            return defaultApp != null ? defaultApp.getId() : null;
        } catch (Exception e) {
            log.error("获取默认应用失败", e);
            return null;
        }
    }
}
