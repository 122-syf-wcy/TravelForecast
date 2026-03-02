package com.travel.entity.system;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统通知实体
 */
@Data
@TableName("system_notifications")
public class SystemNotification {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 通知标题
     */
    private String title;
    
    /**
     * 通知内容
     */
    private String content;
    
    /**
     * 通知类型: warning-警告, info-信息, success-成功, error-错误
     */
    private String type;
    
    /**
     * 目标角色: all-所有人, merchant-商家, admin-管理员, user-用户
     */
    private String targetRole;
    
    /**
     * 优先级，数字越大越靠前
     */
    private Integer priority;
    
    /**
     * 状态: active-启用, inactive-停用
     */
    private String status;
    
    /**
     * 过期时间，NULL表示永不过期
     */
    private LocalDateTime expiresAt;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

