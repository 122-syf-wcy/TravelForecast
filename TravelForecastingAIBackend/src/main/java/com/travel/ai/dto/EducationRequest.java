package com.travel.ai.dto;

import lombok.Data;
import java.util.List;

/**
 * 研学教育请求DTO
 */
@Data
public class EducationRequest {
    /** 年龄段: primary/middle/high/college/all */
    private String ageGroup;
    /** 主题: geography/history/ecology/culture */
    private String theme;
    /** 天数 */
    private Integer days;
    /** 选择的景区 */
    private List<String> attractions;
    /** 学习目标 */
    private List<String> objectives;
}
