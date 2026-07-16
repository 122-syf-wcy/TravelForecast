package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("mp_coupons")
public class Coupon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String type;
    private BigDecimal discount;
    private BigDecimal minAmount;
    private String condition;
    private String status;
    private LocalDateTime expireTime;
    private LocalDateTime createdAt;
}
