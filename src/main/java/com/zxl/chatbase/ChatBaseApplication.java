package com.zxl.chatbase;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.HashMap;

@SpringBootApplication
@MapperScan("com.zxl.chatbase.**.mapper")
@EnableScheduling
public class ChatBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatBaseApplication.class, args);
        HashMap<Object, Object> objectObjectHashMap = new HashMap<>();

    }

}
