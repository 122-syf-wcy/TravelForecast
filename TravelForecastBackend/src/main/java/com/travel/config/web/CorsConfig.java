package com.travel.config.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 跨域配置
 * 支持本地开发模式和生产部署模式
 * 
 * @author Travel Team
 * @date 2025-10-10
 */
@Configuration
public class CorsConfig {

    @Value("${app.env:local}")
    private String appEnv;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 根据环境决定允许的源
        List<String> allowedOrigins = new ArrayList<>();
        
        if ("local".equalsIgnoreCase(appEnv) || "dev".equalsIgnoreCase(appEnv)) {
            // 本地开发环境：允许多个本地端口
            allowedOrigins.addAll(Arrays.asList(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://localhost:8080",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:8080",
                "http://localhost:5175",  // Python 服务
                "http://127.0.0.1:5175"
            ));
        } else {
            // 生产环境：使用配置的前端地址
            allowedOrigins.add(frontendUrl);
            // 同时允许本地访问
            allowedOrigins.addAll(Arrays.asList(
                "http://localhost:3000",
                "http://127.0.0.1:3000"
            ));
        }
        
        config.setAllowedOrigins(allowedOrigins);
        
        // 允许的请求方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        
        // 允许的请求头
        config.addAllowedHeader("*");
        
        // 是否允许发送Cookie
        config.setAllowCredentials(true);
        
        // 预检请求的有效期（秒）
        config.setMaxAge(3600L);
        
        // 允许的响应头
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Content-Disposition");
        config.addExposedHeader("X-Total-Count");
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}

