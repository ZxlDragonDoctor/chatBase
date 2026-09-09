package com.zxl.chatbase.wxroboot.webhook.config;

import com.zxl.chatbase.wxroboot.webhook.util.aes.AesException;
import com.zxl.chatbase.wxroboot.webhook.util.aes.WXBizJsonMsgCrypt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
public class WXBizJsonMsgCryptConfig {

    public static class WxCryptConfiguredCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String stoken = context.getEnvironment().getProperty("wechat.corp.stoken", "");
            String aesKey = context.getEnvironment().getProperty("wechat.corp.sEncodingAESKey", "");
            boolean configured = StringUtils.hasText(stoken) && StringUtils.hasText(aesKey);
            if (!configured) {
                log.warn("企业微信配置不完整，不注册 WXBizJsonMsgCrypt Bean（企业微信功能不可用）");
            }
            return configured;
        }
    }

    @Value("${wechat.corp.stoken:}")
    private String stoken;

    @Value("${wechat.corp.sEncodingAESKey:}")
    private String sEncodingAESKey;

    @Value("${wechat.corp.botName:企业内部机器人}")
    private String botName;

    private String receiveId = "";

    private volatile WXBizJsonMsgCrypt cached;

    /**
     * 仅在 stoken / AES Key 均非空时注册 Bean，避免 @Bean 返回 null 导致容器异常。
     */
    @Bean
    @Conditional(WxCryptConfiguredCondition.class)
    public WXBizJsonMsgCrypt wxcpt() throws AesException {
        return new WXBizJsonMsgCrypt(stoken, sEncodingAESKey, receiveId);
    }

    /**
     * 供运行时按需获取；未配置时返回 null，不抛异常。
     */
    public WXBizJsonMsgCrypt getWxcpt() {
        if (cached != null) {
            return cached;
        }
        if (!StringUtils.hasText(sEncodingAESKey) || !StringUtils.hasText(stoken)) {
            return null;
        }
        try {
            cached = new WXBizJsonMsgCrypt(stoken, sEncodingAESKey, receiveId);
            return cached;
        } catch (AesException e) {
            log.error("企业微信 AES 密钥无效: {}", e.getMessage());
            return null;
        }
    }

    public String getsEncodingAESKey() {
        return sEncodingAESKey;
    }

    public String getBotName() {
        return botName;
    }
}
