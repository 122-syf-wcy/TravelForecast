package com.travel.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 游韵华章 - API网关服务
 * 
 * 功能：
 * 1. 统一入口：所有微服务请求通过网关路由
 * 2. 统一认证：JWT Token验证
 * 3. 限流熔断：保护后端服务
 * 4. 请求日志：记录所有API调用
 * 5. CORS处理：跨域支持
 * 
 * @author Travel Gateway Team
 * @version 1.0.0
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("""
            
            ╔══════════════════════════════════════════════════════════╗
            ║     游韵华章 - API网关服务 启动成功                        ║
            ║     Travel Forecast Gateway Service Started             ║
            ╚══════════════════════════════════════════════════════════╝
            
            📍 网关地址: http://localhost:8888
            📚 API文档: http://localhost:8888/actuator/gateway/routes
            🔍 健康检查: http://localhost:8888/actuator/health
            
            """);
    }
}

