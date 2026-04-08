package com.zxl.chatbase.wxroboot.webhook.service;


/**
 *  企业微信智能机器人服务
 */
public interface IntelligentRobotService {
    String verifyUrl(String msgSignature, String timestamp, String nonce, String echoStr) ;
    String handleMessage(String msgSignature, String timestamp, String nonce, String postData);

}
