package com.travel.dto.scenic;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 用户端景区详情VO
 */
@Data
public class ScenicDetailVO {
    private Long id;
    private String name;
    private String description;
    private String fullDescription;
    private String imageUrl;
    private Double rating;
    private Integer currentFlow;
    private Integer maxCapacity;
    private String category;
    private String level;
    private String address;
    private String openingHours;
    private String price;
    private String suggestedTime;
    private Integer reviewCount;
    private List<String> tags;
    private Double latitude;
    private Double longitude;
    private List<String> images;
    private Map<String, Object> weather;
}

