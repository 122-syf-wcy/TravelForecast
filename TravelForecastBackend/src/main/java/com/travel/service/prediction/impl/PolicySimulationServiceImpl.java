package com.travel.service.prediction.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.entity.scenic.ScenicStatistics;
import com.travel.mapper.scenic.ScenicStatisticsMapper;
import com.travel.service.prediction.PolicySimulationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * 政策沙盘模拟服务实现类
 * 基于真实历史数据计算政策效果
 */
@Slf4j
@Service
public class PolicySimulationServiceImpl implements PolicySimulationService {
    
    @Autowired
    private ScenicStatisticsMapper statisticsMapper;
    
    @Override
    public Map<String, Object> simulatePolicyEffect(double discount, double subsidy, int capacity) {
        log.info("模拟政策效果: discount={}%, subsidy={}元, capacity={}人", discount, subsidy, capacity);
        
        // 获取最近7天的历史数据作为基准
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(7);
        
        List<ScenicStatistics> historicalData = statisticsMapper.selectList(
            new LambdaQueryWrapper<ScenicStatistics>()
                .between(ScenicStatistics::getStatDate, startDate, today)
                .orderByAsc(ScenicStatistics::getStatDate)
        );
        
        // 计算基准指标
        double baseFlow = calculateBaseFlow(historicalData);
        double baseRevenue = calculateBaseRevenue(historicalData);
        
        // 计算政策效果指标
        double flowIndex = calculateFlowIndex(baseFlow, discount, subsidy, capacity);
        double incomeIndex = calculateIncomeIndex(baseRevenue, flowIndex, discount);
        double carbonIndex = calculateCarbonIndex(flowIndex, subsidy);
        
        // 生成7日模拟数据
        List<Map<String, Object>> dailyData = generateDailySimulation(flowIndex, incomeIndex, carbonIndex);
        
        // 计算客单价提升
        double priceBoost = 18.0 * (discount / 20.0 + 1.0);
        
        Map<String, Object> result = new HashMap<>();
        result.put("flowIndex", Math.round(flowIndex * 100.0) / 100.0);
        result.put("incomeIndex", Math.round(incomeIndex * 100.0) / 100.0);
        result.put("carbonIndex", Math.round(carbonIndex * 100.0) / 100.0);
        result.put("priceBoost", Math.round(priceBoost * 10.0) / 10.0);
        result.put("dailyData", dailyData);
        
        log.info("政策模拟完成: flowIndex={}, incomeIndex={}, carbonIndex={}", 
            result.get("flowIndex"), result.get("incomeIndex"), result.get("carbonIndex"));
        
        return result;
    }
    
    /**
     * 计算基准客流（最近7天平均）
     */
    private double calculateBaseFlow(List<ScenicStatistics> data) {
        if (data.isEmpty()) {
            return 5000.0; // 默认值
        }
        
        // 按日期聚合
        Map<LocalDate, Integer> dailyFlow = new HashMap<>();
        for (ScenicStatistics stat : data) {
            dailyFlow.merge(stat.getStatDate(), stat.getVisitorCount(), Integer::sum);
        }
        
        return dailyFlow.values().stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(5000.0);
    }
    
    /**
     * 计算基准收入（最近7天平均）
     */
    private double calculateBaseRevenue(List<ScenicStatistics> data) {
        if (data.isEmpty()) {
            return 400000.0; // 默认值
        }
        
        // 按日期聚合
        Map<LocalDate, BigDecimal> dailyRevenue = new HashMap<>();
        for (ScenicStatistics stat : data) {
            dailyRevenue.merge(stat.getStatDate(), stat.getRevenue(), BigDecimal::add);
        }
        
        return dailyRevenue.values().stream()
            .mapToDouble(BigDecimal::doubleValue)
            .average()
            .orElse(400000.0);
    }
    
    /**
     * 计算客流指数
     * 考虑因素：折扣刺激、补贴效应、容量限制
     */
    private double calculateFlowIndex(double baseFlow, double discount, double subsidy, int capacity) {
        // 折扣效应：每1%折扣提升2%客流
        double discountEffect = 1.0 + (discount * 0.02);
        
        // 补贴效应：每5元补贴提升2.5%客流
        double subsidyEffect = 1.0 + (subsidy * 0.005);
        
        // 容量限制效应
        double predictedFlow = baseFlow * discountEffect * subsidyEffect;
        double capacityEffect = Math.min(1.0, capacity / predictedFlow);
        
        // 综合客流指数（相对于基准的倍数）
        double flowIndex = (predictedFlow / baseFlow) * capacityEffect;
        
        return Math.max(0.5, Math.min(2.5, flowIndex)); // 限制在0.5-2.5倍之间
    }
    
    /**
     * 计算收入指数
     * 考虑因素：客流变化、客单价提升（联票效应）
     */
    private double calculateIncomeIndex(double baseRevenue, double flowIndex, double discount) {
        // 联票折扣刺激客单价提升（联票刺激联购行为）
        double priceBoost = 1.0 + (discount / 100.0) * 0.6;
        
        // 收入指数 = 客流指数 × 客单价提升
        double incomeIndex = flowIndex * priceBoost;
        
        return Math.max(0.5, Math.min(3.0, incomeIndex));
    }
    
    /**
     * 计算碳足迹指数
     * 考虑因素：客流增加、交通方式改善（补贴促进公共交通）
     */
    private double calculateCarbonIndex(double flowIndex, double subsidy) {
        // 基础碳排放与客流成正比
        double baseCarbon = flowIndex;
        
        // 交通补贴降低人均碳强度（鼓励公共交通）
        // 每10元补贴降低5%碳强度
        double carbonReduction = 1.0 - (subsidy * 0.005);
        
        // 碳足迹指数
        double carbonIndex = baseCarbon * Math.max(0.6, carbonReduction);
        
        return Math.max(0.2, Math.min(2.0, carbonIndex));
    }
    
    /**
     * 生成7日模拟数据
     * 在基准指标上加入合理波动
     */
    private List<Map<String, Object>> generateDailySimulation(double flowIndex, double incomeIndex, double carbonIndex) {
        List<Map<String, Object>> dailyData = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 1; i <= 7; i++) {
            // 添加±5%的随机波动，模拟实际不确定性
            double flowVariation = 0.95 + random.nextDouble() * 0.1;
            double incomeVariation = 0.95 + random.nextDouble() * 0.1;
            double carbonVariation = 0.95 + random.nextDouble() * 0.1;
            
            Map<String, Object> day = new HashMap<>();
            day.put("day", "D" + i);
            day.put("flow", Math.round(flowIndex * flowVariation * 1000.0) / 1000.0);
            day.put("income", Math.round(incomeIndex * incomeVariation * 1000.0) / 1000.0);
            day.put("carbon", Math.round(carbonIndex * carbonVariation * 1000.0) / 1000.0);
            
            dailyData.add(day);
        }
        
        return dailyData;
    }
}

