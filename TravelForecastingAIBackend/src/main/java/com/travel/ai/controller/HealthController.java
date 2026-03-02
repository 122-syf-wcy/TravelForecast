package com.travel.ai.controller;

import com.travel.ai.client.BusinessClient;
import com.travel.ai.client.DigitalHumanClient;
import com.travel.ai.client.PredictionClient;
import com.travel.ai.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 展示AI服务及其依赖服务的状态
 */
@RestController
@Tag(name = "健康检查", description = "服务状态监控")
public class HealthController {

    @Autowired
    private DigitalHumanClient digitalHumanClient;

    @Autowired
    private PredictionClient predictionClient;

    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public Result<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("service", "travel-ai-service");
        status.put("status", "UP");
        status.put("timestamp", LocalDateTime.now().toString());

        // 检查依赖服务
        Map<String, Object> dependencies = new LinkedHashMap<>();
        dependencies.put("digital-human", digitalHumanClient.isAvailable() ? "UP" : "DOWN");
        dependencies.put("prediction-service", predictionClient.isAvailable() ? "UP" : "DOWN");
        status.put("dependencies", dependencies);

        return Result.success(status);
    }
}
