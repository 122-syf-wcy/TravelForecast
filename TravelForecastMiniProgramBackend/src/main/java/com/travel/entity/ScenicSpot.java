package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "scenic_spots", autoResultMap = true)
public class ScenicSpot {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private Integer sortOrder;
    private String spotCode;
    private String name;
    private String city;
    private String category;
    private String level;
    private String address;
    private String description;
    private String fullDescription;
    private String imageUrl;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String openingHours;
    private String price;
    private Integer maxCapacity;
    private BigDecimal rating;
    private Integer reviewCount;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deletedAt;
}
