package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("mp_itinerary_items")
public class ItineraryItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long itineraryId;
    private Integer dayNum;
    private String timeSlot;
    private String title;
    private String description;
    private Long scenicId;
    private String imageUrl;
    private Integer sortOrder;
}
