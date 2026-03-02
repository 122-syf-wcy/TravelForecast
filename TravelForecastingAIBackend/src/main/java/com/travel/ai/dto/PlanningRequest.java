package com.travel.ai.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * AI行程规划请求DTO
 */
@Data
public class PlanningRequest {
    /** 开始日期 "2025-10-16" */
    private String startDate;
    /** 结束日期 "2025-10-19" */
    private String endDate;
    /** 景区列表 */
    private List<String> attractions;
    /** 出行方式: car/public/tour */
    private String transportation;
    /** 人数 */
    private Integer peopleCount;
    /** 预算(元) */
    private Integer budget;
    /** 偏好列表 */
    private List<String> preferences;
    /** 各景区客流预测数据 */
    private Map<String, List<FlowPrediction>> flowPredictions;

    @Data
    public static class FlowPrediction {
        private String date;
        private Integer flow;
        /** 拥挤等级: low/medium/high */
        private String crowdLevel;
    }
}
