package com.travel.dto.dashboard;

import lombok.Data;

/**
 * 热门景点DTO
 */
@Data
public class PopularSpot {
    private Long id;
    private String name;
    private String image;
    private String description;
    /** 星级评分（基于收藏数计算） */
    private Double rating;
    /** 收藏数 */
    private Integer favoritesCount;
    /** 收藏增长率（相比昨天） */
    private Double growthRate;
    private String category;
}

