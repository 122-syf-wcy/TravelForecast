package com.travel.entity.system;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知阅读记录实体
 */
@Data
@TableName("notification_reads")
public class NotificationRead {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 通知ID
     */
    private Long notificationId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 阅读时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime readAt;
}

