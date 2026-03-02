package com.travel.entity.merchant;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商家待办事项表
 */
@Data
@TableName("business_todos")
public class BusinessTodo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long merchantId;

    private String title;

    private String description;

    private String priority;

    private LocalDate deadline;

    private Boolean completed;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

