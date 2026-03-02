package com.travel.dto.dashboard;

import lombok.Data;

/**
 * 天气信息DTO
 */
@Data
public class WeatherInfo {
    private String city;
    private Double temperature;
    private String condition;
    private Double humidity;
    private Double minTemp;
    private Double maxTemp;
    private String updateTime;
}

