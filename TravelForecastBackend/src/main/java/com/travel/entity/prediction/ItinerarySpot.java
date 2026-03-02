package com.travel.entity.prediction;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 行程景点关联表
 */
@Data
@TableName("itinerary_spots")
public class ItinerarySpot implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long itineraryId;

    private Long scenicId;

    private Integer dayNumber;

    private Integer visitOrder;

    private Integer stayMinutes;

    private LocalTime arrivalTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

