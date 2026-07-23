package com.zxl.chatbase.wxroboot.webhook.util;

import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * http请求工具类
 * @author 史明达
 */
@Component
public class HttpUtils {
    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);
    private HttpUtils(){}

    private static CloseableHttpClient sharedHttpClient;

    @Autowired
    public void setHttpClient(CloseableHttpClient httpClient) {
        HttpUtils.sharedHttpClient = httpClient;
    }

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
        try {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setConfig(REQUEST_CONFIG);
            response = sharedHttpClient.execute(httpGet);
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
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Content-type", "application/json;charset=utf-8");
        httpPost.setConfig(REQUEST_CONFIG);

        StringEntity entity = new StringEntity(jsonStr, StandardCharsets.UTF_8);
        entity.setContentEncoding(Constants.UTF_8);
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
 
        CloseableHttpResponse httpResponse = null;
        String result;
        
        try {
            httpResponse = sharedHttpClient.execute(httpPost);
            result = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
        } finally {
            try {
                if (httpResponse != null)
                    httpResponse.close();
            } catch (IOException e) {
                log.error("POST 连接关闭失败：{}", e.getMessage());
            }
        }
        return result;
    }

    public static String postFormData(String url, String data) throws IOException {
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
            httpResponse = sharedHttpClient.execute(httpPost);
            result = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
        } finally {
            try {
                if (httpResponse != null) {
                    httpResponse.close();
                }
            } catch (IOException e) {
                log.error("POST 连接关闭失败：{}", e.getMessage());
            }
        }

        return result;
    }
}
