package com.travel.dto.dashboard;

import lombok.Data;

/**
 * 旅游资讯DTO
 */
@Data
public class TourismNews {
    private Long id;
    private String title;
    private String content;
    private String publishTime;
    /** 分类: notice/event/promotion */
    private String category;
    private Boolean important;
}

