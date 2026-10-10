package com.zxl.chatbase.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AdminInterceptor adminInterceptor;

    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/api/health",
            "/api/user/login",
            "/api/user/register", // 保留路径但仅返回「已关闭」提示，不再放行业务
            // 仅放行 ChatController 的问答/上传端点；会话管理 /api/chat/session/** 必须鉴权
            "/api/chat/ask",
            "/api/chat/message",
            "/api/chat/web",
            "/api/chat/web/session",
            "/api/chat/im",
            "/api/chat/v1/files/**",
            "/api/upload/**",
            "/api/uploads/**",
            "/api/feedback/submit",
            "/qq/**",
            "/intellrobot/**",
            "/opencode/bridge",
            "/error",
            "/uploads/**"
    );

    private static final List<String> ADMIN_PATHS = Arrays.asList(
            "/api/feedback/page",
            "/api/feedback/*/reply",
            "/api/feedback/*/status",
            "/api/feedback/stats",
            "/api/user/list",
            "/api/user/create",
            "/api/user/*/detail",
            "/api/user/*/role",
            "/api/user/*/status",
            "/api/user/*/remove",
            "/api/kb/app/admin/**",
            "/api/kb/admin/**",
            "/api/opencode/**",
            "/api/console/conversations/*/opencode-node"
    );

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(EXCLUDE_PATHS);
        
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns(ADMIN_PATHS);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
        registry.addResourceHandler("/api/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
