package com.travel.ai.controller;

import com.travel.ai.common.Result;
import com.travel.ai.dto.PlanningRequest;
import com.travel.ai.service.AiPlanningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI行程规划控制器
 * 迁移自业务后端，使用通义千问生成行程
 */
@RestController
@RequestMapping("/ai-planning")
@Tag(name = "AI行程规划", description = "智能行程规划生成")
public class AiPlanningController {

    private static final Logger log = LoggerFactory.getLogger(AiPlanningController.class);

    @Autowired
    private AiPlanningService aiPlanningService;

    /**
     * 生成AI行程规划
     */
    @PostMapping("/generate")
    @Operation(summary = "生成AI行程规划")
    public Result<?> generatePlan(@RequestBody PlanningRequest request) {
        log.info("收到行程规划请求: {}天 {}人 预算{}元",
                calculateDays(request.getStartDate(), request.getEndDate()),
                request.getPeopleCount(),
                request.getBudget());

        Map<String, Object> plan = aiPlanningService.generatePlan(request);
        return Result.success(plan);
    }

    private int calculateDays(String start, String end) {
        try {
            long s = java.time.LocalDate.parse(start).toEpochDay();
            long e = java.time.LocalDate.parse(end).toEpochDay();
            return (int) (e - s + 1);
        } catch (Exception ex) {
            return 0;
        }
    }
}
