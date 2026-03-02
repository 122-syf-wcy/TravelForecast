package com.travel.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 网关健康检查控制器
 * 
 * 提供网关自身健康状态和下游服务聚合健康检查
 * - /health           网关自身健康
 * - /health/services  所有下游服务健康聚合
 */
@RestController
public class HealthCheckController {

    private final WebClient webClient = WebClient.builder()
            .defaultHeaders(h -> h.set("Accept", "application/json"))
            .build();

    @Value("${spring.cloud.gateway.routes[0].uri:http://localhost:8080}")
    private String businessServiceUrl;

    @Value("${spring.cloud.gateway.routes[1].uri:http://localhost:8081}")
    private String aiServiceUrl;

    @Value("${spring.cloud.gateway.routes[2].uri:http://localhost:8082}")
    private String miniprogramServiceUrl;

    @Value("${spring.cloud.gateway.routes[3].uri:http://localhost:8001}")
    private String predictionServiceUrl;

    @Value("${spring.cloud.gateway.routes[4].uri:http://localhost:8000}")
    private String digitalHumanServiceUrl;

    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("service", "travel-gateway");
        result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return Mono.just(ResponseEntity.ok(result));
    }

    @GetMapping("/health/services")
    public Mono<ResponseEntity<Map<String, Object>>> servicesHealth() {
        Mono<String> business = checkService(businessServiceUrl, "business-service");
        Mono<String> ai = checkService(aiServiceUrl, "ai-service");
        Mono<String> miniprogram = checkService(miniprogramServiceUrl, "miniprogram-service");
        Mono<String> prediction = checkService(predictionServiceUrl, "prediction-service");
        Mono<String> digitalHuman = checkService(digitalHumanServiceUrl, "digital-human-service");

        return Mono.zip(business, ai, miniprogram, prediction, digitalHuman)
                .map(tuple -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("gateway", "UP");
                    result.put("business-service", tuple.getT1());
                    result.put("ai-service", tuple.getT2());
                    result.put("miniprogram-service", tuple.getT3());
                    result.put("prediction-service", tuple.getT4());
                    result.put("digital-human-service", tuple.getT5());

                    boolean allUp = "UP".equals(tuple.getT1()) && "UP".equals(tuple.getT2())
                            && "UP".equals(tuple.getT3()) && "UP".equals(tuple.getT4())
                            && "UP".equals(tuple.getT5());
                    result.put("status", allUp ? "UP" : "DEGRADED");
                    result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    return ResponseEntity.ok(result);
                });
    }

    private Mono<String> checkService(String baseUrl, String serviceName) {
        return webClient.get()
                .uri(baseUrl + "/actuator/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(3))
                .map(body -> "UP")
                .onErrorResume(e -> {
                    // fallback: try root path
                    return webClient.get()
                            .uri(baseUrl + "/")
                            .retrieve()
                            .bodyToMono(String.class)
                            .timeout(Duration.ofSeconds(2))
                            .map(body -> "UP")
                            .onErrorReturn("DOWN");
                });
    }
}
