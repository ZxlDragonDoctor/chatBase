package com.zxl.chatbase.wxroboot.webhook.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 微信发送工具类
 * https://developer.work.weixin.qq.com/document/path/101138
 */
public class WeChatUtil {
	private static final Logger log = LoggerFactory.getLogger(WeChatUtil.class);
	private WeChatUtil(){}

	/**
	 * 发送文本消息
	 * @deprecated  最新企业消息不支持纯文本消息，改使用markdown格式的消息
	 * @author zhuxu
	 * @date 2022/3/4
	 * @param bot 企业微信机器人链接
	 * @param text 文本内容
	 * @return boolean 是否发送成功
	 */
	@Deprecated
	public static boolean sendText(String bot, String text) {
		Map<String, Object> item = new HashMap<>();
		item.put("msgtype", "text");
		HashMap<String, String> textMap = new HashMap<>();
		textMap.put("content",text);
		item.put("text",textMap);
		String data = JSON.toJSONString(item);
		try {
			HttpUtils.post(bot,data);
			return true;
		} catch (IOException e) {
			log.error("微信文本消息发送失败:", e);
			return false;
		}
	}

	/**
	 * 发送markdown消息，支持的markdown格式见企业微信说明
	 * @author zhuxu
	 * @date 2022/3/4
	 * @param bot 机器人链接
	 * @param markdown 内容
	 * @return boolean 是否发送成功
	 */
	public static boolean sendMarkdown(String bot, String markdown) {
		Map<String, Object> item = new HashMap<>();
		item.put("msgtype", "markdown");
		HashMap<String, String> textMap = new HashMap<>();
		textMap.put("content",markdown);
		item.put("markdown",textMap);
		String data = JSON.toJSONString(item);
		try {
			HttpUtils.post(bot, data);
			return true;
		} catch (IOException e) {
			log.error("微信markdown消息发送失败:", e);
			return false;
		}
	}

	/**
	 * 企业微信群@所有人
	 * @author zhuxu
	 * @date 2022/3/4
	 * @param bot 群机器人链接
	 * @return boolean 是否发送成功
	 */
	public static boolean atAll(String bot) {
    	
		Map<String, Object> item = new HashMap<>();
		item.put("msgtype", "text");
		Map<String, Object> textMap = new HashMap<>();
		textMap.put("content", "");
		textMap.put("mentioned_mobile_list", Arrays.asList("@all"));
		Map<String, Object> atMap = new HashMap<>();
		atMap.put("isAtAll", true);
		item.put("text", textMap);
		item.put("at", atMap);
		String data = JSON.toJSONString(item);
		try {
			HttpUtils.post(bot, data);
			return true;
		} catch (IOException e) {
			log.error("微信@all发送失败:", e);
			return false;
		}
	}

	/**
	 * @一些人
	 * @author zhuxu
	 * @date 2022/3/4
	 * @param bot 机器人hook链接
	 * @param name 人员名称列表如：zhangsan01、lisi03
	 * @return void
	 */
	public static boolean atMembers(String bot, List<String> name) {

		Map<String, Object> item = new HashMap<>();
		item.put("msgtype", "text");
		Map<String, Object> textMap = new HashMap<>();
		textMap.put("content", "");
		textMap.put("mentioned_list", name);
		item.put("text", textMap);
		String data = JSON.toJSONString(item);
		try {
			HttpUtils.post(bot, data);
			return true;
		} catch (IOException e) {
			log.error("微信@发送失败:", name);
			return false;
		}
	}

	/**
	 * 根据给定的人员列表和手机号列表@一些人
	 * @author zhuxu
	 * @date 2022/3/21
	 * @param bot 微信机器人链接
	 * @param name 人员拼音列表
	 * @param phone 人员电话列表
	 * @return boolean
	 */
	public static boolean atMembers(String bot, List<String> name, List<String> phone) {

		Map<String, Object> item = new HashMap<>();
		item.put("msgtype", "text");
		Map<String, Object> textMap = new HashMap<>();
		textMap.put("content", "");
		textMap.put("mentioned_list", name);
		textMap.put("mentioned_mobile_list", phone);
		item.put("text", textMap);
		String data = JSON.toJSONString(item);
		try {
			HttpUtils.post(bot, data);
			return true;
		} catch (IOException e) {
			log.error("微信@发送失败:{}{}", name, phone);
			return false;
		}
	}


	/**
	 * @一些人
	 * @author zhuxu
	 * @date 2022/3/21
	 * @param bot 机器人hook链接
	 * @param phone 手机号列表
	 * @return void
	 */
	public static boolean atMembersByPhone(String bot, List<String> phone) {

		Map<String, Object> item = new HashMap<>();
		item.put("msgtype", "text");
		Map<String, Object> textMap = new HashMap<>();
		textMap.put("content", "");
		textMap.put("mentioned_mobile_list", phone);
		item.put("text", textMap);
		String data = JSON.toJSONString(item);
		try {
			HttpUtils.post(bot, data);
			return true;
		} catch (IOException e) {
			log.error("微信@发送失败:", phone);
			return false;
		}
	}


}
