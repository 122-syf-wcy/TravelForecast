package com.travel.controller.prediction;

import com.travel.service.prediction.PredictionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客流预测控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/prediction")
public class PredictionController {

    @Autowired
    private PredictionService predictionService;

    /**
     * 获取未来7天总客流预测
     */
    @GetMapping("/next-7-days-total")
    public Map<String, Object> getNext7DaysTotal(
            @RequestParam(required = false, defaultValue = "dual_stream") String model) {
        log.info("获取未来7天总客流预测: model={}", model);
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = predictionService.predictNext7DaysTotal(model);
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取未来7天总客流预测失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取景区客流趋势预测
     */
    @GetMapping("/scenic-trend")
    public Map<String, Object> getScenicTrend(
            @RequestParam(required = false) Long scenicId,
            @RequestParam(required = false, defaultValue = "hybrid") String model,
            @RequestParam(required = false, defaultValue = "7") Integer days,
            @RequestParam(required = false) List<String> factors) {
        log.info("获取景区客流趋势预测: scenicId={}, model={}, days={}, factors={}", scenicId, model, days, factors);
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = predictionService.predictScenicTrend(scenicId, model, days, factors);
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取景区客流趋势预测失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取小时客流分布预测
     */
    @GetMapping("/hourly-distribution")
    public Map<String, Object> getHourlyDistribution(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Long scenicId,
            @RequestParam(required = false, defaultValue = "dual_stream") String model) {
        log.info("获取小时客流分布预测: date={}, scenicId={}, model={}", date, scenicId, model);
        Map<String, Object> result = new HashMap<>();
        try {
            LocalDate predictDate = date != null ? LocalDate.parse(date) : LocalDate.now().plusDays(1);
            Map<String, Object> data = predictionService.predictHourlyDistribution(predictDate, scenicId, model);
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取小时客流分布预测失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取高峰预警
     */
    @GetMapping("/peak-warning")
    public Map<String, Object> getPeakWarning() {
        log.info("获取高峰预警");
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = predictionService.getPeakWarning();
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取高峰预警失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取预测详情列表
     */
    @GetMapping("/details")
    public Map<String, Object> getPredictionDetails(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        log.info("获取预测详情列表: page={}, pageSize={}", page, pageSize);
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = predictionService.getPredictionDetails(page, pageSize);
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取预测详情列表失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取最佳游览时段
     */
    @GetMapping("/best-visit-time")
    public Map<String, Object> getBestVisitTime() {
        log.info("获取最佳游览时段");
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = predictionService.getBestVisitTime();
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取最佳游览时段失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }
}
