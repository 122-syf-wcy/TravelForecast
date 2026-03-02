package com.travel.controller.merchant;

import com.travel.service.merchant.MerchantAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 商家数据分析控制器
 */
@Slf4j
@RestController
@RequestMapping("/merchant/analysis")
public class MerchantAnalysisController {

    @Autowired
    private MerchantAnalysisService analysisService;

    /**
     * 获取数据概览
     */
    @GetMapping("/overview")
    public Map<String, Object> getOverview(
            @RequestParam(required = false) Long scenicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("获取数据概览: scenicId={}, 日期范围={} to {}", scenicId, startDate, endDate);
        
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = analysisService.getAnalysisOverview(scenicId, startDate, endDate);
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取数据概览失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取游客量趋势
     */
    @GetMapping("/visitor-trend")
    public Map<String, Object> getVisitorTrend(
            @RequestParam(required = false) Long scenicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "daily") String metric) {
        log.info("获取游客量趋势: scenicId={}, 日期范围={} to {}, metric={}", scenicId, startDate, endDate, metric);
        
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = analysisService.getVisitorTrend(scenicId, startDate, endDate, metric);
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取游客量趋势失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取收入分析
     */
    @GetMapping("/revenue-analysis")
    public Map<String, Object> getRevenueAnalysis(
            @RequestParam(required = false) Long scenicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "all") String type) {
        log.info("获取收入分析: scenicId={}, 日期范围={} to {}, type={}", scenicId, startDate, endDate, type);
        
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = analysisService.getRevenueAnalysis(scenicId, startDate, endDate, type);
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取收入分析失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取游客来源
     */
    @GetMapping("/visitor-source")
    public Map<String, Object> getVisitorSource(
            @RequestParam(required = false) Long scenicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("获取游客来源: scenicId={}, 日期范围={} to {}", scenicId, startDate, endDate);
        
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = analysisService.getVisitorSource(scenicId, startDate, endDate);
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取游客来源失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取年龄分布
     */
    @GetMapping("/age-distribution")
    public Map<String, Object> getAgeDistribution(
            @RequestParam(required = false) Long scenicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("获取年龄分布: scenicId={}, 日期范围={} to {}", scenicId, startDate, endDate);
        
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = analysisService.getAgeDistribution(scenicId, startDate, endDate);
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取年龄分布失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取满意度分析
     */
    @GetMapping("/satisfaction-analysis")
    public Map<String, Object> getSatisfactionAnalysis(
            @RequestParam(required = false) Long scenicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("获取满意度分析: scenicId={}, 日期范围={} to {}", scenicId, startDate, endDate);
        
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = analysisService.getSatisfactionAnalysis(scenicId, startDate, endDate);
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取满意度分析失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取景点热度排行
     */
    @GetMapping("/spot-ranking")
    public Map<String, Object> getSpotRanking(
            @RequestParam(required = false) Long scenicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("获取景点热度排行: scenicId={}, 日期范围={} to {}", scenicId, startDate, endDate);
        
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = analysisService.getSpotHotRanking(scenicId, startDate, endDate);
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取景点热度排行失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }
}


