package com.zxl.chatbase.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/api/user/login",
            "/api/user/register",
            "/api/chat/**",
            "/api/statistics/**",
            "/api/console/**",
            "/api/kb/**",
            "/qq/**",
            "/intellrobot/**",
            "/error"
    );

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(EXCLUDE_PATHS);
    }
}