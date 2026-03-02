package com.travel.ai.service;

import com.travel.ai.dto.ChatRequest;
import com.travel.ai.dto.ChatResponse;
import com.travel.ai.entity.Message;

import java.util.List;

/**
 * AI聊天服务接口
 */
public interface AiChatService {

    /**
     * 发送聊天消息
     */
    ChatResponse chat(ChatRequest request, Long userId);

    /**
     * 获取会话历史
     */
    List<Message> getHistory(String conversationId);

    /**
     * 清空会话历史
     */
    void clearHistory(String conversationId);

    /**
     * 获取用户的所有会话列表
     */
    List<?> getUserConversations(Long userId);
}
