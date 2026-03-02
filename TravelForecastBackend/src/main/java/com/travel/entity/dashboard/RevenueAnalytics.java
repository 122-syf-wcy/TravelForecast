package com.travel.entity.dashboard;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 收入分析实体
 */
@Data
@TableName("revenue_analytics")
public class RevenueAnalytics {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 统计日期
     */
    private LocalDate statDate;
    
    /**
     * 门票收入
     */
    private BigDecimal ticketRevenue;
    
    /**
     * 餐饮收入
     */
    private BigDecimal mealRevenue;
    
    /**
     * 住宿收入
     */
    private BigDecimal accommodationRevenue;
    
    /**
     * 购物收入
     */
    private BigDecimal shoppingRevenue;
    
    /**
     * 其他收入
     */
    private BigDecimal otherRevenue;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

