package com.travel.entity.merchant;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 待办事项实体
 */
@Data
@TableName("todos")
public class Todo {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 待办事项标题
     */
    private String title;
    
    /**
     * 优先级: urgent-紧急, high-高, medium-中, low-低
     */
    private String priority;
    
    /**
     * 截止日期
     */
    private LocalDate deadline;
    
    /**
     * 是否完成: 0-未完成, 1-已完成
     */
    private Boolean completed;
    
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

