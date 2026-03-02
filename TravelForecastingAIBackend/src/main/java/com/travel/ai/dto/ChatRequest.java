package com.travel.ai.dto;

import lombok.Data;

/**
 * 聊天请求DTO
 */
@Data
public class ChatRequest {
    /** 用户消息 */
    private String message;
    /** 关联景区ID */
    private Long scenicId;
    /** 会话ID (可选，为空时创建新会话) */
    private String conversationId;
}
