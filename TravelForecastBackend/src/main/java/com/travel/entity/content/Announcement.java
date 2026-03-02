package com.travel.entity.content;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实时公告实体
 */
@Data
@TableName("announcements")
public class Announcement {
    
    /**
     * 公告ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 景区ID
     */
    private Long scenicId;
    
    /**
     * 发布商家ID
     */
    private Long merchantId;
    
    /**
     * 公告标题
     */
    private String title;
    
    /**
     * 公告内容
     */
    private String content;
    
    /**
     * 公告类型：normal-普通, important-重要, urgent-紧急
     */
    private String type;
    
    /**
     * 状态：active-启用, inactive-停用
     */
    private String status;
    
    /**
     * 优先级，数字越大越靠前
     */
    private Integer priority;
    
    /**
     * 开始显示时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束显示时间
     */
    private LocalDateTime endTime;
    
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

