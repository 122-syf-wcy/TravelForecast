package com.travel.ai.service;

import com.travel.ai.dto.PlanningRequest;

import java.util.Map;

/**
 * AI行程规划服务接口
 */
public interface AiPlanningService {

    /**
     * 生成AI行程规划
     */
    Map<String, Object> generatePlan(PlanningRequest request);
}
