package com.zxl.chatbase.wxroboot.webhook.util;

import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * http请求工具类
 * 异常未处理
 * @author 史明达
 */
public class HttpUtils {
    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);
    private HttpUtils(){}

    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
            .setConnectTimeout(3000).setConnectionRequestTimeout(1000)
                    .setSocketTimeout(2000).build();
    /**
     * 发送 get 请求
     *
     * @param url 请求地址
     * @return String 请求结果的字符串
     */
    public static String get(String url) throws IOException{
        String result = null;
        CloseableHttpResponse response = null;
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            // 创建http GET请求
            HttpGet httpGet = new HttpGet(url);
            //设置超时时间
            httpGet.setConfig(REQUEST_CONFIG);
            // 执行请求
            response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        } finally {
            try {
                if (response != null)
                    response.close();
            } catch (IOException e) {
                log.error("GET 连接关闭失败：{}",e.getMessage());
            }
            try {
                if (httpclient != null)
                    httpclient.close();
            } catch (IOException e) {
                log.error("GET HttpClient 关闭失败：{}",e.getMessage());
            }
        }
       return result;
    }
 
    /**
     * 发送 post 请求
     *
     * @param url     请求地址
     * @param jsonStr Form表单json字符串
     * @return String 请求结果的字符串
     */
    public static String post(String url, String jsonStr) throws IOException{
        // 创建httpClient
        CloseableHttpClient httpClient = HttpClients.createDefault();
        // 创建post请求方式实例
        HttpPost httpPost = new HttpPost(url);
        // 设置请求头 发送的是json数据格式
        httpPost.setHeader("Content-type", "application/json;charset=utf-8");
        //httpPost.setHeader("Connection", "Close");
        httpPost.setConfig(REQUEST_CONFIG);

        // 设置参数---设置消息实体 也就是携带的数据
        StringEntity entity = new StringEntity(jsonStr, StandardCharsets.UTF_8);
        // 设置编码格式
        entity.setContentEncoding(Constants.UTF_8);
        // 发送Json格式的数据请求
        entity.setContentType("application/json");
        // 把请求消息实体塞进去
        httpPost.setEntity(entity);
 
        // 执行http的post请求
        CloseableHttpResponse httpResponse = null;
        String result;
        
        try {
            log.info("httpPost:"+ httpPost);
            httpResponse = httpClient.execute(httpPost);
            log.info("httpResponse:"+httpResponse.toString());
            result = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
            log.info("result:"+result);
        } finally {
            try {
                if (httpResponse != null)
                    httpResponse.close();
            } catch (IOException e) {
                log.error("POST 连接关闭失败：{}", e.getMessage());
            }
            try {
                if (httpClient != null)
                    httpClient.close();
            } catch (IOException e) {
                log.error("POST HttpClient 关闭失败：{}", e.getMessage());
            }
        }
        return result;
    }

    public static String postFormData(String url, String data) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
        httpPost.setConfig(REQUEST_CONFIG);
        StringEntity entity = new StringEntity(data, StandardCharsets.UTF_8);
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/x-www-form-urlencoded");
        httpPost.setEntity(entity);
        CloseableHttpResponse httpResponse = null;

        String result;
        try {
            log.info("httpPost:" + httpPost);
            httpResponse = httpClient.execute(httpPost);
            log.info("httpResponse:" + httpResponse.toString());
            result = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
            log.info("result:" + result);
        } finally {
            try {
                if (httpResponse != null) {
                    httpResponse.close();
                }
            } catch (IOException var13) {
                log.error("POST 连接关闭失败：{}", var13.getMessage());
            }
            try {
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException var13) {
                log.error("POST HttpClient 关闭失败：{}", var13.getMessage());
            }
        }

        return result;
    }
}
