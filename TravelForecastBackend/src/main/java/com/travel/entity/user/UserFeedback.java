package com.travel.entity.user;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户反馈实体
 */
@Data
@TableName("user_feedbacks")
public class UserFeedback {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 反馈类型
     */
    private String feedbackType;
    
    /**
     * 反馈标题
     */
    private String title;
    
    /**
     * 反馈内容
     */
    private String content;
    
    /**
     * 优先级
     */
    private String priority;
    
    /**
     * 处理状态
     */
    private String status;
    
    /**
     * 管理员回复
     */
    private String adminReply;
    
    /**
     * 回复时间
     */
    private LocalDateTime repliedAt;
    
    /**
     * 回复人ID
     */
    private Long repliedBy;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

