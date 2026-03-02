package com.travel.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * DashScope（通义千问）配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "dashscope")
public class DashScopeConfig {

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 默认聊天模型
     */
    private String defaultModel = "qwen-plus";

    /**
     * 行程规划模型 (更快)
     */
    private String planningModel = "qwen-turbo";

    /**
     * 最大Token数（降低=更快响应）
     */
    private Integer maxTokens = 1024;

    /**
     * 温度 (降低=更快收敛、更稳定)
     */
    private Double temperature = 0.5;

    /**
     * 获取有效的API Key (优先环境变量)
     */
    public String getEffectiveApiKey() {
        String envKey = System.getenv("DASHSCOPE_API_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            return envKey;
        }
        return apiKey;
    }
}
