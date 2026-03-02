package com.travel.service.common.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.service.common.AmapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 高德地图服务实现
 * 使用异步方式并行调用API，提升响应速度
 */
@Service
public class AmapServiceImpl implements AmapService {

    private static final Logger logger = LoggerFactory.getLogger(AmapServiceImpl.class);

    @Value("${amap.api.key:339146bdb9038c3caf85a7aca9c9bb7f}")
    private String amapApiKey;

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 异步获取单个路线信息
     */
    @Async("asyncExecutor")
    @Override
    public CompletableFuture<RouteInfo> getRouteInfoAsync(String origin, String destination) {
        try {
            String apiUrl = String.format(
                    "https://restapi.amap.com/v3/direction/driving?key=%s&origin=%s&destination=%s",
                    amapApiKey, origin, destination
            );

            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(response.getBody(), Map.class);

            if ("1".equals(data.get("status"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> route = (Map<String, Object>) data.get("route");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> paths = (List<Map<String, Object>>) route.get("paths");

                if (paths != null && !paths.isEmpty()) {
                    Map<String, Object> path = paths.get(0);
                    // 高德API返回的distance和duration是字符串类型，需要转换
                    Integer distance = parseInteger(path.get("distance"));
                    Integer duration = parseInteger(path.get("duration"));

                    logger.debug("路线查询成功: {} -> {}, {}m, {}s", origin, destination, distance, duration);
                    return CompletableFuture.completedFuture(
                            new RouteInfo(origin, destination, distance, duration, true)
                    );
                }
            }

            logger.warn("路线查询失败: {} -> {}", origin, destination);
            return CompletableFuture.completedFuture(
                    new RouteInfo(origin, destination, null, null, false)
            );

        } catch (Exception e) {
            logger.warn("路线查询异常: {} -> {}, {}", origin, destination, e.getMessage());
            return CompletableFuture.completedFuture(
                    new RouteInfo(origin, destination, null, null, false)
            );
        }
    }

    /**
     * 批量异步获取路线信息（并行处理）
     */
    @Override
    public CompletableFuture<List<RouteInfo>> getBatchRouteInfoAsync(List<String[]> routes) {
        List<CompletableFuture<RouteInfo>> futures = new ArrayList<>();

        for (String[] route : routes) {
            String origin = route[0];
            String destination = route[1];
            futures.add(getRouteInfoAsync(origin, destination));
        }

        // 等待所有异步任务完成
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }
    
    /**
     * 安全地将对象转换为Integer
     * 高德API返回的数值字段可能是String或Integer类型
     */
    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                logger.warn("无法解析数值: {}", value);
                return null;
            }
        }
        return null;
    }
}

