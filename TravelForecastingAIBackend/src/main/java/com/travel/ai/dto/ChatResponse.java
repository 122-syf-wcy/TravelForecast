package com.travel.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天响应DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponse {
    /** AI回复内容 */
    private String reply;
    /** 会话ID */
    private String conversationId;
}
