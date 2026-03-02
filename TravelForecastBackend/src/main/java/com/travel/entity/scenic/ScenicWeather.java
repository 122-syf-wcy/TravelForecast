package com.travel.entity.scenic;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 景区天气数据表
 */
@Data
@TableName("scenic_weather")
public class ScenicWeather implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long scenicId;

    private BigDecimal temperature;

    private String weatherCondition;

    private Integer humidity;

    private BigDecimal windSpeed;

    private Integer aqi;

    private LocalDateTime timestamp;
}

