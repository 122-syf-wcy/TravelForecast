package com.travel.gateway.controller;

import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网关健康检查控制器。
 *
 * 原实现通过 `spring.cloud.gateway.routes[N].uri` 数组索引解析下游地址，
 * 一旦调整路由顺序就会"错位"。改为通过 {@link RouteDefinitionLocator}
 * 按 route id 取出 URI，保证下游地址始终来自运行中的路由表。
 */
@RestController
public class HealthCheckController {

    private static final Map<String, String> SERVICE_ROUTES = Map.of(
            "business-service", "business-service",
            "ai-service", "ai-service",
            "miniprogram-service", "miniprogram-service",
            "prediction-service", "prediction-service",
            "digital-human-service", "digital-human-service"
    );

    private final WebClient webClient = WebClient.builder()
            .defaultHeaders(headers -> headers.set("Accept", "application/json"))
            .build();

    private final RouteDefinitionLocator routeDefinitionLocator;

    public HealthCheckController(RouteDefinitionLocator routeDefinitionLocator) {
        this.routeDefinitionLocator = routeDefinitionLocator;
    }

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
        Flux<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions().cache();

        return routes
                .filter(r -> SERVICE_ROUTES.containsKey(r.getId()))
                .flatMap(r -> checkService(r).map(status -> Map.entry(r.getId(), status)))
                .collectList()
                .map(entries -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("gateway", "UP");
                    Map<String, String> serviceStatus = entries.stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
                    result.putAll(serviceStatus);
                    boolean allUp = serviceStatus.values().stream().allMatch("UP"::equals);
                    result.put("status", allUp ? "UP" : "DEGRADED");
                    result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    return ResponseEntity.ok(result);
                });
    }

    private Mono<String> checkService(RouteDefinition route) {
        URI uri = route.getUri();
        String baseUrl = uri == null ? null : uri.toString();
        if (baseUrl == null) {
            return Mono.just("DOWN");
        }
        String actuatorUrl = baseUrl + "/actuator/health";
        return webClient.get()
                .uri(actuatorUrl)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(3))
                .map(body -> "UP")
                .onErrorResume(e -> webClient.get()
                        .uri(baseUrl + "/health")
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(2))
                        .map(body -> "UP")
                        .onErrorReturn("DOWN"));
    }
}
