package com.travel.ai.service.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.ai.config.DashScopeConfig;
import com.travel.ai.dto.PlanningRequest;
import com.travel.ai.exception.AiServiceException;
import com.travel.ai.service.AiPlanningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * AI行程规划服务实现
 * 迁移自业务后端的AiPlanningController
 *
 * 性能优化：复用Generation实例、专用线程池、maxTokens限制
 */
@Service
public class AiPlanningServiceImpl implements AiPlanningService {

    private static final Logger log = LoggerFactory.getLogger(AiPlanningServiceImpl.class);

    private static final String PLAN_CACHE_PREFIX = "ai:plan:";

    @Autowired
    private DashScopeConfig dashScopeConfig;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    @Qualifier("aiTaskExecutor")
    private Executor aiTaskExecutor;

    // 复用 Generation 实例
    private final Generation generation = new Generation();

    @Value("${ai.planning.timeout:60}")
    private int planningTimeout;

    @Value("${ai.planning.cache-ttl-hours:168}")
    private long planCacheTtlHours;

    @Value("${ai.planning.max-tokens:3072}")
    private int planMaxTokens;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> generatePlan(PlanningRequest request) {
        long startTime = System.currentTimeMillis();

        // 1. 检查缓存
        String cacheKey = generatePlanCacheKey(request);
        Map<String, Object> cachedPlan = getFromCache(cacheKey);
        if (cachedPlan != null) {
            log.info("行程规划命中缓存 [key={}]", cacheKey);
            return cachedPlan;
        }

        // 2. 构建AI提示词
        String prompt = buildPlanningPrompt(request);
        log.debug("行程规划提示词长度: {} 字符", prompt.length());

        // 3. 调用AI（使用专用线程池 + 超时控制）
        String aiResponse;
        try {
            CompletableFuture<String> aiFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return callDashScope(prompt);
                } catch (Exception e) {
                    throw new RuntimeException("AI调用失败: " + e.getMessage(), e);
                }
            }, aiTaskExecutor);

            aiResponse = aiFuture.get(planningTimeout, TimeUnit.SECONDS);
            long aiDuration = System.currentTimeMillis() - startTime;
            log.info("行程规划AI响应: {} 字符, 耗时 {}ms", aiResponse.length(), aiDuration);

        } catch (TimeoutException e) {
            throw new AiServiceException("AI行程规划超时，请稍后重试");
        } catch (Exception e) {
            throw new AiServiceException("AI行程规划失败: " + e.getMessage());
        }

        // 4. 解析AI响应
        Map<String, Object> plan = parseAiResponse(aiResponse);

        // 5. 缓存结果
        putToCache(cacheKey, plan);

        long totalDuration = System.currentTimeMillis() - startTime;
        log.info("行程规划完成，总耗时: {}ms", totalDuration);

        return plan;
    }

    /**
     * 构建行程规划提示词
     */
    private String buildPlanningPrompt(PlanningRequest request) {
        int days = calculateDays(request.getStartDate(), request.getEndDate());
        String transportDesc = getTransportDescription(request.getTransportation());
        int totalBudget = request.getBudget();
        int peopleCount = request.getPeopleCount();

        int dailyBudget = totalBudget / days;
        int ticketBudget = (int) (totalBudget * 0.30);
        int foodBudget = (int) (totalBudget * 0.25);
        int transportBudget = (int) (totalBudget * 0.20);
        int hotelBudget = (int) (totalBudget * 0.20);
        int otherBudget = (int) (totalBudget * 0.05);

        // 构建客流预测信息
        StringBuilder flowInfo = new StringBuilder();
        if (request.getFlowPredictions() != null && !request.getFlowPredictions().isEmpty()) {
            flowInfo.append("\n【客流预测数据】根据双流融合预测模型分析：\n");
            for (Map.Entry<String, List<PlanningRequest.FlowPrediction>> entry : request.getFlowPredictions().entrySet()) {
                String spotName = entry.getKey();
                List<PlanningRequest.FlowPrediction> predictions = entry.getValue();
                if (predictions != null && !predictions.isEmpty()) {
                    flowInfo.append("- ").append(spotName).append("：");
                    for (int i = 0; i < predictions.size(); i++) {
                        PlanningRequest.FlowPrediction pred = predictions.get(i);
                        String levelCn = switch (pred.getCrowdLevel()) {
                            case "high" -> "拥挤";
                            case "medium" -> "适中";
                            default -> "舒适";
                        };
                        flowInfo.append("第").append(i + 1).append("天约").append(pred.getFlow())
                                .append("人(").append(levelCn).append(") ");
                    }
                    flowInfo.append("\n");
                }
            }
            flowInfo.append("请根据预测数据安排游览时段。\n\n");
        }

        return String.format("""
                生成六盘水%d日游行程，%d人%s，总预算%d元。
                
                景区：%s
                %s
                【预算分配】
                - 总预算：%d元，每日约%d元
                - 门票：%d元 | 餐饮：%d元 | 交通：%d元 | 住宿：%d元 | 其他：%d元
                
                【景区门票】梅花山¥60/人、玉舍¥40/人、乌蒙¥65/人、明湖免费、水城古镇免费
                
                直接输出JSON：
                {"title":"六盘水%d日游","summary":"概述","totalBudget":%d,"budgetBreakdown":{"ticket":%d,"food":%d,"transportation":%d,"accommodation":%d,"other":%d},"itinerary":[{"day":1,"date":"%s","activities":[{"time":"08:00","title":"活动","description":"描述","type":"scenic/food/hotel/transit","duration":"时长","cost":费用,"location":"地点"}]}],"tips":["建议"]}
                
                要求：每天6-8个活动，cost字段必须填数字，门票费用=单价×人数%d。
                """,
                days, peopleCount, transportDesc, totalBudget,
                String.join("、", request.getAttractions()),
                flowInfo,
                totalBudget, dailyBudget,
                ticketBudget, foodBudget, transportBudget, hotelBudget, otherBudget,
                days, totalBudget, ticketBudget, foodBudget, transportBudget, hotelBudget, otherBudget,
                request.getStartDate(),
                peopleCount
        );
    }

    /**
     * 调用通义千问AI（优化版 - maxTokens限制 + 低温度加速）
     */
    private String callDashScope(String prompt) throws Exception {
        String apiKey = dashScopeConfig.getEffectiveApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new AiServiceException("未配置DashScope API Key");
        }

        long start = System.currentTimeMillis();

        List<Message> messages = new ArrayList<>();
        messages.add(Message.builder()
                .role(Role.USER.getValue())
                .content(prompt)
                .build());

        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(dashScopeConfig.getPlanningModel())
                .messages(messages)
                .maxTokens(planMaxTokens)
                .temperature(0.5f)
                .topP(0.8)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();

        GenerationResult result = generation.call(param);
        String content = result.getOutput().getChoices().get(0).getMessage().getContent();

        log.debug("行程规划DashScope调用耗时: {}ms, 输出: {} 字符",
                System.currentTimeMillis() - start, content.length());

        return content;
    }

    /**
     * 解析AI响应JSON
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAiResponse(String aiResponse) {
        String jsonStr = aiResponse.trim();

        // 移除markdown代码块标记
        if (jsonStr.startsWith("```json")) jsonStr = jsonStr.substring(7);
        if (jsonStr.startsWith("```")) jsonStr = jsonStr.substring(3);
        if (jsonStr.endsWith("```")) jsonStr = jsonStr.substring(0, jsonStr.length() - 3);

        int firstBrace = jsonStr.indexOf('{');
        int lastBrace = jsonStr.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            jsonStr = jsonStr.substring(firstBrace, lastBrace + 1);
        }
        jsonStr = jsonStr.trim();
        jsonStr = jsonStr.replaceAll("[\\u0000-\\u001F\\u007F-\\u009F\\u200B-\\u200D\\uFEFF]", "");

        try {
            return objectMapper.readValue(jsonStr, Map.class);
        } catch (Exception e) {
            log.warn("JSON解析失败，尝试修复: {}", e.getMessage());
            String fixed = fixCommonJsonIssues(jsonStr);
            try {
                return objectMapper.readValue(fixed, Map.class);
            } catch (Exception e2) {
                log.error("JSON修复失败: {}", aiResponse);
                throw new AiServiceException("AI返回的行程规划格式无效，请重试");
            }
        }
    }

    private String fixCommonJsonIssues(String json) {
        json = json.replace('\uff1a', ':');
        json = json.replace('\u201c', '\"');
        json = json.replace('\u201d', '\"');
        json = json.replace('\u2018', '\"');
        json = json.replace('\u2019', '\"');
        json = json.replaceAll(",\\s*]", "]");
        json = json.replaceAll(",\\s*}", "}");
        json = json.replaceAll("}\\s*\"", "},\"");
        json = json.replaceAll("]\\s*\"", "],\"");
        json = json.replaceAll(",+", ",");
        return json;
    }

    private int calculateDays(String startDate, String endDate) {
        try {
            long start = LocalDate.parse(startDate).toEpochDay();
            long end = LocalDate.parse(endDate).toEpochDay();
            return (int) (end - start + 1);
        } catch (Exception e) {
            return 3;
        }
    }

    private String getTransportDescription(String transportation) {
        return switch (transportation) {
            case "car" -> "自驾游";
            case "public" -> "公共交通";
            case "tour" -> "跟团游";
            default -> "自驾游";
        };
    }

    // ==================== 缓存 ====================

    private String generatePlanCacheKey(PlanningRequest request) {
        String keySource = String.format("%s_%s_%s_%d_%d_%s",
                request.getStartDate(), request.getEndDate(),
                String.join(",", request.getAttractions()),
                request.getPeopleCount(), request.getBudget(),
                request.getTransportation());
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(keySource.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return PLAN_CACHE_PREFIX + hex.toString().substring(0, 16);
        } catch (Exception e) {
            return PLAN_CACHE_PREFIX + keySource.hashCode();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getFromCache(String cacheKey) {
        if (redisTemplate == null) return null;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            return cached != null ? (Map<String, Object>) cached : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void putToCache(String cacheKey, Map<String, Object> plan) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.opsForValue().set(cacheKey, plan, planCacheTtlHours, TimeUnit.HOURS);
            log.info("行程规划已缓存 [key={}, ttl={}h]", cacheKey, planCacheTtlHours);
        } catch (Exception e) {
            log.warn("缓存行程规划失败: {}", e.getMessage());
        }
    }
}
