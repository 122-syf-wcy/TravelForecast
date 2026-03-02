package com.travel.controller.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.Result;
import com.travel.entity.common.ChatConversation;
import com.travel.entity.common.ChatMessage;
import com.travel.mapper.common.ChatConversationMapper;
import com.travel.mapper.common.ChatMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.travel.service.prediction.AiService;
import org.springframework.beans.factory.annotation.Value;

/**
 * @deprecated 已迁移到 AI智能服务 (TravelForecastingAIBackend, Port 8081)
 * 前端已改为调用 /ai-api/chat/message
 * 此Controller保留作为备份，后续可安全删除
 */
@Deprecated
@RestController
@RequestMapping("/chat-legacy")
public class ChatController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private ChatConversationMapper conversationMapper;
    @Autowired
    private ChatMessageMapper messageMapper;
    @Autowired(required = false)
    private AiService aiService;

    @Value("${dashscope.api-key:}")
    private String dashscopeApiKeyFromConfig;
    
    private static final int AI_TIMEOUT_SECONDS = 60; // 聊天响应超时60秒
    private static final int MAX_HISTORY_MESSAGES = 10; // 最多保留10条历史消息（避免上下文过长）

    public record ChatRequest(String message, Long scenicId, String conversationId) {}
    public record ChatResponse(String reply, String conversationId) {}

    @PostMapping("/message")
    public Result<ChatResponse> message(@RequestBody ChatRequest req,
                                        @RequestAttribute(value = "userId", required = false) Long userId,
                                        HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        // 找或建会话
        ChatConversation conv = null;
        if (req.conversationId() != null) {
            conv = conversationMapper.selectOne(new LambdaQueryWrapper<ChatConversation>().eq(ChatConversation::getConversationUuid, req.conversationId()));
        }
        if (conv == null) {
            conv = new ChatConversation();
            conv.setConversationUuid(UUID.randomUUID().toString());
            conv.setUserId(userId);
            conv.setScenicId(req.scenicId());
            conv.setLastMessage(req.message());
            conversationMapper.insert(conv);
        } else {
            conv.setLastMessage(req.message());
            conversationMapper.updateById(conv);
        }
        log.info("traceId={} chat.msg convUuid={}, userId={}, scenicId={}", traceId, conv.getConversationUuid(), userId, req.scenicId());

        // 保存用户消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setConversationId(conv.getId());
        userMsg.setRole("user");
        userMsg.setContent(req.message());
        userMsg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(userMsg);

        // 构造多轮上下文（最近若干条）
        List<ChatMessage> history = messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conv.getId())
                .orderByAsc(ChatMessage::getCreatedAt));
        java.util.ArrayList<Message> msgs = new java.util.ArrayList<>();
        
        // 获取当前日期时间
        LocalDateTime now = LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");
        String currentDateTime = now.format(formatter);
        
        // 构建六盘水景区专属的系统提示词
        String systemPrompt = String.format("""
                你是"游韵华章·时空旅测绘梦蓝图"旅游系统的智能客服助手，专门为贵州省六盘水市的旅游景区提供咨询服务。
                
                【重要时间信息】当前时间是：%s
                - 在回答时间相关问题时，必须基于这个准确时间
                - 不要使用过时的日期（如2024年等错误日期）
                - 涉及"今天"、"当前"、"现在"等词时，请参考上述时间
                
                【重要】你的所有回答必须以六盘水景区为主，不要介绍其他城市的景区。
                
                六盘水市主要旅游景区包括：
                1. 梅花山风景区（钟山区）- 六盘水的城市名片，以梅花观赏、生态休闲为特色
                2. 乌蒙大草原 - 被誉为"贵州屋脊"，有高山草原、风电场等景观
                3. 明湖国家湿地公园 - 城市中的生态湿地，适合休闲观光
                4. 玉舍国家森林公园 - 拥有原始森林、滑雪场等，四季皆宜
                5. 水城古镇 - 历史文化古镇，展现地方民俗风情
                
                当用户询问景区信息时：
                - 默认指的是六盘水的景区，而不是其他城市的同名景区
                - 重点介绍六盘水当地的特色、气候、交通、美食等信息
                - 如果用户明确询问其他城市的景区，可以礼貌说明本系统主要服务六盘水旅游
                
                回答风格：专业、热情、详细，突出六盘水"中国凉都"的特色。
                """, currentDateTime);
        
        msgs.add(Message.builder().role(Role.SYSTEM.getValue()).content(systemPrompt).build());
        
        // 限制历史消息数量，避免上下文过长导致响应变慢
        int startIndex = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
        List<ChatMessage> recentHistory = history.subList(startIndex, history.size());
        
        log.debug("traceId={} history total={} used={}", traceId, history.size(), recentHistory.size());
        
        for (ChatMessage m : recentHistory) {
            String role = "user".equals(m.getRole()) ? Role.USER.getValue() : Role.ASSISTANT.getValue();
            msgs.add(Message.builder().role(role).content(m.getContent()).build());
        }

        String apiKey = (dashscopeApiKeyFromConfig != null && !dashscopeApiKeyFromConfig.isEmpty())
                ? dashscopeApiKeyFromConfig
                : System.getenv("DASHSCOPE_API_KEY");
        String model = "qwen-plus";
        String reply;
        
        if (aiService != null && apiKey != null && !apiKey.isEmpty()) {
            try {
                long startTime = System.currentTimeMillis();
                
                // 异步调用AI，带超时控制
                CompletableFuture<String> aiFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return aiService.chat(model, msgs, apiKey);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                
                try {
                    reply = aiFuture.get(AI_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("traceId={} ai.call ok model={} duration={}ms", traceId, model, duration);
                } catch (TimeoutException e) {
                    log.warn("traceId={} ai.call timeout after {}s", traceId, AI_TIMEOUT_SECONDS);
                    reply = "抱歉，AI服务响应超时，请稍后重试。";
                }
                
            } catch (Exception e) {
                reply = "抱歉，AI服务暂时不可用，请稍后重试。";
                log.warn("traceId={} ai.call fail: {}", traceId, e.toString());
            }
        } else {
            reply = "您好，我是智能助手。您说：" + req.message();
        }

        // 保存助手消息
        ChatMessage botMsg = new ChatMessage();
        botMsg.setConversationId(conv.getId());
        botMsg.setRole("assistant");
        botMsg.setContent(reply);
        botMsg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(botMsg);

        log.info("traceId={} chat.reply convUuid={} length={}", traceId, conv.getConversationUuid(), reply == null ? 0 : reply.length());
        return Result.success(new ChatResponse(reply, conv.getConversationUuid()));
    }

    @GetMapping("/history/{conversationId}")
    public Result<List<ChatMessage>> history(@PathVariable String conversationId) {
        ChatConversation conv = conversationMapper.selectOne(new LambdaQueryWrapper<ChatConversation>().eq(ChatConversation::getConversationUuid, conversationId));
        if (conv == null) return Result.success(java.util.Collections.emptyList());
        List<ChatMessage> list = messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conv.getId())
                .orderByAsc(ChatMessage::getCreatedAt));
        return Result.success(list);
    }

    @DeleteMapping("/history/{conversationId}")
    public Result<Void> clear(@PathVariable String conversationId) {
        ChatConversation conv = conversationMapper.selectOne(new LambdaQueryWrapper<ChatConversation>().eq(ChatConversation::getConversationUuid, conversationId));
        if (conv != null) {
            messageMapper.delete(new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getConversationId, conv.getId()));
        }
        return Result.success(null, "已清空");
    }
}


