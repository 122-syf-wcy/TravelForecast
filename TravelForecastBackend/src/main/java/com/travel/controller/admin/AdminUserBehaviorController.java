package com.travel.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.travel.common.Result;
import com.travel.mapper.user.UserBehaviorLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 管理员端-用户行为分析
 */
@Slf4j
@RestController
@RequestMapping("/admin/users/behavior")
public class AdminUserBehaviorController {

    @Autowired
    private UserBehaviorLogMapper userBehaviorLogMapper;

    /**
     * 获取行为分析统计数据
     * GET /admin/users/behavior/statistics
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getBehaviorStatistics() {
        log.info("管理员查询用户行为统计数据");
        
        Map<String, Object> result = new HashMap<>();
        
        // 总访问量（最近30天）
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long totalVisits = userBehaviorLogMapper.selectCount(
            new QueryWrapper<com.travel.entity.user.UserBehaviorLog>().ge("created_at", thirtyDaysAgo)
        );
        
        // 日均访问量
        long dailyAvgVisits = totalVisits / 30;
        
        // 平均停留时长（分钟）
        Double avgDuration = userBehaviorLogMapper.getAverageDuration(thirtyDaysAgo);
        
        // 独立访客数（UV）
        long uniqueVisitors = userBehaviorLogMapper.countUniqueUsers(thirtyDaysAgo);
        
        result.put("totalVisits", totalVisits);
        result.put("dailyAvgVisits", dailyAvgVisits);
        result.put("avgDuration", avgDuration != null ? Math.round(avgDuration / 60.0 * 10) / 10.0 : 0);
        result.put("uniqueVisitors", uniqueVisitors);
        
        return Result.success(result);
    }

    /**
     * 获取访问时段分布
     * GET /admin/users/behavior/time-distribution
     */
    @GetMapping("/time-distribution")
    public Result<Map<String, Object>> getTimeDistribution() {
        log.info("管理员查询访问时段分布");
        
        List<Map<String, Object>> distribution = userBehaviorLogMapper.getHourlyDistribution();
        
        Map<String, Object> result = new HashMap<>();
        result.put("distribution", distribution);
        
        return Result.success(result);
    }

    /**
     * 获取功能使用排行
     * GET /admin/users/behavior/module-ranking
     */
    @GetMapping("/module-ranking")
    public Result<List<Map<String, Object>>> getModuleRanking() {
        log.info("管理员查询功能使用排行");
        
        List<Map<String, Object>> ranking = userBehaviorLogMapper.getModuleRanking();
        
        return Result.success(ranking);
    }

    /**
     * 获取停留时长分布
     * GET /admin/users/behavior/duration-distribution
     */
    @GetMapping("/duration-distribution")
    public Result<List<Map<String, Object>>> getDurationDistribution() {
        log.info("管理员查询停留时长分布");
        
        List<Map<String, Object>> distribution = userBehaviorLogMapper.getDurationDistribution();
        
        return Result.success(distribution);
    }

    /**
     * 获取热门页面排行
     * GET /admin/users/behavior/hot-pages
     */
    @GetMapping("/hot-pages")
    public Result<List<Map<String, Object>>> getHotPages() {
        log.info("管理员查询热门页面排行");
        
        List<Map<String, Object>> hotPages = userBehaviorLogMapper.getHotPages();
        
        return Result.success(hotPages);
    }
}

