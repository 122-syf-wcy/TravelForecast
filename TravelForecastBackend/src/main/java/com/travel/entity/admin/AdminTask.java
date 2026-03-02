package com.travel.entity.admin;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 管理员待办事项实体
 */
@Data
@TableName("admin_tasks")
public class AdminTask {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 事项标题
     */
    private String title;
    
    /**
     * 详细说明
     */
    private String description;
    
    /**
     * 提交人
     */
    private String submitter;
    
    /**
     * 提交人ID
     */
    private Long submitterId;
    
    /**
     * 是否已处理
     */
    private Integer isProcessed;
    
    /**
     * 处理时间
     */
    private LocalDateTime processedAt;
    
    /**
     * 处理人
     */
    private String processedBy;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

