package com.travel.dto.admin;

import lombok.Data;

/**
 * 用户统计数据DTO
 */
@Data
public class UserStats {
    private Integer totalVisits;
    private Integer savedPlans;
    private Integer favoriteSpots;
    private Integer reviewsCount;
}

