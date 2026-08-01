package com.zxl.chatbase.wx.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxl.chatbase.chat.service.ChatService;
import com.zxl.chatbase.dify.model.request.FileInfo;
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
import com.zxl.chatbase.wx.config.WxProperties;
import com.zxl.chatbase.wx.model.WxInboundMessage;
import com.zxl.chatbase.wx.model.WxMediaInfo;
import com.zxl.chatbase.wx.model.WxOutboundMessage;
import com.zxl.chatbase.wx.util.WxCryptoUtil;
import com.zxl.chatbase.wx.util.WxIlinkUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class WxIlinkService {

    private final WxProperties wxProperties;
    private final WxIlinkUtil wxIlinkUtil;
    private final ObjectMapper objectMapper;
    private final GroupMessageSyncService groupMessageSyncService;
    private final ImGroupService imGroupService;
    private final ImUserService imUserService;
    private final ChatService chatService;
    private final DifyService difyService;
    private final ImGroupMapper imGroupMapper;
    private final KbAppMapper kbAppMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RestTemplate restTemplate;
    private final ImConversationService imConversationService;
    private final OpencodeService opencodeService;

    private static final String REDIS_UPDATES_BUF_KEY = "bot:wx:get_updates_buf";
    private static final String REDIS_ONLINE_KEY = "bot:wx:online";
    private static final String REDIS_CREDENTIALS_KEY = "bot:wx:credentials";
    private static final String ILINK_API_BASE = "https://ilinkai.weixin.qq.com";

    private volatile String activeBaseUrl;
    private volatile String activeBotToken;
    private volatile String activeNickname;
    private volatile Thread pollingThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    public void startPolling() {
        String savedCreds = stringRedisTemplate.opsForValue().get(REDIS_CREDENTIALS_KEY);
        if (StringUtils.hasText(savedCreds)) {
            try {
                Map<String, String> creds = objectMapper.readValue(savedCreds,
                        new TypeReference<Map<String, String>>() {});
                activeBaseUrl = creds.get("baseUrl");
                activeBotToken = creds.get("botToken");
                activeNickname = creds.get("nickname");
                log.info("从 Redis 恢复微信 ilink 凭证");
            } catch (Exception e) {
                log.warn("恢复微信 ilink 凭证失败", e);
                stringRedisTemplate.delete(REDIS_CREDENTIALS_KEY);
            }
        }

        String baseUrl = resolveBaseUrl();
        String botToken = resolveBotToken();
        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(botToken)) {
            if (wxProperties.isEnable()) {
                log.warn("微信 ilink 机器人配置不完整，跳过启动");
            }
            return;
        }

        log.info("微信 ilink 机器人启动，baseUrl={}, nickname={}", baseUrl, getNickname());
        startPollingThread();
    }

    private void startPollingThread() {
        if (running.get()) return;
        running.set(true);
        pollingThread = new Thread(this::pollingLoop, "wx-ilink-poll");
        pollingThread.setDaemon(false);
        pollingThread.start();
    }

    public void login(String baseUrl, String botToken, String nickname) {
        this.activeBaseUrl = baseUrl;
        this.activeBotToken = botToken;
        this.activeNickname = nickname;
        try {
            Map<String, String> creds = new HashMap<>();
            creds.put("baseUrl", baseUrl);
            creds.put("botToken", botToken);
            creds.put("nickname", nickname);
            stringRedisTemplate.opsForValue().set(REDIS_CREDENTIALS_KEY,
                    objectMapper.writeValueAsString(creds), Duration.ofDays(30));
        } catch (Exception e) {
            log.warn("保存微信 ilink 凭证失败", e);
        }
        startPollingThread();
    }

    public void logout() {
        running.set(false);
        if (pollingThread != null) {
            pollingThread.interrupt();
            pollingThread = null;
        }
        activeBaseUrl = null;
        activeBotToken = null;
        activeNickname = null;
        stringRedisTemplate.delete(REDIS_CREDENTIALS_KEY);
        markOffline();
    }

    public boolean isOnline() {
        return "1".equals(stringRedisTemplate.opsForValue().get(REDIS_ONLINE_KEY));
    }

    public String getNickname() {
        return activeNickname != null ? activeNickname : wxProperties.getNickname();
    }

    private String resolveBaseUrl() {
        return activeBaseUrl != null ? activeBaseUrl : wxProperties.getBaseUrl();
    }

    private String resolveBotToken() {
        return activeBotToken != null ? activeBotToken : wxProperties.getBotToken();
    }

    private void pollingLoop() {
        String getUpdatesBuf = loadGetUpdatesBuf();
        int consecutiveErrors = 0;

        while (running.get()) {
            try {
                String baseUrl = resolveBaseUrl();
                String botToken = resolveBotToken();
                if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(botToken)) {
                    log.warn("微信 ilink 凭证缺失，停止轮询");
                    break;
                }

                List<WxInboundMessage> messages = wxIlinkUtil.getUpdates(
                        baseUrl, botToken, getUpdatesBuf);

                if (messages == null) {
                    consecutiveErrors++;
                    if (consecutiveErrors >= 3) {
                        log.warn("微信 ilink 连续 {} 次轮询失败，{}s 后重试",
                                consecutiveErrors, wxProperties.getReconnectDelaySec());
                    }
                    sleep(wxProperties.getReconnectDelaySec() * 1000L);
                    continue;
                }
                consecutiveErrors = 0;

                markOnline();

                for (WxInboundMessage msg : messages) {
                    try {
                        processMessage(msg);
                    } catch (Exception e) {
                        log.error("处理微信消息失败: msgId={}", msg.getMsgId(), e);
                    }
                }

                String newBuf = wxIlinkUtil.getNextUpdatesBuf();
                if (newBuf != null && !newBuf.equals(getUpdatesBuf)) {
                    getUpdatesBuf = newBuf;
                    saveGetUpdatesBuf(getUpdatesBuf);
                }

                sleep(1000);
            } catch (Exception e) {
                log.error("微信 ilink 轮询异常", e);
                sleep(wxProperties.getReconnectDelaySec() * 1000L);
            }
        }
        log.info("微信 ilink 轮询已停止");
        markOffline();
    }

    private void processMessage(WxInboundMessage msg) {

        Integer messageType = msg.getMessageType();
        if (messageType == null || messageType != 1) {
            log.info("跳过非用户消息: messageType={}, fromUser={}, fromGroup={}, text={}",
                    messageType, msg.getFromUserId(), msg.getFromGroupId(), truncate(msg.getTextContent(), 50));
            return;
        }

        String fromUser = msg.getFromUserId();
        String fromGroup = msg.getFromGroupId();
        if (!StringUtils.hasText(fromUser)) {
            log.warn("消息缺少 from_user_id，跳过");
            return;
        }

        String textContent = msg.getTextContent();
        String msgType = msg.getContentType();
        String rawMessage = textContent;
        String fileUrl = null;
        String fileName = null;
        List<FileInfo> fileInfos = new ArrayList<>();

        boolean isGroup = msg.isGroupChat();
        boolean isPrivate = msg.isPrivateChat();
        log.info("收到微信消息: msgId={}, messageType={}, isGroup={}, isPrivate={}, fromUser={}, fromGroup={}, text=[{}]",
                msg.getMsgId(), messageType, isGroup, isPrivate, fromUser, fromGroup, truncate(rawMessage, 100));

        String conversationId = null;
        if (isPrivate) {
            conversationId = "single:wx:" + fromUser;
        }

        if (isGroup && !StringUtils.hasText(fromGroup)) {
            fromGroup = fromUser;
        }

        String effectiveGroupId = isGroup ? fromGroup : fromUser;
        String displayGroupId = isGroup ? fromGroup : conversationId;

        // 媒体文件处理：图片/文件
        if (msg.isImage() || msg.isFile()) {
            try {
                WxInboundMessage.MediaItem media = msg.getFirstMedia();
                if (media != null) {
                    WxMediaInfo mediaInfo = null;
                    if (StringUtils.hasText(media.getCdnUrl())) {
                        WxMediaInfo info = new WxMediaInfo();
                        info.setCdnUrl(media.getCdnUrl());
                        info.setAesKey(media.getAesKey());
                        info.setFileName(media.getFileName());
                        info.setFileSize(media.getFileSize());
                        mediaInfo = info;
                    } else if (StringUtils.hasText(media.getMediaKey())) {
                        mediaInfo = wxIlinkUtil.getUploadUrl(
                                resolveBaseUrl(), resolveBotToken(),
                                msg.getMsgId(), media.getMediaKey());
                    }

                    if (mediaInfo != null && StringUtils.hasText(mediaInfo.getCdnUrl())) {
                        byte[] encryptedData = wxIlinkUtil.downloadFromCdn(mediaInfo.getCdnUrl());
                        if (encryptedData != null) {
                            byte[] decryptedData;
                            if (StringUtils.hasText(mediaInfo.getAesKey())) {
                                decryptedData = WxCryptoUtil.decryptMedia(encryptedData, mediaInfo.getAesKey());
                            } else {
                                decryptedData = encryptedData;
                            }

                            String mediaFileName = StringUtils.hasText(mediaInfo.getFileName())
                                    ? mediaInfo.getFileName()
                                    : (msg.isImage() ? "image_" + System.currentTimeMillis() + ".jpg" : "file_" + System.currentTimeMillis());

                            java.io.File tempFile = java.io.File.createTempFile("wx_media_", "_" + mediaFileName);
                            tempFile.deleteOnExit();
                            java.nio.file.Files.write(tempFile.toPath(), decryptedData);

                            org.springframework.web.multipart.MultipartFile multipartFile =
                                    new ByteArrayMultipartFile("file", mediaFileName, decryptedData);

                            var difyFile = difyService.uploadFile(multipartFile);
                            if (difyFile != null && difyFile.getId() != null) {
                                String difyFileId = difyFile.getId().toString();
                                FileInfo fi = new FileInfo();
                                fi.setType("image");
                                fi.setTransferMethod("local_file");
                                fi.setUploadFileId(difyFileId);
                                fileInfos.add(fi);

                                fileUrl = "dify://" + difyFileId;
                                fileName = mediaFileName;
                            }

                            tempFile.delete();
                        }
                    }

                    if (!StringUtils.hasText(textContent)) {
                        rawMessage = msg.isImage() ? "[图片消息]" : "[文件消息]";
                    }
                }
            } catch (Exception e) {
                log.error("媒体文件处理失败: msgId={}", msg.getMsgId(), e);
                if (!StringUtils.hasText(textContent)) {
                    rawMessage = msg.isImage() ? "[图片消息]" : "[文件消息]";
                }
            }
        }

        if (!StringUtils.hasText(rawMessage)) {
            log.debug("跳过空消息: msgId={}", msg.getMsgId());
            return;
        }

        if (isGroup) {
            String botNickname = getNickname();
            String atMention = "@" + botNickname;
            if (!rawMessage.contains(atMention)) {
                log.info("群聊消息未 @机器人({}), 仅采集: groupId={}, msg=[{}]",
                        atMention, fromGroup, truncate(rawMessage, 100));
                groupMessageSyncService.saveGroupMessage("wx", msg.getMsgId(), fromGroup,
                        fromUser, rawMessage, msgType, msg.getTimestamp() != null ? msg.getTimestamp() : System.currentTimeMillis() / 1000,
                        fileUrl, fileName);
                imGroupService.getOrCreateGroup("wx", fromGroup, null);
                imUserService.getOrCreateUser("wx", fromUser, fromGroup, fromUser);
                return;
            }
            rawMessage = rawMessage.replace(atMention, "").trim();
            log.info("群聊消息触发 @机器人 问答: groupId={}, 去除@后=[{}]", fromGroup, truncate(rawMessage, 100));
        }

        if (isPrivate) {
            groupMessageSyncService.savePrivateMessage("wx", msg.getMsgId(), fromUser,
                    rawMessage, msgType, msg.getTimestamp() != null ? msg.getTimestamp() : System.currentTimeMillis() / 1000,
                    conversationId, fileUrl, fileName);
            imConversationService.getOrCreateConversation("wx", fromUser, fromUser, null);
            imConversationService.updateLastMessage(conversationId, rawMessage, fromUser, "wx");
        } else {
            groupMessageSyncService.saveGroupMessage("wx", msg.getMsgId(), fromGroup,
                    fromUser, rawMessage, msgType, msg.getTimestamp() != null ? msg.getTimestamp() : System.currentTimeMillis() / 1000,
                    fileUrl, fileName);
            imGroupService.getOrCreateGroup("wx", fromGroup, null);
        }
        imUserService.getOrCreateUser("wx", fromUser, isGroup ? fromGroup : null, fromUser);

        // Dify 问答
        try {
            if (isPrivate && imConversationService.isOpencodeBound(conversationId)) {
                String opencodeAnswer = opencodeService.chat(conversationId, rawMessage, fromUser, "wx");
                opencodeAnswer = filterThinkingContent(opencodeAnswer);
                if (StringUtils.hasText(opencodeAnswer) && StringUtils.hasText(msg.getContextToken())) {
                    WxOutboundMessage reply = WxOutboundMessage.createTextMessage(
                            fromUser, msg.getContextToken(), opencodeAnswer);
                    int ret = wxIlinkUtil.sendMessage(
                            resolveBaseUrl(), resolveBotToken(), reply);
                    if (ret == -14) {
                        log.error("微信 ilink token 过期，停止轮询（需要重新扫码）");
                        markOffline();
                        Thread.currentThread().interrupt();
                        return;
                    }
                    log.info("微信opencode回复发送成功: msgId={}, toUser={}, ret={}", msg.getMsgId(), fromUser, ret);
                } else {
                    log.warn("微信opencode问答结果为空或缺少context_token，未发送回复: msgId={}, hasAnswer={}, hasContextToken={}",
                            msg.getMsgId(), StringUtils.hasText(opencodeAnswer), StringUtils.hasText(msg.getContextToken()));
                }
                return;
            }

            Long appId = isPrivate ? imConversationService.getAppIdForConversation(conversationId) : getAppIdForGroup(fromGroup);
            log.info("微信消息开始问答: msgId={}, fromUser={}, groupId={}, appId={}",
                    msg.getMsgId(), fromUser, isPrivate ? conversationId : fromGroup, appId);

            DifyChatResponse response = chatService.chat(
                    appId, "wx", fromUser, displayGroupId, rawMessage);

            String answer = response != null ? response.getAnswer() : "";
            answer = filterThinkingContent(answer);

            if (StringUtils.hasText(answer) && StringUtils.hasText(msg.getContextToken())) {
                WxOutboundMessage reply = WxOutboundMessage.createTextMessage(
                        fromUser, msg.getContextToken(), answer);
                int ret = wxIlinkUtil.sendMessage(
                        resolveBaseUrl(), resolveBotToken(), reply);
                if (ret == -14) {
                    log.error("微信 ilink token 过期，停止轮询（需要重新扫码）");
                    markOffline();
                    Thread.currentThread().interrupt();
                    return;
                }
                log.info("微信回复发送成功: msgId={}, toUser={}, ret={}", msg.getMsgId(), fromUser, ret);
            } else {
                log.warn("微信问答结果为空或缺少context_token，未发送回复: msgId={}, hasAnswer={}, hasContextToken={}",
                        msg.getMsgId(), StringUtils.hasText(answer), StringUtils.hasText(msg.getContextToken()));
            }
        } catch (Exception e) {
            log.error("微信消息问答失败: msgId={}", msg.getMsgId(), e);
        }
    }

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
            log.error("获取微信群组应用失败: groupId={}", groupId, e);
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

    private String filterThinkingContent(String text) {
        if (text == null) return "";
        String filtered = text.replaceAll("(?s)<think>.*?</think>", "").trim();
        return filtered.isEmpty() ? text : filtered;
    }

    private void markOnline() {
        stringRedisTemplate.opsForValue().set(REDIS_ONLINE_KEY, "1", 30, TimeUnit.SECONDS);
    }

    private void markOffline() {
        stringRedisTemplate.delete(REDIS_ONLINE_KEY);
    }

    private String loadGetUpdatesBuf() {
        String buf = stringRedisTemplate.opsForValue().get(REDIS_UPDATES_BUF_KEY);
        return buf != null ? buf : "";
    }

    private void saveGetUpdatesBuf(String buf) {
        if (buf != null) {
            stringRedisTemplate.opsForValue().set(REDIS_UPDATES_BUF_KEY, buf, Duration.ofDays(7));
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    private static class ByteArrayMultipartFile implements org.springframework.web.multipart.MultipartFile {
        private final String name;
        private final String originalFilename;
        private final byte[] content;

        ByteArrayMultipartFile(String name, String originalFilename, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.content = content;
        }

        @Override
        public String getName() { return name; }

        @Override
        public String getOriginalFilename() { return originalFilename; }

        @Override
        public String getContentType() { return null; }

        @Override
        public boolean isEmpty() { return content == null || content.length == 0; }

        @Override
        public long getSize() { return content != null ? content.length : 0; }

        @Override
        public byte[] getBytes() { return content; }

        @Override
        public InputStream getInputStream() { return new ByteArrayInputStream(content); }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}
