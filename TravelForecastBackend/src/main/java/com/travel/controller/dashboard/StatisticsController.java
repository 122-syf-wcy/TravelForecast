package com.travel.controller.dashboard;

import com.travel.service.dashboard.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 统计报表控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/statistics")
public class StatisticsController {
    
    @Autowired
    private StatisticsService statisticsService;
    
    /**
     * 获取旅游收入统计
     */
    @GetMapping("/tourism-income")
    public Map<String, Object> getTourismIncome(@RequestParam(defaultValue = "12m") String range) {
        log.info("获取旅游收入统计: range={}", range);
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = statisticsService.getTourismIncome(range);
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取旅游收入统计失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取游客来源分析
     */
    @GetMapping("/visitor-source")
    public Map<String, Object> getVisitorSource(@RequestParam(defaultValue = "12m") String range) {
        log.info("获取游客来源分析: range={}", range);
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = statisticsService.getVisitorSource(range);
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取游客来源分析失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取景区客流对比
     */
    @GetMapping("/scenic-comparison")
    public Map<String, Object> getScenicComparison(@RequestParam(defaultValue = "2025") String year) {
        log.info("获取景区客流对比: year={}", year);
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = statisticsService.getScenicComparison(year);
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取景区客流对比失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取详细统计数据（分页）
     */
    @GetMapping("/detailed-data")
    public Map<String, Object> getDetailedData(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        log.info("获取详细统计数据: page={}, pageSize={}, keyword={}", page, pageSize, keyword);
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = statisticsService.getDetailedData(page, pageSize, keyword);
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取详细统计数据失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }
}

