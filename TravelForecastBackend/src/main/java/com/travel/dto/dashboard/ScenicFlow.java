package com.travel.dto.dashboard;

import lombok.Data;

/**
 * 景区实时流量DTO
 */
@Data
public class ScenicFlow {
    private Long id;
    private String name;
    private Integer currentFlow;
    private Integer maxCapacity;
    private Double flowRate;
    /** 趋势: up/down/stable */
    private String trend;
}

