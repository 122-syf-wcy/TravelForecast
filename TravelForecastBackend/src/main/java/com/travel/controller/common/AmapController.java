package com.travel.controller.common;

import com.travel.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * 高德地图API代理控制器
 * 用于安全地调用高德地图服务，避免API Key暴露在前端
 */
@RestController
@RequestMapping("/amap")
@CrossOrigin(origins = "*")
public class AmapController {

    private static final Logger logger = LoggerFactory.getLogger(AmapController.class);
    
    // 高德地图API Key（配置在application.yml中）
    @Value("${amap.api.key:339146bdb9038c3caf85a7aca9c9bb7f}")
    private String amapApiKey;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * 驾车路径规划
     * @param origin 起点坐标 "lng,lat"
     * @param destination 终点坐标 "lng,lat"
     * @param waypoints 途经点（可选）多个坐标用;分隔
     * @param strategy 路线策略（可选）默认10：躲避拥堵，路程较短
     * @return 高德地图API返回的路径规划结果
     */
    @GetMapping("/driving")
    public Result<?> getDrivingRoute(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam(required = false) String waypoints,
            @RequestParam(defaultValue = "10") Integer strategy
    ) {
        try {
            logger.info("驾车路径规划请求 - 起点: {}, 终点: {}, 途经点: {}, 策略: {}", 
                       origin, destination, waypoints, strategy);
            
            // 构建高德地图API URL
            StringBuilder url = new StringBuilder("https://restapi.amap.com/v3/direction/driving?");
            url.append("key=").append(amapApiKey);
            url.append("&origin=").append(origin);
            url.append("&destination=").append(destination);
            url.append("&extensions=all"); // 返回详细信息
            url.append("&strategy=").append(strategy);
            
            if (waypoints != null && !waypoints.isEmpty()) {
                url.append("&waypoints=").append(waypoints);
            }
            
            logger.debug("调用高德地图API: {}", url.toString().replace(amapApiKey, "***"));
            
            // 调用高德地图API
            ResponseEntity<String> response = restTemplate.getForEntity(url.toString(), String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("路径规划成功");
                return Result.success(response.getBody());
            } else {
                logger.error("高德地图API返回错误状态码: {}", response.getStatusCode());
                return Result.error("路径规划失败");
            }
            
        } catch (Exception e) {
            logger.error("驾车路径规划失败", e);
            return Result.error("路径规划服务异常: " + e.getMessage());
        }
    }
    
    /**
     * 步行路径规划
     * @param origin 起点坐标 "lng,lat"
     * @param destination 终点坐标 "lng,lat"
     * @return 高德地图API返回的路径规划结果
     */
    @GetMapping("/walking")
    public Result<?> getWalkingRoute(
            @RequestParam String origin,
            @RequestParam String destination
    ) {
        try {
            logger.info("步行路径规划请求 - 起点: {}, 终点: {}", origin, destination);
            
            // 构建高德地图API URL
            String url = String.format(
                "https://restapi.amap.com/v3/direction/walking?key=%s&origin=%s&destination=%s",
                amapApiKey, origin, destination
            );
            
            logger.debug("调用高德地图API: {}", url.replace(amapApiKey, "***"));
            
            // 调用高德地图API
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("步行路径规划成功");
                return Result.success(response.getBody());
            } else {
                logger.error("高德地图API返回错误状态码: {}", response.getStatusCode());
                return Result.error("路径规划失败");
            }
            
        } catch (Exception e) {
            logger.error("步行路径规划失败", e);
            return Result.error("路径规划服务异常: " + e.getMessage());
        }
    }
    
    /**
     * 公交路径规划
     * @param origin 起点坐标 "lng,lat"
     * @param destination 终点坐标 "lng,lat"
     * @param city 城市名称或城市编码
     * @param strategy 公交策略 0:最快捷 1:最经济 2:最少换乘 3:最少步行 5:不乘地铁
     * @return 高德地图API返回的路径规划结果
     */
    @GetMapping("/transit")
    public Result<?> getTransitRoute(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam(defaultValue = "六盘水") String city,
            @RequestParam(defaultValue = "0") Integer strategy
    ) {
        try {
            logger.info("公交路径规划请求 - 起点: {}, 终点: {}, 城市: {}, 策略: {}", 
                       origin, destination, city, strategy);
            
            // 构建高德地图API URL
            String url = String.format(
                "https://restapi.amap.com/v3/direction/transit/integrated?key=%s&origin=%s&destination=%s&city=%s&strategy=%d&extensions=all",
                amapApiKey, origin, destination, city, strategy
            );
            
            logger.debug("调用高德地图API: {}", url.replace(amapApiKey, "***"));
            
            // 调用高德地图API
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("公交路径规划成功");
                return Result.success(response.getBody());
            } else {
                logger.error("高德地图API返回错误状态码: {}", response.getStatusCode());
                return Result.error("路径规划失败");
            }
            
        } catch (Exception e) {
            logger.error("公交路径规划失败", e);
            return Result.error("路径规划服务异常: " + e.getMessage());
        }
    }
}

