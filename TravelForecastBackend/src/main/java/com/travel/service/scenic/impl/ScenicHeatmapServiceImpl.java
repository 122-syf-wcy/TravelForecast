package com.travel.service.scenic.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.travel.entity.scenic.ScenicStatistics;
import com.travel.mapper.scenic.ScenicStatisticsMapper;
import com.travel.service.scenic.ScenicHeatmapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * 景区热力图服务实现
 */
@Slf4j
@Service
public class ScenicHeatmapServiceImpl implements ScenicHeatmapService {
    
    @Autowired
    private ScenicStatisticsMapper scenicStatisticsMapper;
    
    @Override
    public Map<String, Object> getScenicHeatmapData(Long scenicId) {
        log.info("获取景区 {} 的热力图数据", scenicId);
        
        // 获取最近30天的统计数据
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        
        QueryWrapper<ScenicStatistics> wrapper = new QueryWrapper<>();
        wrapper.eq("scenic_id", scenicId)
                .between("stat_date", startDate, endDate)
                .orderByDesc("stat_date");
        
        List<ScenicStatistics> statistics = scenicStatisticsMapper.selectList(wrapper);
        
        if (statistics.isEmpty()) {
            log.warn("景区 {} 没有统计数据，返回空热力图数据", scenicId);
            Map<String, Object> empty = new HashMap<>();
            empty.put("time", new String[0]);
            empty.put("areas", new String[0]);
            empty.put("data", new ArrayList<>());
            empty.put("avgVisitorCount", 0);
            empty.put("isWeekendOrHoliday", false);
            empty.put("weatherFactor", 1.0);
            empty.put("noData", true);
            return empty;
        }
        
        // 计算平均访客数
        int avgVisitorCount = (int) statistics.stream()
                .mapToInt(ScenicStatistics::getVisitorCount)
                .average()
                .orElse(1000);
        
        log.info("景区 {} 最近30天平均访客数: {}", scenicId, avgVisitorCount);
        
        // 生成热力图数据
        return generateHeatmapData(avgVisitorCount, statistics);
    }
    
    /**
     * 基于真实数据生成热力图
     */
    private Map<String, Object> generateHeatmapData(int avgVisitorCount, List<ScenicStatistics> statistics) {
        Map<String, Object> result = new HashMap<>();
        
        // 时间段（8:00-17:00）
        String[] timeSlots = {"8:00", "9:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00"};
        result.put("time", timeSlots);
        
        // 景区区域
        String[] areas = {"入口区", "核心景点A", "核心景点B", "观景台", "休息区", "餐饮区", "商业区", "出口区"};
        result.put("areas", areas);
        
        // 生成热力数据
        List<List<Object>> data = new ArrayList<>();
        
        // 时间段客流分布权重（模拟真实客流高峰）
        double[] timeWeights = {0.3, 0.7, 1.0, 0.95, 0.6, 0.7, 0.85, 0.9, 0.8, 0.4};
        
        // 区域客流分布权重
        double[] areaWeights = {0.6, 1.0, 0.95, 0.85, 0.5, 0.7, 0.65, 0.4};
        
        // 考虑周末和节假日因素
        boolean isWeekendOrHoliday = statistics.stream()
                .limit(7)
                .anyMatch(s -> Boolean.TRUE.equals(s.getIsWeekend()) || Boolean.TRUE.equals(s.getIsHoliday()));
        
        double holidayFactor = isWeekendOrHoliday ? 1.3 : 1.0;
        
        // 考虑天气因素
        long rainyDays = statistics.stream()
                .limit(7)
                .filter(s -> s.getWeather() != null && s.getWeather().contains("雨"))
                .count();
        double weatherFactor = rainyDays > 3 ? 0.7 : 1.0;
        
        // 生成每个时间段和区域的数据
        for (int timeIndex = 0; timeIndex < timeSlots.length; timeIndex++) {
            for (int areaIndex = 0; areaIndex < areas.length; areaIndex++) {
                // 基础流量 = 平均访客数 * 时间权重 * 区域权重 * 节假日因素 * 天气因素
                double baseFlow = avgVisitorCount * timeWeights[timeIndex] * areaWeights[areaIndex] * holidayFactor * weatherFactor;
                
                // 基于时间和区域索引生成确定性波动（替代随机数）
                double deterministicFactor = 1.0 + 0.1 * Math.sin(timeIndex * 2.0 + areaIndex * 3.0);
                int flow = (int) (baseFlow * deterministicFactor);
                
                // 转换为百分比（0-100）
                int percentage = Math.min(100, Math.max(0, (int) ((flow / (double) avgVisitorCount) * 100)));
                
                // [时间索引, 区域索引, 人流百分比]
                data.add(Arrays.asList(timeIndex, areaIndex, percentage));
            }
        }
        
        result.put("data", data);
        result.put("avgVisitorCount", avgVisitorCount);
        result.put("isWeekendOrHoliday", isWeekendOrHoliday);
        result.put("weatherFactor", weatherFactor);
        
        log.info("生成热力图数据完成，数据点数量: {}", data.size());
        
        return result;
    }
    
    /**
     * 生成默认热力图数据（当没有历史数据时）
     */
    private Map<String, Object> generateDefaultHeatmapData() {
        Map<String, Object> result = new HashMap<>();
        
        String[] timeSlots = {"8:00", "9:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00"};
        String[] areas = {"入口区", "核心景点A", "核心景点B", "观景台", "休息区", "餐饮区", "商业区", "出口区"};
        
        result.put("time", timeSlots);
        result.put("areas", areas);
        
        List<List<Object>> data = new ArrayList<>();
        
        // 默认模拟数据
        int[][] defaultData = {
            {15, 35, 45, 38, 22, 18, 12, 8},
            {45, 68, 78, 65, 42, 35, 28, 15},
            {78, 95, 100, 88, 62, 55, 45, 25},
            {88, 98, 95, 92, 75, 68, 58, 32},
            {65, 72, 68, 70, 82, 78, 52, 28},
            {58, 68, 65, 68, 72, 85, 65, 35},
            {72, 82, 78, 75, 62, 70, 88, 45},
            {85, 92, 88, 82, 55, 62, 78, 58},
            {68, 75, 72, 68, 45, 52, 58, 68},
            {38, 48, 45, 42, 28, 32, 35, 78}
        };
        
        for (int i = 0; i < defaultData.length; i++) {
            for (int j = 0; j < defaultData[i].length; j++) {
                data.add(Arrays.asList(i, j, defaultData[i][j]));
            }
        }
        
        result.put("data", data);
        result.put("avgVisitorCount", 1000);
        result.put("isWeekendOrHoliday", false);
        result.put("weatherFactor", 1.0);
        
        return result;
    }
}

