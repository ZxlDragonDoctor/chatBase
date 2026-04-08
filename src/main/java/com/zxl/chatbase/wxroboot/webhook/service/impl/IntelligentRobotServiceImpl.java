package com.zxl.chatbase.wxroboot.webhook.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxl.chatbase.chat.ChatService;
import com.zxl.chatbase.common.MonitorException;
import com.zxl.chatbase.dify.model.response.DifyChatResponse;
import com.zxl.chatbase.im.entity.GroupMessage;
import com.zxl.chatbase.im.service.GroupMessageSyncService;
import com.zxl.chatbase.wxroboot.webhook.entity.DutyChatGroup;
import com.zxl.chatbase.wxroboot.webhook.entity.intelligentBot.IntelligentBotMsg;
import com.zxl.chatbase.wxroboot.webhook.service.IntelligentRobotService;
import com.zxl.chatbase.wxroboot.webhook.mapper.IntelligentRobotMapper;
import com.zxl.chatbase.wxroboot.webhook.util.WeChatUtil;
import com.zxl.chatbase.wxroboot.webhook.util.aes.WXBizJsonMsgCrypt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Reference;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;



@Slf4j
@Service
public class IntelligentRobotServiceImpl extends ServiceImpl<IntelligentRobotMapper,
        DutyChatGroup> implements IntelligentRobotService {
    @Resource
    private IntelligentRobotMapper intelligentRobotMapper;

    @Resource
    private WXBizJsonMsgCrypt wxBizJsonMsgCrypt;

    @Resource
    private ChatService chatService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ThreadPoolExecutor threadPool;
    @Resource
    private GroupMessageSyncService groupMessageSyncService;

    @Override
    public String verifyUrl(String msgSignature, String timestamp, String nonce, String echoStr) {
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
     *  TODO: 核心方法，接收企微消息
     * @param msgSignature
     * @param timestamp
     * @param nonce
     * @param postData
     * @return
     */
    @Override
    public String handleMessage(String msgSignature, String timestamp, String nonce, String postData) {
        if (msgSignature == null || timestamp == null || nonce == null || postData == null) {
            log.error("缺少必要参数");
        }
        log.info("-->收到消息解密前，msg_signature={}, timestamp={}, nonce={}, data={}", msgSignature, timestamp, nonce, postData);

        try {
            // 解密消息
            String msgStr = decryptData(msgSignature, timestamp, nonce, postData);
            log.info("-->收到消息解密后，msg{}", msgStr);
            IntelligentBotMsg msg = objectMapper.readValue(msgStr, IntelligentBotMsg.class);

            String reply_content = "";

            // 只开放群聊使用
            if (!msg.isFromGroup()) {
                log.info("-->该功能仅限于群聊中使用!");
                reply_content = "该功能仅限于群聊中使用";
                return buildReturnString(reply_content, timestamp, nonce);
            }

            if (!msg.isValidMessage()) {
                log.info("-->msg无效");
                reply_content = "msg无效";
                return buildReturnString(reply_content, timestamp, nonce);
            }
            GroupMessage groupMessage = new GroupMessage();
            groupMessage.setPlatform("wx");
            // TODO: 可能是混合消息内容
            groupMessage.setMessageType(msg.getMsgtype());
            groupMessage.setUserId(msg.getFrom().getUserid());
            groupMessage.setGroupId(msg.getChatid());
            // // TODO： 图片等其他文件内容需要oss存储并记录其URL
            groupMessage.setRawMessage(msg.getText().getContent());
            groupMessage.setMessageTime(LocalDateTime.now());
            // 保存微信群聊消息内容
            CompletableFuture.runAsync(()->{
                groupMessageSyncService.save(groupMessage);
            },threadPool);

            if (msg.isMsgImage() || msg.isMsgStream() || msg.isMsgText() || msg.isMsgMixed()) {
                String query = msg.getText().getContent();
                // TODO：调用Dify获取模型结果
                // 异步回答，避免阻塞 WebSocket 消息线程
                CompletableFuture<DifyChatResponse> completableFuture = CompletableFuture.supplyAsync(() -> chatService.chat(
                                "wx",
                                msg.getFrom().getUserid(),
                                msg.getChatid(),
                                query
                        ), threadPool)
                        .orTimeout(160, TimeUnit.SECONDS)
                        .exceptionally(e -> {
                            log.error("聊天任务执行失败，groupId={}, userId={}", msg.getChatid(), msg.getFrom().getUserid(), e);
                            DifyChatResponse fallBack = new DifyChatResponse();
                            fallBack.setAnswer("[系统超时，请稍后重试]");
                            return  fallBack;
                        });
                DifyChatResponse difyChatResponse = completableFuture.get();
                // TODO; 发送消息
                reply_content  = difyChatResponse.getAnswer();
                // 群聊机器人链接
                String distributionRobotUrl = this.getBaseMapper().selectOne(new LambdaQueryWrapper<DutyChatGroup>()
                        .eq(DutyChatGroup::getChatGroupUrlId, msg.getChatid())).getDistributionRobotUrl();
                CompletableFuture.runAsync(()->
                        WeChatUtil.sendText(distributionRobotUrl,difyChatResponse.getAnswer())
                        ,threadPool);
                return buildReturnString(reply_content,timestamp, nonce);
            } else {
                reply_content = "未知的消息类型";
                return buildReturnString(reply_content, timestamp, nonce);
            }
        } catch (Exception e) {
            log.info("-->" + e.getMessage());
            throw MonitorException.build(e.getMessage());
        }
    }



    //解密
    private String decryptData(String msgSignature, String timestamp, String nonce, String postData) {
        try {
            return wxBizJsonMsgCrypt.DecryptMsg(msgSignature, timestamp, nonce, postData);
        } catch (Exception e) {
            log.info("-->" + e.getMessage());
            throw MonitorException.build(e.getMessage());
        }
    }

    //加密
    private String encryptData(String timestamp, String nonce, String reply) {
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

}
