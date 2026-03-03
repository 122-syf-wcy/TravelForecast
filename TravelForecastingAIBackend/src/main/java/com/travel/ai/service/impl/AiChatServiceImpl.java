package com.travel.ai.service.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.ai.config.DashScopeConfig;
import com.travel.ai.dto.ChatRequest;
import com.travel.ai.dto.ChatResponse;
import com.travel.ai.entity.Conversation;
import com.travel.ai.entity.Message;
import com.travel.ai.exception.AiServiceException;
import com.travel.ai.mapper.ConversationMapper;
import com.travel.ai.mapper.MessageMapper;
import com.travel.ai.service.AiChatService;
import com.travel.ai.service.RagService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.travel.ai.client.PredictionClient;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * AI聊天服务实现
 * 迁移自业务后端的ChatController + CachedAiServiceImpl
 *
 * 性能优化：
 * 1. maxTokens 限制输出长度，避免模型无限生成
 * 2. 复用 Generation 实例，减少对象创建开销
 * 3. 使用专用 aiTaskExecutor 线程池
 * 4. 精简系统提示词减少输入Token
 * 5. 启动时预热高频问题缓存
 * 6. RAG结果缓存避免重复查库
 */
@Service
public class AiChatServiceImpl implements AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    private static final String CACHE_PREFIX = "ai:chat:";
    private static final String RAG_CACHE_PREFIX = "ai:rag:";

    // 实时性关键词 → 缓存10分钟
    private static final String[] REALTIME_KEYWORDS = {
            "天气", "气温", "温度", "下雨", "晴天", "阴天",
            "当前", "现在", "今天", "此刻", "实时",
            "客流", "人数", "拥挤", "排队",
            "营业", "开放", "关闭", "时间"
    };

    // 静态内容关键词 → 缓存1年（景区介绍、美食、交通等几乎不变的信息）
    private static final String[] STATIC_KEYWORDS = {
            "介绍", "简介", "在哪", "地址", "位置", "怎么去", "怎么走",
            "门票", "票价", "多少钱", "收费", "免费",
            "特色", "特产", "美食", "小吃", "烙锅", "羊肉粉",
            "历史", "文化", "民族", "彝族", "苗族",
            "海拔", "面积", "景点", "景区",
            "梅花山", "乌蒙", "玉舍", "明湖", "水城古镇",
            "凉都", "六盘水", "避暑",
            "交通", "机场", "高铁", "火车", "汽车"
    };

    // 高频问题列表 → 启动时预热缓存
    private static final String[] HOT_QUESTIONS = {
            "六盘水有什么好玩的",
            "梅花山介绍",
            "乌蒙大草原介绍",
            "玉舍国家森林公园介绍",
            "明湖湿地公园介绍",
            "水城古镇介绍",
            "六盘水美食推荐",
            "六盘水怎么去",
            "六盘水门票多少钱",
            "六盘水天气怎么样"
    };

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private DashScopeConfig dashScopeConfig;

    @Autowired(required = false)
    private RagService ragService;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PredictionClient predictionClient;

    @Autowired
    @Qualifier("aiTaskExecutor")
    private Executor aiTaskExecutor;

    // 复用 Generation 实例（线程安全）
    private final Generation generation = new Generation();

    @Value("${ai.chat.timeout:30}")
    private int chatTimeout;

    @Value("${ai.chat.max-history:6}")
    private int maxHistory;

    @Value("${ai.chat.cache-ttl-hours:8760}")
    private long staticCacheTtlHours;

    @Value("${ai.chat.normal-cache-ttl-hours:72}")
    private long normalCacheTtlHours;

    @Value("${ai.chat.realtime-cache-ttl-minutes:10}")
    private long realtimeCacheTtlMinutes;

    @Value("${ai.chat.max-tokens:1024}")
    private int maxTokens;

    /**
     * 启动时预热高频问题缓存（异步，不阻塞启动）
     */
    @PostConstruct
    public void warmUpCache() {
        CompletableFuture.runAsync(() -> {
            log.info("========== 开始预热高频问题缓存 ==========");
            int cached = 0;
            for (String question : HOT_QUESTIONS) {
                try {
                    String cacheKey = generateCacheKey(question);
                    if (getFromCache(cacheKey) != null) {
                        log.debug("缓存已存在，跳过: {}", question);
                        cached++;
                        continue;
                    }
                    // 构造简单消息调AI
                    List<com.alibaba.dashscope.common.Message> msgs = new ArrayList<>();
                    msgs.add(com.alibaba.dashscope.common.Message.builder()
                            .role("system").content(buildSystemPrompt()).build());
                    msgs.add(com.alibaba.dashscope.common.Message.builder()
                            .role("user").content(question).build());
                    String reply = callDashScope(dashScopeConfig.getDefaultModel(), msgs);
                    if (reply != null && !reply.isEmpty()) {
                        putToCache(cacheKey, reply, question);
                        cached++;
                        log.info("预热缓存成功: {}", question);
                    }
                    // 控制速率，避免API限流
                    Thread.sleep(500);
                } catch (Exception e) {
                    log.warn("预热缓存失败 [{}]: {}", question, e.getMessage());
                }
            }
            log.info("========== 缓存预热完成: {}/{} ==========", cached, HOT_QUESTIONS.length);
        }, aiTaskExecutor);
    }

    @Override
    public ChatResponse chat(ChatRequest request, Long userId) {
        long startTime = System.currentTimeMillis();

        // 1. 找或建会话
        Conversation conv = findOrCreateConversation(request, userId);

        // 2. 保存用户消息
        saveMessage(conv.getId(), "user", request.getMessage(), null);

        // 3. 先查缓存（缓存命中直接返回，最快路径）
        String cacheKey = generateCacheKey(request.getMessage());
        String cachedReply = getFromCache(cacheKey);
        if (cachedReply != null) {
            log.info("聊天命中缓存 [key={}], 耗时{}ms", cacheKey, System.currentTimeMillis() - startTime);
            saveMessage(conv.getId(), "assistant", cachedReply, dashScopeConfig.getDefaultModel());
            updateConversation(conv, cachedReply);
            return new ChatResponse(cachedReply, conv.getConversationUuid());
        }

        // 4. 构造多轮上下文（缓存未命中才构造，节省时间）
        List<com.alibaba.dashscope.common.Message> messages = buildMessages(conv, request.getMessage());

        // 5. 调用AI（使用专用线程池 + 超时控制）
        String reply;
        try {
            CompletableFuture<String> aiFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return callDashScope(dashScopeConfig.getDefaultModel(), messages);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, aiTaskExecutor);

            reply = aiFuture.get(chatTimeout, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;
            log.info("AI聊天响应成功: {} 字符, 耗时 {}ms", reply.length(), duration);

            // 缓存结果
            putToCache(cacheKey, reply, request.getMessage());

        } catch (TimeoutException e) {
            log.warn("AI聊天超时 ({}秒)", chatTimeout);
            reply = "抱歉，AI服务响应超时，请稍后重试。";
        } catch (Exception e) {
            log.error("AI聊天失败: {}", e.getMessage());
            reply = "抱歉，AI服务暂时不可用，请稍后重试。";
        }

        // 6. 保存AI回复
        saveMessage(conv.getId(), "assistant", reply, dashScopeConfig.getDefaultModel());
        updateConversation(conv, reply);

        return new ChatResponse(reply, conv.getConversationUuid());
    }

    @Override
    public List<Message> getHistory(String conversationId) {
        Conversation conv = conversationMapper.selectOne(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getConversationUuid, conversationId));
        if (conv == null) return Collections.emptyList();

        return messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conv.getId())
                        .orderByAsc(Message::getCreatedAt));
    }

    @Override
    public void clearHistory(String conversationId) {
        Conversation conv = conversationMapper.selectOne(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getConversationUuid, conversationId));
        if (conv != null) {
            messageMapper.delete(
                    new LambdaQueryWrapper<Message>()
                            .eq(Message::getConversationId, conv.getId()));
        }
    }

    @Override
    public List<?> getUserConversations(Long userId) {
        return conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUserId, userId)
                        .orderByDesc(Conversation::getUpdatedAt));
    }

    // ==================== 私有方法 ====================

    /**
     * 找或创建会话
     */
    private Conversation findOrCreateConversation(ChatRequest request, Long userId) {
        Conversation conv = null;
        if (request.getConversationId() != null) {
            conv = conversationMapper.selectOne(
                    new LambdaQueryWrapper<Conversation>()
                            .eq(Conversation::getConversationUuid, request.getConversationId()));
        }

        if (conv == null) {
            conv = new Conversation();
            conv.setConversationUuid(UUID.randomUUID().toString());
            conv.setUserId(userId);
            conv.setScenicId(request.getScenicId());
            conv.setType("chat");
            conv.setTitle(request.getMessage().length() > 30
                    ? request.getMessage().substring(0, 30) + "..."
                    : request.getMessage());
            conv.setLastMessage(request.getMessage());
            conv.setMessageCount(0);
            conv.setCreatedAt(LocalDateTime.now());
            conv.setUpdatedAt(LocalDateTime.now());
            conversationMapper.insert(conv);
        }

        return conv;
    }

    /**
     * 保存消息
     */
    private void saveMessage(Long conversationId, String role, String content, String model) {
        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setContentType("text");
        msg.setModel(model);
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);
    }

    /**
     * 更新会话
     */
    private void updateConversation(Conversation conv, String lastMessage) {
        conv.setLastMessage(lastMessage.length() > 100
                ? lastMessage.substring(0, 100) + "..."
                : lastMessage);
        conv.setMessageCount(conv.getMessageCount() != null ? conv.getMessageCount() + 2 : 2);
        conv.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conv);
    }

    /**
     * 构建多轮对话消息
     */
    private List<com.alibaba.dashscope.common.Message> buildMessages(Conversation conv, String currentMessage) {
        List<com.alibaba.dashscope.common.Message> messages = new ArrayList<>();

        // 系统提示词（精简版，减少输入Token）
        String systemPrompt = buildSystemPrompt();
        messages.add(com.alibaba.dashscope.common.Message.builder()
                .role("system").content(systemPrompt).build());

        // 尝试检索知识库增强（RAG），带缓存
        if (ragService != null && currentMessage.length() > 2) {
            try {
                String knowledgeContext = getCachedRagResult(currentMessage, conv.getScenicId());
                if (knowledgeContext != null && !knowledgeContext.isEmpty()) {
                    messages.add(com.alibaba.dashscope.common.Message.builder()
                            .role("system")
                            .content("【参考资料】" + knowledgeContext)
                            .build());
                }
            } catch (Exception e) {
                log.debug("知识库检索跳过: {}", e.getMessage());
            }
        }

        // 尝试注入客流预测数据
        if (isPredictionQuery(currentMessage) && conv.getScenicId() != null) {
            try {
                // 查询未来3天的预测
                List<Map<String, Object>> predictions = predictionClient.getPrediction(conv.getScenicId(), 3);
                if (predictions != null && !predictions.isEmpty()) {
                    StringBuilder predText = new StringBuilder("【实时客流预测数据】");
                    for (Map<String, Object> p : predictions) {
                        predText.append(String.format("\\n%s: 预计客流 %s人", p.get("date"), p.get("predicted_flow")));
                    }
                    messages.add(com.alibaba.dashscope.common.Message.builder()
                            .role("system")
                            .content(predText.toString())
                            .build());
                    log.info("已向大模型注入 {} 的客流预测上下文", conv.getScenicId());
                }
            } catch (Exception e) {
                log.warn("获取预测数据失败: {}", e.getMessage());
            }
        }

        // 历史消息（限制数量，只查最近N条，减少DB压力）
        List<Message> history = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conv.getId())
                        .orderByDesc(Message::getCreatedAt)
                        .last("LIMIT " + maxHistory));

        // 反转为时间正序
        Collections.reverse(history);

        for (Message m : history) {
            String role = "user".equals(m.getRole()) ? "user" : "assistant";
            messages.add(com.alibaba.dashscope.common.Message.builder()
                    .role(role).content(m.getContent()).build());
        }

        return messages;
    }

    /**
     * 带缓存的RAG查询（避免相同问题重复查库）
     */
    private String getCachedRagResult(String question, Long scenicId) {
        if (ragService == null) return null;

        // 尝试从缓存获取RAG结果
        String ragCacheKey = RAG_CACHE_PREFIX + Math.abs((question + ":" + scenicId).hashCode());
        String cachedRag = getFromCache(ragCacheKey);
        if (cachedRag != null) {
            log.debug("RAG命中缓存: {}", ragCacheKey);
            return cachedRag;
        }

        // 查库
        String result = ragService.answerWithKnowledge(question, scenicId);
        if (result != null && !result.isEmpty()) {
            // RAG结果缓存24小时（知识库不常变）
            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set(ragCacheKey, result, 24, TimeUnit.HOURS);
                } catch (Exception e) {
                    log.debug("RAG缓存写入失败: {}", e.getMessage());
                }
            }
        }
        return result;
    }

    /**
     * 构建系统提示词（精简版 - 减少Token消耗提升速度）
     */
    private String buildSystemPrompt() {
        String currentDateTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm"));

        return String.format("""
                你是六盘水旅游智能客服，服务于"游韵华章"旅游系统。当前时间：%s
                
                六盘水（中国凉都）主要景区：梅花山（梅花观赏/生态休闲）、乌蒙大草原（高山草原/贵州屋脊）、明湖湿地（城市生态/休闲观光）、玉舍森林公园（原始森林/滑雪场）、水城古镇（历史民俗/非遗文化）。
                
                规则：只介绍六盘水景区；回答简洁专业，突出凉都特色；时间相关问题基于当前时间。
                """, currentDateTime);
    }

    /**
     * 调用DashScope通义千问（优化版）
     * - maxTokens 限制输出长度，大幅减少生成时间
     * - temperature 降低到0.5，减少采样随机性，加速收敛
     * - 复用 Generation 实例
     */
    private String callDashScope(String model, List<com.alibaba.dashscope.common.Message> messages) throws Exception {
        String apiKey = dashScopeConfig.getEffectiveApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new AiServiceException("未配置DashScope API Key");
        }

        long start = System.currentTimeMillis();

        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(model)
                .messages(messages)
                .maxTokens(maxTokens)
                .temperature(0.5f)
                .topP(0.8)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();

        GenerationResult result = generation.call(param);
        String content = result.getOutput().getChoices().get(0).getMessage().getContent();

        log.debug("DashScope调用耗时: {}ms, 模型: {}, 输出: {} 字符",
                System.currentTimeMillis() - start, model, content.length());

        return content;
    }

    // ==================== 缓存方法 ====================

    private String getFromCache(String cacheKey) {
        if (redisTemplate == null) return null;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            return cached != null ? (String) cached : null;
        } catch (Exception e) {
            log.debug("读取缓存失败: {}", e.getMessage());
            return null;
        }
    }

    private void putToCache(String cacheKey, String reply, String userMessage) {
        if (redisTemplate == null || reply == null || reply.isEmpty()) return;
        try {
            if (isRealtimeQuery(userMessage)) {
                // 实时数据：10分钟
                redisTemplate.opsForValue().set(cacheKey, reply, realtimeCacheTtlMinutes, TimeUnit.MINUTES);
                log.info("缓存[实时-{}分钟]: {}", realtimeCacheTtlMinutes, cacheKey.substring(cacheKey.length() - 8));
            } else if (isStaticQuery(userMessage)) {
                // 静态内容：1年（景区介绍、门票、美食等）
                redisTemplate.opsForValue().set(cacheKey, reply, staticCacheTtlHours, TimeUnit.HOURS);
                log.info("缓存[静态-1年]: {}", cacheKey.substring(cacheKey.length() - 8));
            } else {
                // 普通对话：3天
                redisTemplate.opsForValue().set(cacheKey, reply, normalCacheTtlHours, TimeUnit.HOURS);
                log.info("缓存[普通-{}小时]: {}", normalCacheTtlHours, cacheKey.substring(cacheKey.length() - 8));
            }
        } catch (Exception e) {
            log.debug("写入缓存失败: {}", e.getMessage());
        }
    }

    private boolean isRealtimeQuery(String message) {
        if (message == null || message.isEmpty()) return false;
        for (String keyword : REALTIME_KEYWORDS) {
            if (message.contains(keyword)) return true;
        }
        return false;
    }

    private boolean isStaticQuery(String message) {
        if (message == null || message.isEmpty()) return false;
        for (String keyword : STATIC_KEYWORDS) {
            if (message.contains(keyword)) return true;
        }
        return false;
    }

    private boolean isPredictionQuery(String message) {
        if (message == null || message.isEmpty()) return false;
        return message.contains("客流") || message.contains("人多") || message.contains("预测") 
            || message.contains("人数") || message.contains("拥挤");
    }

    private String generateCacheKey(String message) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(message.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return CACHE_PREFIX + hex;
        } catch (Exception e) {
            return CACHE_PREFIX + Math.abs(message.hashCode());
        }
    }
}
