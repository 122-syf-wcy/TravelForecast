package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mp_footprints")
public class Footprint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String targetId;
    private String targetType;
    private String title;
    private String imageUrl;
    private LocalDateTime createdAt;
}
