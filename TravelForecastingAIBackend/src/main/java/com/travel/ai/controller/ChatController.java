package com.travel.ai.controller;

import com.travel.ai.common.Result;
import com.travel.ai.dto.ChatRequest;
import com.travel.ai.dto.ChatResponse;
import com.travel.ai.entity.Message;
import com.travel.ai.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI聊天控制器
 * 迁移自业务后端，提供智能对话功能
 */
@RestController
@RequestMapping("/chat")
@Tag(name = "AI聊天", description = "智能旅游咨询对话")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private AiChatService aiChatService;

    /**
     * 发送聊天消息
     */
    @PostMapping("/message")
    @Operation(summary = "发送聊天消息")
    public Result<ChatResponse> sendMessage(@RequestBody ChatRequest request,
                                            @RequestAttribute(value = "userId", required = false) Long userId,
                                            HttpServletRequest httpRequest) {
        String traceId = httpRequest.getHeader("X-Trace-Id");
        log.info("traceId={} chat.message userId={}, scenicId={}", traceId, userId, request.getScenicId());

        ChatResponse response = aiChatService.chat(request, userId);
        return Result.success(response);
    }

    /**
     * 获取会话历史
     */
    @GetMapping("/history/{conversationId}")
    @Operation(summary = "获取会话历史")
    public Result<List<Message>> getHistory(@PathVariable String conversationId) {
        List<Message> messages = aiChatService.getHistory(conversationId);
        return Result.success(messages);
    }

    /**
     * 清空会话历史
     */
    @DeleteMapping("/history/{conversationId}")
    @Operation(summary = "清空会话历史")
    public Result<Void> clearHistory(@PathVariable String conversationId) {
        aiChatService.clearHistory(conversationId);
        return Result.success(null, "已清空");
    }

    /**
     * 获取用户会话列表
     */
    @GetMapping("/conversations")
    @Operation(summary = "获取用户会话列表")
    public Result<?> getUserConversations(@RequestAttribute(value = "userId", required = false) Long userId) {
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        return Result.success(aiChatService.getUserConversations(userId));
    }
}
