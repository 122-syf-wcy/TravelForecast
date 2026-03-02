package com.travel.ai.controller;

import com.travel.ai.common.Result;
import com.travel.ai.dto.EducationRequest;
import com.travel.ai.entity.StudyRoute;
import com.travel.ai.service.EducationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 研学教育控制器
 * 提供研学路线管理和AI生成研学方案
 */
@RestController
@RequestMapping("/education")
@Tag(name = "研学教育", description = "研学路线管理与智能方案生成")
public class EducationController {

    @Autowired
    private EducationService educationService;

    /**
     * 获取研学路线列表
     */
    @GetMapping("/routes")
    @Operation(summary = "获取研学路线列表")
    public Result<List<StudyRoute>> listRoutes(
            @RequestParam(required = false) String ageGroup,
            @RequestParam(required = false) String theme) {
        return Result.success(educationService.listRoutes(ageGroup, theme));
    }

    /**
     * 获取研学路线详情
     */
    @GetMapping("/routes/{id}")
    @Operation(summary = "获取研学路线详情")
    public Result<StudyRoute> getRouteDetail(@PathVariable Long id) {
        StudyRoute route = educationService.getRouteDetail(id);
        if (route == null) {
            return Result.error(404, "研学路线不存在");
        }
        return Result.success(route);
    }

    /**
     * AI生成研学方案
     */
    @PostMapping("/generate")
    @Operation(summary = "AI生成研学方案")
    public Result<?> generateStudyPlan(@RequestBody EducationRequest request) {
        Map<String, Object> plan = educationService.generateStudyPlan(request);
        return Result.success(plan);
    }

    /**
     * 添加研学路线
     */
    @PostMapping("/routes")
    @Operation(summary = "添加研学路线")
    public Result<StudyRoute> addRoute(@RequestBody StudyRoute route) {
        return Result.success(educationService.addRoute(route));
    }

    /**
     * 更新研学路线
     */
    @PutMapping("/routes/{id}")
    @Operation(summary = "更新研学路线")
    public Result<StudyRoute> updateRoute(@PathVariable Long id, @RequestBody StudyRoute route) {
        route.setId(id);
        return Result.success(educationService.updateRoute(route));
    }
}
