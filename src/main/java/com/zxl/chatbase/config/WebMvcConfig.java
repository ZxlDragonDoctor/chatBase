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
            "/api/user/register",
            "/api/chat/**",
            "/api/upload/**",
            "/api/uploads/**",
            "/api/feedback/submit",
            "/api/feedback/user/**",
            "/qq/**",
            "/intellrobot/**",
            "/error",
            "/uploads/**"
    );

    private static final List<String> ADMIN_PATHS = Arrays.asList(
            "/api/feedback/page",
            "/api/feedback/*/reply",
            "/api/feedback/*/status",
            "/api/feedback/stats",
            "/api/user/list",
            "/api/user/*/detail",
            "/api/user/*/role",
            "/api/user/*/status",
            "/api/user/*/remove",
            "/api/kb/app/admin/**",
            "/api/kb/admin/**"
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