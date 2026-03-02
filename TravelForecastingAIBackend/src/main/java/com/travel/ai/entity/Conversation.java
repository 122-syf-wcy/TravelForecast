package com.travel.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI聊天会话表
 */
@Data
@TableName("ai_conversations")
public class Conversation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 会话UUID */
    private String conversationUuid;

    /** 用户ID */
    private Long userId;

    /** 关联景区ID */
    private Long scenicId;

    /** 会话类型: chat/planning/education */
    private String type;

    /** 会话标题 */
    private String title;

    /** 最后一条消息 */
    private String lastMessage;

    /** 消息数量 */
    private Integer messageCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
