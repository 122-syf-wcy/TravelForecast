package com.travel.entity.scenic;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 景区排行数据实体
 */
@Data
@TableName("scenic_rankings")
public class ScenicRanking {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 景区ID
     */
    private Long scenicId;
    
    /**
     * 排名日期
     */
    private LocalDate rankingDate;
    
    /**
     * 当日游客量
     */
    private Integer dailyVisitors;
    
    /**
     * 当日收入
     */
    private BigDecimal dailyRevenue;
    
    /**
     * 当日订单量
     */
    private Integer dailyOrders;
    
    /**
     * 评分
     */
    private BigDecimal rating;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

