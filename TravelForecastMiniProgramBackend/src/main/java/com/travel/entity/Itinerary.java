package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mp_itineraries")
public class Itinerary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private Integer days;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
