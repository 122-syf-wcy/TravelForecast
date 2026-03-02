package com.travel.entity.prediction;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预测结果表
 */
@Data
@TableName("prediction_results")
public class PredictionResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long predictionId;

    private LocalDate predictDate;

    private Integer predictHour;

    private Integer expectedFlow;

    private Integer confidenceLower;

    private Integer confidenceUpper;

    private BigDecimal confidenceLevel;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

