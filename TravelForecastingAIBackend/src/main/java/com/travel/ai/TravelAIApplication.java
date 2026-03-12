package com.travel.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智教黔行 - AI智能服务 主启动类
 * 
 * 提供：智能聊天、行程规划、研学教育、知识库检索、语音服务代理
 * 端口：8081
 * 
 * @author Travel AI Team
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class TravelAIApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelAIApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("  智教黔行 · AI智能服务 启动成功!");
        System.out.println("  端口: 8081");
        System.out.println("  文档: http://localhost:8081/ai-api/swagger-ui.html");
        System.out.println("========================================\n");
    }
}
