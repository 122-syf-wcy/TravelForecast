package com.travel.controller.prediction;

import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.travel.common.Result;
import com.travel.service.prediction.AiService;
import com.travel.service.common.AmapService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * AI智能行程规划控制器
 * 使用通义千问AI生成行程，结合高德地图API验证和优化
 */
/**
 * @deprecated 已迁移到 AI智能服务 (TravelForecastingAIBackend, Port 8081)
 * 前端已改为调用 /ai-api/ai-planning/generate
 * 此Controller保留作为备份，后续可安全删除
 */
@Deprecated
@RestController
@RequestMapping("/ai-planning-legacy")
@CrossOrigin(origins = "*")
public class AiPlanningController {
    
    private static final Logger logger = LoggerFactory.getLogger(AiPlanningController.class);
    
    @Autowired(required = false)
    private AiService aiService;
    
    @Autowired(required = false)
    private AmapService amapService; // 异步地图服务
    
    @Value("${dashscope.api-key:}")
    private String dashscopeApiKey;
    
    @Value("${amap.api.key:339146bdb9038c3caf85a7aca9c9bb7f}")
    private String amapApiKey;
    
    @Autowired
    private RestTemplate restTemplate; // 使用优化后的RestTemplate
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // AI响应超时时间（秒）- 行程规划需要更长的生成时间
    private static final int AI_TIMEOUT_SECONDS = 90;
    
    /**
     * 获取用户的行程规划列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getUserPlans(@RequestParam Long userId) {
        logger.info("获取用户行程列表, userId: {}", userId);
        // 目前没有持久化行程，返回空列表
        return Result.success(new ArrayList<>());
    }
    
    /**
     * 客流预测数据
     */
    public record FlowPredictionData(
        String date,
        Integer flow,
        String crowdLevel  // "low", "medium", "high"
    ) {}
    
    /**
     * AI智能行程规划请求
     */
    public record AiPlanningRequest(
        String startDate,           // 开始日期 "2025-10-16"
        String endDate,             // 结束日期 "2025-10-19"
        List<String> attractions,   // 景区列表 ["梅花山风景区", "乌蒙大草原"]
        String transportation,      // 出行方式 "car", "public", "tour"
        Integer peopleCount,        // 人数
        Integer budget,             // 预算（元）
        List<String> preferences,   // 偏好 ["自然风光", "历史文化"]
        Map<String, List<FlowPredictionData>> flowPredictions  // 各景区客流预测数据
    ) {}
    
    @Autowired(required = false)
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;
    
    private static final String PLAN_CACHE_PREFIX = "ai:plan:";
    private static final long PLAN_CACHE_TTL_HOURS = 2; // 行程规划缓存2小时
    
    /**
     * 生成行程规划的缓存key（只基于核心参数，不包含实时预测数据）
     */
    private String generatePlanCacheKey(AiPlanningRequest request) {
        // 只使用核心参数生成缓存key：日期、景区、人数、预算、交通方式
        String keySource = String.format("%s_%s_%s_%d_%d_%s",
            request.startDate(),
            request.endDate(),
            String.join(",", request.attractions()),
            request.peopleCount(),
            request.budget(),
            request.transportation()
        );
        
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(keySource.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return PLAN_CACHE_PREFIX + hexString.toString().substring(0, 16);
        } catch (Exception e) {
            return PLAN_CACHE_PREFIX + keySource.hashCode();
        }
    }
    
    /**
     * 生成AI行程规划
     */
    @PostMapping("/generate")
    public Result<?> generatePlan(@RequestBody AiPlanningRequest request) {
        try {
            logger.info("收到AI行程规划请求: {}", request);
            long startTime = System.currentTimeMillis();
            
            // 0. 检查缓存（基于核心参数）
            String cacheKey = generatePlanCacheKey(request);
            if (redisTemplate != null) {
                try {
                    Object cached = redisTemplate.opsForValue().get(cacheKey);
                    if (cached != null) {
                        logger.info("行程规划命中缓存 [key={}]", cacheKey);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> cachedPlan = (Map<String, Object>) cached;
                        return Result.success(cachedPlan);
                    }
                } catch (Exception e) {
                    logger.warn("读取缓存失败: {}", e.getMessage());
                }
            }
            
            // 1. 构建AI提示词
            String prompt = buildPrompt(request);
            logger.debug("AI提示词长度: {} 字符", prompt.length());
            
            // 2. 异步调用通义千问AI（带超时控制）
            CompletableFuture<String> aiFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return callAI(prompt);
                } catch (Exception e) {
                    throw new RuntimeException("AI调用失败: " + e.getMessage(), e);
                }
            });
            
            String aiResponse;
            try {
                // 等待AI响应，最多90秒
                aiResponse = aiFuture.get(AI_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                long aiDuration = System.currentTimeMillis() - startTime;
                logger.info("AI响应成功: {} 字符, 耗时 {}ms", aiResponse.length(), aiDuration);
            } catch (TimeoutException e) {
                logger.error("AI响应超时（超过{}秒）", AI_TIMEOUT_SECONDS);
                return Result.error("AI响应超时，请稍后重试");
            }
            
            // 3. 解析AI响应
            Map<String, Object> plan = parseAndOptimizePlan(aiResponse, request);
            
            // 4. 缓存结果
            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set(cacheKey, plan, PLAN_CACHE_TTL_HOURS, TimeUnit.HOURS);
                    logger.info("行程规划已缓存 [key={}, ttl={}h]", cacheKey, PLAN_CACHE_TTL_HOURS);
                } catch (Exception e) {
                    logger.warn("缓存行程规划失败: {}", e.getMessage());
                }
            }
            
            // 5. 异步优化路线（不阻塞响应）
            if (amapService != null) {
                optimizeRouteAsync(plan);
            }
            
            long totalDuration = System.currentTimeMillis() - startTime;
            logger.info("AI行程规划完成，总耗时: {}ms", totalDuration);
            
            return Result.success(plan);
            
        } catch (Exception e) {
            logger.error("AI行程规划失败", e);
            return Result.error("生成行程失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建AI提示词（优化版：包含客流预测数据，生成详细行程，严格控制预算）
     */
    private String buildPrompt(AiPlanningRequest request) {
        int days = calculateDays(request.startDate(), request.endDate());
        String transportDesc = getTransportDescription(request.transportation());
        int totalBudget = request.budget();
        int peopleCount = request.peopleCount();
        
        // 计算每日预算和各项预算
        int dailyBudget = totalBudget / days;
        int ticketBudget = (int)(totalBudget * 0.30);
        int foodBudget = (int)(totalBudget * 0.25);
        int transportBudget = (int)(totalBudget * 0.20);
        int hotelBudget = (int)(totalBudget * 0.20);
        int otherBudget = (int)(totalBudget * 0.05);
        
        // 构建客流预测信息
        StringBuilder flowInfo = new StringBuilder();
        if (request.flowPredictions() != null && !request.flowPredictions().isEmpty()) {
            flowInfo.append("\n【客流预测数据】根据双流融合预测模型分析：\n");
            for (Map.Entry<String, List<FlowPredictionData>> entry : request.flowPredictions().entrySet()) {
                String spotName = entry.getKey();
                List<FlowPredictionData> predictions = entry.getValue();
                if (predictions != null && !predictions.isEmpty()) {
                    flowInfo.append("- ").append(spotName).append("：");
                    for (int i = 0; i < predictions.size(); i++) {
                        FlowPredictionData pred = predictions.get(i);
                        String levelCn = switch (pred.crowdLevel()) {
                            case "high" -> "拥挤";
                            case "medium" -> "适中";
                            default -> "舒适";
                        };
                        flowInfo.append("第").append(i + 1).append("天约").append(pred.flow()).append("人(").append(levelCn).append(") ");
                    }
                    flowInfo.append("\n");
                }
            }
            flowInfo.append("请根据预测数据安排游览时段，拥挤时段安排下午游览。\n\n");
        }
        
        return String.format("""
            生成六盘水%d日游行程，%d人%s，总预算%d元（必须严格控制在此预算内）。
            
            景区：%s
            %s
            【预算分配（必须严格遵守）】
            - 总预算：%d元，每日约%d元
            - 门票预算：%d元（景区门票需×人数%d）
            - 餐饮预算：%d元（每餐人均30-50元）
            - 交通预算：%d元
            - 住宿预算：%d元（每晚%d元左右）
            - 其他：%d元
            
            【景区门票】梅花山¥60/人、玉舍¥40/人、乌蒙¥65/人、明湖免费、水城古镇免费
            
            直接输出JSON：
            {"title":"六盘水%d日游","summary":"概述","totalBudget":%d,"budgetBreakdown":{"ticket":%d,"food":%d,"transportation":%d,"accommodation":%d,"other":%d},"itinerary":[{"day":1,"date":"%s","activities":[{"time":"08:00","title":"活动","description":"描述","type":"scenic/food/hotel/transit","duration":"时长","cost":费用数字,"location":"地点"}]}],"tips":["建议"]}
            
            要求：
            1. 每天6-8个活动，每个活动cost字段必须填写具体数字
            2. 所有活动cost加起来必须等于totalBudget（%d元）
            3. 门票费用=单价×人数%d
            4. 根据客流预测安排游览时段
            """,
            days, peopleCount, transportDesc, totalBudget,
            String.join("、", request.attractions()),
            flowInfo.toString(),
            totalBudget, dailyBudget,
            ticketBudget, peopleCount,
            foodBudget,
            transportBudget,
            hotelBudget, hotelBudget / (days > 1 ? days - 1 : 1),
            otherBudget,
            days, totalBudget, ticketBudget, foodBudget, transportBudget, hotelBudget, otherBudget,
            request.startDate(),
            totalBudget, peopleCount
        );
    }
    
    /**
     * 调用通义千问AI
     */
    private String callAI(String prompt) throws Exception {
        if (aiService == null) {
            throw new Exception("AI服务未启用");
        }
        
        String apiKey = dashscopeApiKey != null && !dashscopeApiKey.isEmpty()
            ? dashscopeApiKey
            : System.getenv("DASHSCOPE_API_KEY");
        
        if (apiKey == null || apiKey.isEmpty()) {
            throw new Exception("未配置通义千问API Key");
        }
        
        List<Message> messages = new ArrayList<>();
        messages.add(Message.builder()
            .role(Role.USER.getValue())
            .content(prompt)
            .build());
        
        // 使用qwen-turbo模型（更快，适合结构化输出）
        String model = "qwen-turbo";
        return aiService.chat(model, messages, apiKey);
    }
    
    /**
     * 解析AI响应并使用高德地图API验证优化
     */
    private Map<String, Object> parseAndOptimizePlan(String aiResponse, AiPlanningRequest request) throws Exception {
        // 1. 清理AI响应（去除可能的markdown标记和其他干扰字符）
        String jsonStr = aiResponse.trim();
        
        // 移除markdown代码块标记
        if (jsonStr.startsWith("```json")) {
            jsonStr = jsonStr.substring(7);
        }
        if (jsonStr.startsWith("```")) {
            jsonStr = jsonStr.substring(3);
        }
        if (jsonStr.endsWith("```")) {
            jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
        }
        
        // 查找第一个{和最后一个}，提取纯JSON部分
        int firstBrace = jsonStr.indexOf('{');
        int lastBrace = jsonStr.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            jsonStr = jsonStr.substring(firstBrace, lastBrace + 1);
        }
        
        jsonStr = jsonStr.trim();
        
        // 移除JSON中的控制字符和零宽字符
        jsonStr = jsonStr.replaceAll("[\\u0000-\\u001F\\u007F-\\u009F\\u200B-\\u200D\\uFEFF]", "");
        
        logger.debug("清理后的JSON: {}", jsonStr.substring(0, Math.min(200, jsonStr.length())));
        
        // 2. 尝试解析JSON，如果失败则尝试修复
        Map<String, Object> plan;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsedPlan = objectMapper.readValue(jsonStr, Map.class);
            plan = parsedPlan;
        } catch (Exception e) {
            logger.warn("JSON解析失败，尝试修复: {}", e.getMessage());
            
            // 尝试修复常见的JSON格式问题
            String fixedJson = fixCommonJsonIssues(jsonStr);
            
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsedPlan = objectMapper.readValue(fixedJson, Map.class);
                plan = parsedPlan;
                logger.info("JSON修复成功");
            } catch (Exception e2) {
                logger.error("JSON修复失败，原始响应: {}", aiResponse);
                throw new Exception("AI返回的JSON格式无效，请重试");
            }
        }
        
        // 3. 使用高德地图API验证和优化路线
        optimizeRouteWithAmap(plan, request);
        
        return plan;
    }
    
    /**
     * 修复常见的JSON格式问题
     */
    private String fixCommonJsonIssues(String json) {
        // 1. 修复中文冒号（全角冒号）
        json = json.replace('\uff1a', ':');  // 全角冒号
        
        // 2. 修复中文引号
        json = json.replace('\u201c', '\"');  // 左双引号
        json = json.replace('\u201d', '\"');  // 右双引号
        json = json.replace('\u2018', '\"');  // 左单引号
        json = json.replace('\u2019', '\"');  // 右单引号
        
        // 3. 移除数组中多余的逗号
        json = json.replaceAll(",\\s*]", "]");
        json = json.replaceAll(",\\s*}", "}");
        
        // 4. 修复缺失的逗号（在}后面应该有逗号的情况）
        json = json.replaceAll("}\\s*\"", "},\"");
        json = json.replaceAll("]\\s*\"", "],\"");
        
        // 5. 移除多余的逗号
        json = json.replaceAll(",+", ",");
        
        return json;
    }
    
    /**
     * 异步优化路线（使用并行API调用）
     */
    private void optimizeRouteAsync(Map<String, Object> plan) {
        CompletableFuture.runAsync(() -> {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> itinerary = (List<Map<String, Object>>) plan.get("itinerary");
                
                if (itinerary == null) return;
                
                // 收集所有需要查询的路线
                List<String[]> routes = new ArrayList<>();
                List<Map<String, Object>> routeActivities = new ArrayList<>();
                
                for (Map<String, Object> day : itinerary) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> activities = (List<Map<String, Object>>) day.get("activities");
                    
                    if (activities == null) continue;
                    
                    for (int i = 0; i < activities.size() - 1; i++) {
                        Map<String, Object> current = activities.get(i);
                        Map<String, Object> next = activities.get(i + 1);
                        
                        if ("scenic".equals(current.get("type")) && "scenic".equals(next.get("type"))) {
                            @SuppressWarnings("unchecked")
                            List<Double> coords1 = (List<Double>) current.get("coordinates");
                            @SuppressWarnings("unchecked")
                            List<Double> coords2 = (List<Double>) next.get("coordinates");
                            
                            if (coords1 != null && coords2 != null) {
                                String origin = coords1.get(0) + "," + coords1.get(1);
                                String destination = coords2.get(0) + "," + coords2.get(1);
                                routes.add(new String[]{origin, destination});
                                routeActivities.add(current);
                            }
                        }
                    }
                }
                
                // 批量异步查询路线信息（并行处理）
                if (!routes.isEmpty() && amapService != null) {
                    long startTime = System.currentTimeMillis();
                    List<AmapService.RouteInfo> routeInfos = amapService.getBatchRouteInfoAsync(routes)
                            .get(10, TimeUnit.SECONDS); // 最多等待10秒
                    
                    // 更新活动信息
                    for (int i = 0; i < routeInfos.size(); i++) {
                        AmapService.RouteInfo info = routeInfos.get(i);
                        if (info.success()) {
                            Map<String, Object> activity = routeActivities.get(i);
                            activity.put("nextDistance", info.distance());
                            activity.put("nextDuration", info.duration());
                            logger.debug("路线优化: {} -> {}, {}m, {}s", 
                                    activity.get("title"), info.destination(), info.distance(), info.duration());
                        }
                    }
                    
                    long duration = System.currentTimeMillis() - startTime;
                    logger.info("批量路线优化完成: {} 条路线, 耗时 {}ms", routes.size(), duration);
                    
                    plan.put("optimized", true);
                    plan.put("optimizedBy", "高德地图API（异步并行）");
                }
                
            } catch (Exception e) {
                logger.warn("异步路线优化失败: {}", e.getMessage());
            }
        });
    }
    
    /**
     * 使用高德地图API优化路线（同步版本，保留用于降级）
     */
    private void optimizeRouteWithAmap(Map<String, Object> plan, AiPlanningRequest request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itinerary = (List<Map<String, Object>>) plan.get("itinerary");
            
            if (itinerary == null) return;
            
            for (Map<String, Object> day : itinerary) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> activities = (List<Map<String, Object>>) day.get("activities");
                
                if (activities == null) continue;
                
                for (int i = 0; i < activities.size() - 1; i++) {
                    Map<String, Object> current = activities.get(i);
                    Map<String, Object> next = activities.get(i + 1);
                    
                    if ("scenic".equals(current.get("type")) && "scenic".equals(next.get("type"))) {
                        @SuppressWarnings("unchecked")
                        List<Double> coords1 = (List<Double>) current.get("coordinates");
                        @SuppressWarnings("unchecked")
                        List<Double> coords2 = (List<Double>) next.get("coordinates");
                        
                        if (coords1 != null && coords2 != null) {
                            try {
                                String origin = coords1.get(0) + "," + coords1.get(1);
                                String destination = coords2.get(0) + "," + coords2.get(1);
                                
                                String apiUrl = String.format(
                                    "https://restapi.amap.com/v3/direction/driving?key=%s&origin=%s&destination=%s",
                                    amapApiKey, origin, destination
                                );
                                
                                ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);
                                @SuppressWarnings("unchecked")
                                Map<String, Object> amapData = objectMapper.readValue(response.getBody(), Map.class);
                                
                                if ("1".equals(amapData.get("status"))) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> route = (Map<String, Object>) amapData.get("route");
                                    @SuppressWarnings("unchecked")
                                    List<Map<String, Object>> paths = (List<Map<String, Object>>) route.get("paths");
                                    
                                    if (paths != null && !paths.isEmpty()) {
                                        Map<String, Object> path = paths.get(0);
                                        // 高德API返回的distance和duration是字符串类型，需要安全转换
                                        Integer distance = parseInteger(path.get("distance"));
                                        Integer duration = parseInteger(path.get("duration"));
                                        
                                        current.put("nextDistance", distance);
                                        current.put("nextDuration", duration);
                                        
                                        logger.debug("优化路线: {} -> {}, 距离: {}m, 时间: {}s", 
                                            current.get("title"), next.get("title"), distance, duration);
                                    }
                                }
                            } catch (Exception e) {
                                logger.warn("获取路线信息失败: {}", e.getMessage());
                            }
                        }
                    }
                }
            }
            
            plan.put("optimized", true);
            plan.put("optimizedBy", "高德地图API");
            
        } catch (Exception e) {
            logger.error("优化路线失败", e);
        }
    }
    
    /**
     * 计算天数
     */
    private int calculateDays(String startDate, String endDate) {
        try {
            long start = java.time.LocalDate.parse(startDate).toEpochDay();
            long end = java.time.LocalDate.parse(endDate).toEpochDay();
            return (int) (end - start + 1);
        } catch (Exception e) {
            return 3; // 默认3天
        }
    }
    
    /**
     * 获取出行方式描述
     */
    private String getTransportDescription(String transportation) {
        return switch (transportation) {
            case "car" -> "自驾游";
            case "public" -> "公共交通";
            case "tour" -> "跟团游";
            default -> "自驾游";
        };
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

