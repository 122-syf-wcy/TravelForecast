package com.travel.entity.user;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户分布数据实体
 */
@Data
@TableName("user_distribution")
public class UserDistribution {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 分布类型: region/age/gender
     */
    private String distributionType;
    
    /**
     * 类别
     */
    private String category;
    
    /**
     * 用户数量
     */
    private Integer userCount;
    
    /**
     * 百分比
     */
    private BigDecimal percentage;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

