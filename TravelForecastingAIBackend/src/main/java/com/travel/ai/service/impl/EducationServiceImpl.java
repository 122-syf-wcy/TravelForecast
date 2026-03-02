package com.travel.ai.service.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.ai.config.DashScopeConfig;
import com.travel.ai.dto.EducationRequest;
import com.travel.ai.entity.StudyRoute;
import com.travel.ai.exception.AiServiceException;
import com.travel.ai.mapper.StudyRouteMapper;
import com.travel.ai.service.EducationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 研学教育服务实现
 *
 * 性能优化：
 * 1. 研学路线列表/详情 Redis缓存1年（静态数据）
 * 2. AI生成的研学方案缓存1年（相同参数=相同结果）
 * 3. 复用Generation实例、专用线程池、maxTokens限制
 */
@Service
public class EducationServiceImpl implements EducationService {

    private static final Logger log = LoggerFactory.getLogger(EducationServiceImpl.class);

    private static final String EDU_CACHE_PREFIX = "ai:edu:";
    private static final String EDU_ROUTE_CACHE = "ai:edu:routes:";
    private static final String EDU_DETAIL_CACHE = "ai:edu:detail:";

    @Autowired
    private StudyRouteMapper studyRouteMapper;

    @Autowired
    private DashScopeConfig dashScopeConfig;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    @Qualifier("aiTaskExecutor")
    private Executor aiTaskExecutor;

    // 复用 Generation 实例
    private final Generation generation = new Generation();

    @Value("${ai.education.cache-ttl-hours:8760}")
    private long eduCacheTtlHours;

    @Value("${ai.education.max-tokens:2048}")
    private int eduMaxTokens;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @SuppressWarnings("unchecked")
    public List<StudyRoute> listRoutes(String ageGroup, String theme) {
        // 缓存：路线列表是静态数据，缓存1年
        String cacheKey = EDU_ROUTE_CACHE + "list:" + ageGroup + ":" + theme;
        if (redisTemplate != null) {
            try {
                Object cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.debug("研学路线列表命中缓存");
                    return (List<StudyRoute>) cached;
                }
            } catch (Exception e) {
                log.debug("读取研学路线缓存失败: {}", e.getMessage());
            }
        }

        LambdaQueryWrapper<StudyRoute> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudyRoute::getEnabled, true);
        if (ageGroup != null) wrapper.eq(StudyRoute::getAgeGroup, ageGroup);
        if (theme != null) wrapper.eq(StudyRoute::getTheme, theme);
        wrapper.orderByAsc(StudyRoute::getSortOrder);
        List<StudyRoute> routes = studyRouteMapper.selectList(wrapper);

        // 写入缓存（1年）
        if (redisTemplate != null && !routes.isEmpty()) {
            try {
                redisTemplate.opsForValue().set(cacheKey, routes, eduCacheTtlHours, TimeUnit.HOURS);
                log.info("研学路线列表已缓存 [key={}, ttl={}h]", cacheKey, eduCacheTtlHours);
            } catch (Exception e) {
                log.debug("写入研学路线缓存失败: {}", e.getMessage());
            }
        }
        return routes;
    }

    @Override
    public StudyRoute getRouteDetail(Long id) {
        // 缓存：路线详情是静态数据，缓存1年
        String cacheKey = EDU_DETAIL_CACHE + id;
        if (redisTemplate != null) {
            try {
                Object cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.debug("研学路线详情命中缓存: id={}", id);
                    return (StudyRoute) cached;
                }
            } catch (Exception e) {
                log.debug("读取研学详情缓存失败: {}", e.getMessage());
            }
        }

        StudyRoute route = studyRouteMapper.selectById(id);

        if (redisTemplate != null && route != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, route, eduCacheTtlHours, TimeUnit.HOURS);
            } catch (Exception e) {
                log.debug("写入研学详情缓存失败: {}", e.getMessage());
            }
        }
        return route;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> generateStudyPlan(EducationRequest request) {
        // 先查缓存（相同参数的研学方案缓存1年）
        String cacheKey = generateEduCacheKey(request);
        if (redisTemplate != null) {
            try {
                Object cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.info("研学方案命中缓存 [key={}]", cacheKey);
                    return (Map<String, Object>) cached;
                }
            } catch (Exception e) {
                log.debug("读取研学方案缓存失败: {}", e.getMessage());
            }
        }

        String prompt = buildEducationPrompt(request);

        try {
            CompletableFuture<String> aiFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return callDashScope(prompt);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, aiTaskExecutor);

            String aiResponse = aiFuture.get(60, TimeUnit.SECONDS);

            // 解析JSON
            String jsonStr = extractJson(aiResponse);
            Map<String, Object> plan = objectMapper.readValue(jsonStr, Map.class);

            // 缓存1年
            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set(cacheKey, plan, eduCacheTtlHours, TimeUnit.HOURS);
                    log.info("研学方案已缓存 [key={}, ttl={}h]", cacheKey, eduCacheTtlHours);
                } catch (Exception e) {
                    log.debug("写入研学方案缓存失败: {}", e.getMessage());
                }
            }

            log.info("研学方案生成成功");
            return plan;

        } catch (Exception e) {
            log.error("研学方案生成失败: {}", e.getMessage());
            throw new AiServiceException("研学方案生成失败，请重试");
        }
    }

    @Override
    public StudyRoute addRoute(StudyRoute route) {
        route.setEnabled(true);
        route.setCreatedAt(LocalDateTime.now());
        route.setUpdatedAt(LocalDateTime.now());
        studyRouteMapper.insert(route);
        // 清除路线列表缓存
        clearRouteListCache();
        return route;
    }

    @Override
    public StudyRoute updateRoute(StudyRoute route) {
        route.setUpdatedAt(LocalDateTime.now());
        studyRouteMapper.updateById(route);
        // 清除相关缓存
        clearRouteListCache();
        if (redisTemplate != null && route.getId() != null) {
            try {
                redisTemplate.delete(EDU_DETAIL_CACHE + route.getId());
            } catch (Exception e) {
                log.debug("清除研学详情缓存失败: {}", e.getMessage());
            }
        }
        return route;
    }

    /**
     * 清除路线列表缓存
     */
    private void clearRouteListCache() {
        if (redisTemplate != null) {
            try {
                Set<String> keys = redisTemplate.keys(EDU_ROUTE_CACHE + "list:*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    log.info("清除研学路线列表缓存: {} 个", keys.size());
                }
            } catch (Exception e) {
                log.debug("清除路线列表缓存失败: {}", e.getMessage());
            }
        }
    }

    private String buildEducationPrompt(EducationRequest request) {
        String ageDesc = switch (request.getAgeGroup()) {
            case "primary" -> "小学生（6-12岁）";
            case "middle" -> "初中生（12-15岁）";
            case "high" -> "高中生（15-18岁）";
            case "college" -> "大学生（18-22岁）";
            default -> "所有年龄段";
        };

        String themeDesc = switch (request.getTheme()) {
            case "geography" -> "地理地质研学";
            case "history" -> "历史人文研学";
            case "ecology" -> "生态环保研学";
            case "culture" -> "民族文化研学";
            default -> "综合研学";
        };

        return String.format("""
                为六盘水市设计一个%s方案，适合%s，共%d天。
                
                选择的景区：%s
                
                六盘水研学资源：
                - 梅花山：生态观察、植物识别
                - 乌蒙大草原：地质地貌、高原生态、风能发电
                - 玉舍森林公园：森林生态、动植物观察
                - 明湖湿地：湿地生态系统、水资源保护
                - 水城古镇：彝族苗族文化、非遗技艺
                
                请输出JSON：
                {"title":"方案名称","theme":"%s","ageGroup":"%s","days":%d,"summary":"概述",
                "objectives":["学习目标1","学习目标2"],
                "schedule":[{"day":1,"theme":"当日主题","activities":[
                {"time":"09:00","title":"活动名","location":"地点","description":"详细内容",
                "learningPoints":["知识点1"],"interactiveTask":"互动任务"}
                ]}],
                "assessment":["考核方式"],"materials":["所需材料"]}
                """,
                themeDesc, ageDesc, request.getDays(),
                String.join("、", request.getAttractions()),
                request.getTheme(), request.getAgeGroup(), request.getDays()
        );
    }

    private String callDashScope(String prompt) throws Exception {
        String apiKey = dashScopeConfig.getEffectiveApiKey();
        long start = System.currentTimeMillis();

        List<Message> messages = new ArrayList<>();
        messages.add(Message.builder().role(Role.USER.getValue()).content(prompt).build());

        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(dashScopeConfig.getPlanningModel())
                .messages(messages)
                .maxTokens(eduMaxTokens)
                .temperature(0.5f)
                .topP(0.8)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();

        GenerationResult result = generation.call(param);
        String content = result.getOutput().getChoices().get(0).getMessage().getContent();

        log.debug("研学DashScope调用耗时: {}ms, 输出: {} 字符",
                System.currentTimeMillis() - start, content.length());

        return content;
    }

    /**
     * 生成研学方案缓存Key
     */
    private String generateEduCacheKey(EducationRequest request) {
        String keySource = String.format("%s_%s_%d_%s",
                request.getAgeGroup(), request.getTheme(),
                request.getDays(),
                String.join(",", request.getAttractions()));
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(keySource.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return EDU_CACHE_PREFIX + "plan:" + hex.toString().substring(0, 16);
        } catch (Exception e) {
            return EDU_CACHE_PREFIX + "plan:" + keySource.hashCode();
        }
    }

    private String extractJson(String response) {
        String str = response.trim();
        if (str.startsWith("```json")) str = str.substring(7);
        if (str.startsWith("```")) str = str.substring(3);
        if (str.endsWith("```")) str = str.substring(0, str.length() - 3);
        int first = str.indexOf('{');
        int last = str.lastIndexOf('}');
        if (first >= 0 && last > first) {
            str = str.substring(first, last + 1);
        }
        return str.trim();
    }
}
