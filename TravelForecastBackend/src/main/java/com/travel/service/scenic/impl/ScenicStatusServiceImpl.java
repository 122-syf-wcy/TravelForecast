package com.travel.service.scenic.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.dto.scenic.ScenicStatusDTO;
import com.travel.entity.order.TicketOrder;
import com.travel.entity.scenic.ScenicSpot;
import com.travel.entity.scenic.ScenicStatistics;
import com.travel.mapper.order.TicketOrderMapper;
import com.travel.mapper.scenic.ScenicSpotMapper;
import com.travel.mapper.scenic.ScenicStatisticsMapper;
import com.travel.service.scenic.ScenicStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 景区状态评估服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScenicStatusServiceImpl implements ScenicStatusService {
    
    private final ScenicSpotMapper scenicSpotMapper;
    private final RestTemplate restTemplate;
    private final TicketOrderMapper ticketOrderMapper;
    private final ScenicStatisticsMapper scenicStatisticsMapper;
    
    @org.springframework.beans.factory.annotation.Value("${python.prediction.service.url:http://localhost:8001}")
    private String predictionServiceUrl;
    
    @Override
    public ScenicStatusDTO getScenicStatus(Long scenicId) {
        log.info("开始评估景区状态: scenicId={}", scenicId);
        
        ScenicStatusDTO statusDTO = new ScenicStatusDTO();
        statusDTO.setScenicId(scenicId);
        statusDTO.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        try {
            // 1. 获取景区信息
            ScenicSpot scenic = scenicSpotMapper.selectById(scenicId);
            if (scenic == null) {
                log.warn("景区不存在: scenicId={}", scenicId);
                return createDefaultStatus(scenicId);
            }
            statusDTO.setScenicName(scenic.getName());
            
            // 2. 获取当前客流数据（基于今日订单和统计数据）
            Integer maxCapacity = scenic.getMaxCapacity() != null ? scenic.getMaxCapacity() : 1000;
            Integer currentFlow = getCurrentFlow(scenicId, maxCapacity);
            Double currentFlowRate = currentFlow.doubleValue() / maxCapacity;
            
            statusDTO.setCurrentFlow(currentFlow);
            statusDTO.setMaxCapacity(maxCapacity);
            statusDTO.setFlowRate(currentFlowRate);
            
            // 3. 调用预测服务获取未来1小时的预测客流
            Integer predictedFlow = getPredictedFlow(scenicId);
            Double predictedFlowRate = predictedFlow.doubleValue() / maxCapacity;
            
            statusDTO.setPredictedFlow(predictedFlow);
            statusDTO.setPredictedFlowRate(predictedFlowRate);
            
            // 4. 判断趋势
            String trend = determineTrend(currentFlow, predictedFlow);
            statusDTO.setTrend(trend);
            
            // 5. 评估运营状态
            String status = evaluateStatus(scenicId, currentFlow, predictedFlow, maxCapacity);
            statusDTO.setStatus(status);
            statusDTO.setStatusDescription(getStatusDescription(status));
            
            // 6. 计算建议等待时间
            Integer waitTime = calculateWaitTime(currentFlowRate, predictedFlowRate, status);
            statusDTO.setSuggestedWaitTime(waitTime);
            
            // 7. 生成建议
            generateSuggestion(statusDTO);
            
            log.info("景区状态评估完成: scenicId={}, status={}, currentFlow={}, predictedFlow={}, trend={}", 
                    scenicId, status, currentFlow, predictedFlow, trend);
            
            return statusDTO;
            
        } catch (Exception e) {
            log.error("评估景区状态失败: scenicId={}", scenicId, e);
            return createDefaultStatus(scenicId);
        }
    }
    
    @Override
    public String evaluateStatus(Long scenicId, Integer currentFlow, Integer predictedFlow, Integer maxCapacity) {
        // 计算当前和预测的客流率
        double currentRate = currentFlow.doubleValue() / maxCapacity;
        double predictedRate = predictedFlow.doubleValue() / maxCapacity;
        
        // 综合当前和预测客流率做判断（权重：当前60%，预测40%）
        double compositeScore = currentRate * 0.6 + predictedRate * 0.4;
        
        log.info("景区状态评估: scenicId={}, currentRate={}, predictedRate={}, 综合评分={}", 
                scenicId, currentRate, predictedRate, compositeScore);
        
        // 根据综合评分判断状态
        if (compositeScore >= 0.95) {
            return "limited";  // 需要限流
        } else if (compositeScore >= 0.80) {
            return "crowded";  // 拥挤
        } else if (compositeScore >= 0.60) {
            return "busy";     // 繁忙
        } else if (compositeScore >= 0.30) {
            return "normal";   // 正常
        } else {
            return "idle";     // 空闲
        }
    }
    
    /**
     * 调用Python预测服务获取未来客流（基于您的Python预测服务）
     */
    private Integer getPredictedFlow(Long scenicId) {
        try {
            log.info("调用Python预测服务: scenicId={}", scenicId);
            
            // 调用您的Python预测服务 GET /api/prediction/hourly/{scenic_id}
            // 获取今天的小时级预测，取未来1小时的预测值
            String date = java.time.LocalDate.now().toString(); // YYYY-MM-DD格式
            String url = String.format("%s/api/prediction/hourly/%d?date=%s", 
                    predictionServiceUrl, scenicId, date);
            
            log.info("请求URL: {}", url);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null) {
                log.info("Python预测服务返回: {}", response);
                
                // 解析响应数据
                // 响应格式: { "scenicId": 1, "scenicName": "...", "date": "...", "hourlyData": [...] }
                Object hourlyDataObj = response.get("hourlyData");
                
                if (hourlyDataObj instanceof List) {
                    List<Map<String, Object>> hourlyData = (List<Map<String, Object>>) hourlyDataObj;
                    
                    if (!hourlyData.isEmpty()) {
                        // 获取当前小时
                        int currentHour = java.time.LocalDateTime.now().getHour();
                        
                        // 找到未来1小时的预测数据
                        int targetHour = (currentHour + 1) % 24;
                        
                        for (Map<String, Object> hourData : hourlyData) {
                            Object hourObj = hourData.get("hour");
                            Object flowObj = hourData.get("expectedFlow");
                            
                            if (hourObj instanceof Number && flowObj instanceof Number) {
                                int hour = ((Number) hourObj).intValue();
                                if (hour == targetHour) {
                                    int predictedFlow = ((Number) flowObj).intValue();
                                    log.info("预测服务返回成功: scenicId={}, hour={}, predictedFlow={}", 
                                            scenicId, targetHour, predictedFlow);
                                    return predictedFlow;
                                }
                            }
                        }
                        
                        // 如果没找到精确小时，取第一个数据点
                        Map<String, Object> firstHour = hourlyData.get(0);
                        Object flowObj = firstHour.get("expectedFlow");
                        if (flowObj instanceof Number) {
                            int predictedFlow = ((Number) flowObj).intValue();
                            log.info("使用第一个小时预测: scenicId={}, predictedFlow={}", scenicId, predictedFlow);
                            return predictedFlow;
                        }
                    }
                }
            }
            
            log.warn("预测服务未返回有效数据，使用历史平均值");
            
        } catch (Exception e) {
            log.error("调用Python预测服务失败: scenicId={}", scenicId, e);
            log.error("错误详情: {}", e.getMessage());
        }
        
        // 如果预测服务调用失败，使用历史数据平均值
        return getHistoricalAverageFlow(scenicId);
    }
    
    /**
     * 获取当前客流（基于今日已使用/已支付的门票订单统计）
     */
    private Integer getCurrentFlow(Long scenicId, Integer maxCapacity) {
        try {
            // 从今日门票订单统计实际入园人数
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            LambdaQueryWrapper<TicketOrder> wrapper = new LambdaQueryWrapper<TicketOrder>()
                    .eq(TicketOrder::getScenicId, scenicId)
                    .ge(TicketOrder::getCreatedAt, todayStart)
                    .in(TicketOrder::getStatus, "paid", "used");
            List<TicketOrder> todayOrders = ticketOrderMapper.selectList(wrapper);

            int totalTickets = todayOrders.stream()
                    .mapToInt(o -> o.getTicketCount() != null ? o.getTicketCount() : 1)
                    .sum();

            if (totalTickets > 0) {
                log.info("今日实际客流: scenicId={}, tickets={}", scenicId, totalTickets);
                return totalTickets;
            }

            // 无今日订单时，查询最近统计数据
            LambdaQueryWrapper<ScenicStatistics> statWrapper = new LambdaQueryWrapper<ScenicStatistics>()
                    .eq(ScenicStatistics::getScenicId, scenicId)
                    .orderByDesc(ScenicStatistics::getStatDate)
                    .last("LIMIT 1");
            ScenicStatistics latestStat = scenicStatisticsMapper.selectOne(statWrapper);
            if (latestStat != null && latestStat.getVisitorCount() != null) {
                return latestStat.getVisitorCount();
            }

            return 0;
        } catch (Exception e) {
            log.error("获取当前客流失败: scenicId={}", scenicId, e);
            return 0;
        }
    }
    
    /**
     * 获取历史平均客流（作为预测失败时的降级方案）
     */
    private Integer getHistoricalAverageFlow(Long scenicId) {
        try {
            // 查询最近7天的平均客流
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(7);
            LambdaQueryWrapper<ScenicStatistics> wrapper = new LambdaQueryWrapper<ScenicStatistics>()
                    .eq(ScenicStatistics::getScenicId, scenicId)
                    .between(ScenicStatistics::getStatDate, startDate, endDate);
            List<ScenicStatistics> stats = scenicStatisticsMapper.selectList(wrapper);

            if (!stats.isEmpty()) {
                int avg = (int) stats.stream()
                        .mapToInt(ScenicStatistics::getVisitorCount)
                        .average()
                        .orElse(0);
                log.info("历史平均客流: scenicId={}, avg={}", scenicId, avg);
                return avg;
            }
            return 0;
        } catch (Exception e) {
            log.error("获取历史平均客流失败: scenicId={}", scenicId, e);
            return 0;
        }
    }
    
    /**
     * 判断客流趋势
     */
    private String determineTrend(Integer currentFlow, Integer predictedFlow) {
        if (currentFlow == null || currentFlow == 0) {
            return predictedFlow != null && predictedFlow > 0 ? "rising" : "stable";
        }
        double changeRate = (predictedFlow - currentFlow) * 100.0 / currentFlow;
        
        if (changeRate > 10) {
            return "rising";   // 上升
        } else if (changeRate < -10) {
            return "falling";  // 下降
        } else {
            return "stable";   // 稳定
        }
    }
    
    /**
     * 计算建议等待时间
     */
    private Integer calculateWaitTime(Double currentRate, Double predictedRate, String status) {
        switch (status) {
            case "limited":
                return 60;  // 建议等待60分钟
            case "crowded":
                return (int) (30 + currentRate * 30);  // 30-60分钟
            case "busy":
                return (int) (15 + currentRate * 20);  // 15-35分钟
            case "normal":
                return 5;   // 5分钟
            case "idle":
                return 0;   // 无需等待
            default:
                return 10;
        }
    }
    
    /**
     * 生成游览建议
     */
    private void generateSuggestion(ScenicStatusDTO statusDTO) {
        String status = statusDTO.getStatus();
        String trend = statusDTO.getTrend();
        
        StringBuilder suggestion = new StringBuilder();
        
        switch (status) {
            case "limited":
                statusDTO.setRecommended(false);
                suggestion.append("景区当前客流已达上限，建议");
                if ("falling".equals(trend)) {
                    suggestion.append("等待1-2小时后前往，届时客流将有所下降。");
                } else {
                    suggestion.append("改期或选择其他景区游览。");
                }
                break;
                
            case "crowded":
                statusDTO.setRecommended(false);
                suggestion.append("景区当前较为拥挤，");
                if ("falling".equals(trend)) {
                    suggestion.append("建议稍等片刻，预计1小时后客流将减少。");
                } else {
                    suggestion.append("建议避开高峰时段（14:00-16:00），选择上午或傍晚游览。");
                }
                break;
                
            case "busy":
                statusDTO.setRecommended("falling".equals(trend));
                suggestion.append("景区当前人流适中，");
                if ("rising".equals(trend)) {
                    suggestion.append("建议尽快入园，避开即将到来的高峰期。");
                } else if ("falling".equals(trend)) {
                    suggestion.append("预计客流将逐渐减少，是入园的好时机。");
                } else {
                    suggestion.append("游览体验良好，可以入园游玩。");
                }
                break;
                
            case "normal":
                statusDTO.setRecommended(true);
                suggestion.append("景区当前客流正常，游览体验舒适，");
                if ("rising".equals(trend)) {
                    suggestion.append("建议尽快入园，预计稍后客流会增加。");
                } else {
                    suggestion.append("非常适合现在入园游览。");
                }
                break;
                
            case "idle":
                statusDTO.setRecommended(true);
                suggestion.append("景区当前人流稀少，是深度游览的最佳时机，可以充分欣赏景色并拍照留念。");
                break;
                
            default:
                statusDTO.setRecommended(true);
                suggestion.append("建议提前查看景区公告，合理安排行程。");
        }
        
        statusDTO.setSuggestion(suggestion.toString());
    }
    
    /**
     * 获取状态描述
     */
    private String getStatusDescription(String status) {
        switch (status) {
            case "limited":
                return "限流中";
            case "crowded":
                return "拥挤";
            case "busy":
                return "繁忙";
            case "normal":
                return "正常开放";
            case "idle":
                return "人少舒适";
            default:
                return "正常";
        }
    }
    
    /**
     * 创建默认状态（当获取失败时）
     */
    private ScenicStatusDTO createDefaultStatus(Long scenicId) {
        ScenicStatusDTO dto = new ScenicStatusDTO();
        dto.setScenicId(scenicId);
        dto.setStatus("normal");
        dto.setStatusDescription("正常开放");
        dto.setCurrentFlow(500);
        dto.setMaxCapacity(1000);
        dto.setFlowRate(0.5);
        dto.setPredictedFlow(500);
        dto.setPredictedFlowRate(0.5);
        dto.setTrend("stable");
        dto.setSuggestedWaitTime(5);
        dto.setRecommended(true);
        dto.setSuggestion("景区运营正常，欢迎游览。");
        dto.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return dto;
    }
}

