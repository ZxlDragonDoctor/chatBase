package com.zxl.chatbase;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@SpringBootTest
class ChatBaseApplicationTests {
    @Test
    void testDifyApi() {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("https://api.dify.ai/v1/chat-messages");
        httpPost.setHeader("Authorization", "Bearer app-x4pDm1oSxDVu9fRLChTGdoFi");
        httpPost.setHeader("Content-Type", "application/json");
        String test = ("{\n  \"inputs\": {\n    \"name\": \"dify\"\n  },\n  \"query\": " +
                "\"iPhone 13 Pro Max 的规格是什么？\",\n  \"response_mode\": \"blocking\",\n  " +
                "\"user\": " +
                "\"abc-123\",\n  \"files\": [\n    {\n      \"type\": \"image\",\n     " +
                " \"transfer_method\": \"remote_url\",\n      " +
                "\"url\": \"https://cloud.dify.ai/logo/logo-site.png\"\n    }\n  ]\n}");
         httpPost.setEntity(new StringEntity(test, StandardCharsets.UTF_8));
        try (CloseableHttpResponse response = httpClient.execute(httpPost)){
            String respnoseString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            System.out.println(respnoseString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



}
