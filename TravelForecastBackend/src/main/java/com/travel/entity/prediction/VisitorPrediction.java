package com.travel.entity.prediction;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客流预测数据实体
 */
@Data
@TableName("visitor_predictions")
public class VisitorPrediction {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 景区ID（NULL表示全平台）
     */
    private Long scenicId;
    
    /**
     * 预测日期
     */
    private LocalDate predictionDate;
    
    /**
     * 预测游客量
     */
    private Integer predictedVisitors;
    
    /**
     * 置信度
     */
    private BigDecimal confidence;
    
    /**
     * 天气因素
     */
    private String weatherFactor;
    
    /**
     * 是否假日
     */
    private Integer holidayFactor;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

