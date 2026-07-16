package com.travel.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 熔断降级控制器。
 * 路由 filter 中配置的 fallbackUri 会在后端超时/异常时转发到这里，
 * 统一返回 JSON 错误体，避免把 Gateway 内部异常暴露给前端。
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/business")
    public Mono<ResponseEntity<Map<String, Object>>> businessFallback() {
        return Mono.just(buildResponse("business-service", "主业务服务暂不可用，稍后再试"));
    }

    @RequestMapping("/ai")
    public Mono<ResponseEntity<Map<String, Object>>> aiFallback() {
        return Mono.just(buildResponse("ai-service", "AI 智能服务暂不可用，已为您切换到离线响应"));
    }

    @RequestMapping("/prediction")
    public Mono<ResponseEntity<Map<String, Object>>> predictionFallback() {
        return Mono.just(buildResponse("prediction-service", "客流预测服务暂不可用，展示缓存结果"));
    }

    /** 网关自身降级兜底，预留给后续自定义规则 */
    @GetMapping
    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> defaultFallback() {
        return Mono.just(buildResponse("gateway", "服务暂时不可用"));
    }

    private ResponseEntity<Map<String, Object>> buildResponse(String service, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 503);
        body.put("message", message);
        body.put("service", service);
        body.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
