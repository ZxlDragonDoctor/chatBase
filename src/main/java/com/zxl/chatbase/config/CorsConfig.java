package com.zxl.chatbase.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 仅显式白名单；生产域名通过 CORS_ALLOWED_ORIGINS 注入（逗号分隔）
                // 禁止 allowedOriginPatterns("*") + allowCredentials(true) 的组合
                .allowedOrigins(resolveOrigins())
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("Authorization", "Content-Type", "X-Requested-With")
                .exposedHeaders("Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
    }

    private String[] resolveOrigins() {
        String env = System.getenv("CORS_ALLOWED_ORIGINS");
        if (env != null && !env.isBlank()) {
            return java.util.Arrays.stream(env.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
        }
        // 默认本地开发源
        return new String[]{
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost",
                "http://127.0.0.1"
        };
    }
}

