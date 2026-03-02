package com.travel.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关配置属性
 * 从application.yml中读取配置
 * 
 * @author Travel Gateway Team
 */
@Data
@Component("travelGatewayProperties")
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    /**
     * 认证白名单
     */
    private List<String> whitelist = new ArrayList<>();

    /**
     * 限流配置
     */
    private RateLimit rateLimit = new RateLimit();

    /**
     * 日志配置
     */
    private Log log = new Log();

    /**
     * JWT配置
     */
    private Jwt jwt = new Jwt();

    @Data
    public static class RateLimit {
        private boolean enabled = true;
        private int defaultRate = 100;
        private int defaultBurst = 200;
    }

    @Data
    public static class Log {
        private boolean enabled = true;
        private boolean logRequest = true;
        private boolean logResponse = false;
    }

    @Data
    public static class Jwt {
        private String secret = "TravelPredictionSecretKey2025LiupanshuiTourismSystem";
        private long expiration = 604800; // 7天，单位：秒
        private String tokenPrefix = "Bearer";
        private String header = "Authorization";
    }
}

