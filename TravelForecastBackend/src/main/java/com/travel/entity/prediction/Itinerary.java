package com.travel.entity.prediction;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

/**
 * 行程表
 */
@Data
@TableName(value = "itineraries", autoResultMap = true)
public class Itinerary implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String itineraryCode;

    private Long userId;

    private String title;

    private LocalDate travelDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer days;

    private Integer partySize;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> preferences;

    private BigDecimal totalDistance;

    private Integer totalDuration;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

