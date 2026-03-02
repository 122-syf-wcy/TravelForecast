package com.travel.entity.merchant;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商家资源表（景点/活动/设施）
 */
@Data
@TableName(value = "business_resources", autoResultMap = true)
public class BusinessResource implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long merchantId;

    private String resourceType;

    private String name;

    private String thumbnail;

    private String description;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    private String status;

    private Integer visitCount;

    private BigDecimal rating;

    private Integer capacity;

    private String openTime;

    private LocalDate startTime;

    private LocalDate endTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

