package com.travel.entity.prediction;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预测任务表
 */
@Data
@TableName(value = "predictions", autoResultMap = true)
public class Prediction implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String predictionCode;

    private Long userId;

    private Long scenicId;

    private LocalDate dateStart;

    private LocalDate dateEnd;

    private String modelType;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> includeFactors;

    private String status;

    private Integer progress;

    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}

