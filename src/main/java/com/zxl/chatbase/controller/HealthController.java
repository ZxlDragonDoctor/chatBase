package com.zxl.chatbase.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());

        Map<String, Object> checks = new HashMap<>();

        if (dataSource == null) {
            checks.put("mysql", "DOWN");
        } else {
            try (Connection conn = dataSource.getConnection()) {
                checks.put("mysql", conn.isValid(3) ? "UP" : "DOWN");
            } catch (Exception e) {
                checks.put("mysql", "DOWN");
            }
        }

        if (redisTemplate == null || redisTemplate.getConnectionFactory() == null) {
            checks.put("redis", "DOWN");
        } else {
            org.springframework.data.redis.connection.RedisConnection redisConn = null;
            try {
                redisConn = redisTemplate.getConnectionFactory().getConnection();
                redisConn.ping();
                checks.put("redis", "UP");
            } catch (Exception e) {
                checks.put("redis", "DOWN");
            } finally {
                if (redisConn != null) {
                    try {
                        redisConn.close();
                    } catch (Exception ignore) {
                        // ignore
                    }
                }
            }
        }

        result.put("checks", checks);
        boolean allUp = checks.values().stream().allMatch(v -> v.equals("UP"));
        result.put("success", allUp);
        return result;
    }
}
