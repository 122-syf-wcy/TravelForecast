package com.travel.entity.admin;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 平台活动日志实体
 */
@Data
@TableName("platform_activity_logs")
public class PlatformActivityLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 活动时间
     */
    private LocalDateTime activityTime;
    
    /**
     * 活动类型
     */
    private String activityType;
    
    /**
     * 活动内容
     */
    private String content;
    
    /**
     * 操作人
     */
    private String operator;
    
    /**
     * 操作人ID
     */
    private Long operatorId;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

