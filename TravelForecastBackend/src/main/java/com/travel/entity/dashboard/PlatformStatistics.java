package com.travel.entity.dashboard;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 平台统计数据实体
 */
@Data
@TableName("platform_statistics")
public class PlatformStatistics {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 统计日期
     */
    private LocalDate statDate;
    
    /**
     * 总用户数
     */
    private Integer totalUsers;
    
    /**
     * 总收入
     */
    private BigDecimal totalRevenue;
    
    /**
     * 总商户数
     */
    private Integer totalMerchants;
    
    /**
     * 总景区数
     */
    private Integer totalScenics;
    
    /**
     * 当日游客量
     */
    private Integer dailyVisitors;
    
    /**
     * 当日订单量
     */
    private Integer dailyOrders;
    
    /**
     * 当日收入
     */
    private BigDecimal dailyRevenue;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

