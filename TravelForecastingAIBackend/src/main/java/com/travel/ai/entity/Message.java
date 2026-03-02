package com.travel.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI聊天消息表
 */
@Data
@TableName("ai_messages")
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 会话ID */
    private Long conversationId;

    /** 角色: user/assistant/system */
    private String role;

    /** 消息内容 */
    private String content;

    /** 消息类型: text/image/audio */
    private String contentType;

    /** Token消耗数 */
    private Integer tokenUsage;

    /** 使用的模型 */
    private String model;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
