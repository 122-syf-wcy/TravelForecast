package com.travel.service.common;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 高德地图服务接口
 */
public interface AmapService {
    
    /**
     * 异步获取路线规划信息
     * @param origin 起点坐标 "经度,纬度"
     * @param destination 终点坐标 "经度,纬度"
     * @return 包含距离(米)和时间(秒)的CompletableFuture
     */
    CompletableFuture<RouteInfo> getRouteInfoAsync(String origin, String destination);
    
    /**
     * 批量异步获取多个路线规划信息
     * @param routes 路线列表，每个元素为 [起点, 终点]
     * @return 路线信息列表的CompletableFuture
     */
    CompletableFuture<List<RouteInfo>> getBatchRouteInfoAsync(List<String[]> routes);
    
    /**
     * 路线信息
     */
    record RouteInfo(
        String origin,      // 起点
        String destination, // 终点
        Integer distance,   // 距离（米）
        Integer duration,   // 时间（秒）
        boolean success     // 是否成功
    ) {}
}

