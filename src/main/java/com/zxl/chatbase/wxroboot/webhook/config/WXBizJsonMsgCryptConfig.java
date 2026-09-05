package com.zxl.chatbase.wxroboot.webhook.config;

import com.zxl.chatbase.wxroboot.webhook.util.aes.AesException;
import com.zxl.chatbase.wxroboot.webhook.util.aes.WXBizJsonMsgCrypt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class WXBizJsonMsgCryptConfig {
    @Value("${wechat.corp.stoken:}")
    private String stoken;

    @Value("${wechat.corp.sEncodingAESKey:}")
    private String sEncodingAESKey;

    @Value("${wechat.corp.botName:企业内部机器人}")
    private String botName;

    private String receiveId = "";

    @Bean
    public WXBizJsonMsgCrypt wxcpt() throws AesException {
        if (sEncodingAESKey == null || sEncodingAESKey.isEmpty()
                || stoken == null || stoken.isEmpty()) {
            log.warn("企业微信配置不完整，使用占位 WXBizJsonMsgCrypt");
            return new WXBizJsonMsgCrypt("placeholder", "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG", "");
        }
         return new WXBizJsonMsgCrypt(stoken, sEncodingAESKey, receiveId);
    }

    public String getsEncodingAESKey() {
    	return sEncodingAESKey;
    }

    public String getBotName() {
        return botName;
    }
}