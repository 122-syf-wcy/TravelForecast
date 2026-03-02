package com.travel.service.prediction.impl;

import com.alibaba.dashscope.common.Message;
import com.travel.service.prediction.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 带缓存的AI服务实现
 * 通过Redis缓存AI响应，大幅提升响应速度
 */
@Service("cachedAiService")
@Primary // 优先使用带缓存的版本
public class CachedAiServiceImpl implements AiService {

    private static final Logger logger = LoggerFactory.getLogger(CachedAiServiceImpl.class);
    
    private static final String CACHE_PREFIX = "ai:response:";
    private static final long CACHE_TTL_HOURS = 24; // 静态内容缓存24小时
    private static final long CACHE_TTL_REALTIME_MINUTES = 5; // 实时数据缓存5分钟
    
    // 实时性关键词列表（这些问题不适合长时间缓存）
    private static final String[] REALTIME_KEYWORDS = {
        "天气", "气温", "温度", "下雨", "晴天", "阴天",
        "当前", "现在", "今天", "此刻", "实时",
        "客流", "人数", "拥挤", "排队",
        "营业", "开放", "关闭", "时间"
    };
    
    @Autowired
    private AiServiceImpl baseAiService; // 原始AI服务
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public String chat(String model, List<Message> messages, String apiKey) throws Exception {
        // 如果Redis不可用，直接调用原始服务
        if (redisTemplate == null) {
            logger.debug("Redis不可用，直接调用AI服务");
            return baseAiService.chat(model, messages, apiKey);
        }

        try {
            // 获取用户消息内容，用于判断缓存策略
            String userMessage = extractLastUserMessage(messages);
            
            // 生成缓存key（基于消息内容的哈希）
            String cacheKey = generateCacheKey(model, messages);
            
            // 尝试从缓存获取
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                logger.info("AI响应命中缓存，跳过API调用 [key={}]", cacheKey);
                return (String) cached;
            }
            
            // 缓存未命中，调用AI服务
            logger.debug("AI响应未命中缓存，调用API [key={}]", cacheKey);
            long startTime = System.currentTimeMillis();
            String response = baseAiService.chat(model, messages, apiKey);
            long duration = System.currentTimeMillis() - startTime;
            
            // 智能缓存策略：根据问题类型决定缓存时长
            if (response != null && !response.isEmpty()) {
                if (isRealtimeQuery(userMessage)) {
                    // 实时数据：短时间缓存（5分钟）
                    redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL_REALTIME_MINUTES, TimeUnit.MINUTES);
                    logger.info("AI响应已缓存（实时数据，{}分钟） [key={}, duration={}ms]", 
                            CACHE_TTL_REALTIME_MINUTES, cacheKey, duration);
                } else {
                    // 静态内容：长时间缓存（24小时）
                    redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL_HOURS, TimeUnit.HOURS);
                    logger.info("AI响应已缓存（静态内容，{}小时） [key={}, duration={}ms]", 
                            CACHE_TTL_HOURS, cacheKey, duration);
                }
            }
            
            return response;
            
        } catch (Exception e) {
            logger.warn("缓存操作失败，降级为直接调用: {}", e.getMessage());
            return baseAiService.chat(model, messages, apiKey);
        }
    }

    /**
     * 提取最后一条用户消息
     */
    private String extractLastUserMessage(List<Message> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if ("user".equals(msg.getRole())) {
                return msg.getContent();
            }
        }
        return "";
    }
    
    /**
     * 判断是否为实时性查询
     * 包含天气、当前状态等实时变化的内容不适合长时间缓存
     */
    private boolean isRealtimeQuery(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        
        String lowerMessage = message.toLowerCase();
        for (String keyword : REALTIME_KEYWORDS) {
            if (lowerMessage.contains(keyword.toLowerCase())) {
                logger.debug("检测到实时性关键词: {}", keyword);
                return true;
            }
        }
        return false;
    }
    
    /**
     * 生成缓存key（基于最后一条用户消息的SHA-256哈希）
     * 注意：只使用最后一条用户消息，避免因对话历史导致相同问题无法命中缓存
     */
    private String generateCacheKey(String model, List<Message> messages) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(model).append(":");
            
            // 只使用最后一条用户消息内容生成key，避免因历史上下文导致缓存未命中
            String lastUserMessage = extractLastUserMessage(messages);
            
            if (lastUserMessage != null && !lastUserMessage.isEmpty()) {
                sb.append(lastUserMessage);
            }
            
            // 生成SHA-256哈希
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return CACHE_PREFIX + hexString.toString();
            
        } catch (Exception e) {
            logger.warn("生成缓存key失败: {}", e.getMessage());
            // 降级方案：使用简单的字符串哈希
            return CACHE_PREFIX + Math.abs(messages.toString().hashCode());
        }
    }
    
    /**
     * 清除指定缓存
     */
    public void clearCache(String model, List<Message> messages) {
        if (redisTemplate != null) {
            try {
                String cacheKey = generateCacheKey(model, messages);
                redisTemplate.delete(cacheKey);
                logger.info("已清除缓存 [key={}]", cacheKey);
            } catch (Exception e) {
                logger.warn("清除缓存失败: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 清除所有AI缓存
     */
    public void clearAllCache() {
        if (redisTemplate != null) {
            try {
                redisTemplate.keys(CACHE_PREFIX + "*").forEach(key -> redisTemplate.delete(key));
                logger.info("已清除所有AI缓存");
            } catch (Exception e) {
                logger.warn("清除所有缓存失败: {}", e.getMessage());
            }
        }
    }
}

