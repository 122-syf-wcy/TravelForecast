package com.travel.service.merchant.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.entity.order.TicketOrder;
import com.travel.entity.scenic.ScenicReview;
import com.travel.entity.scenic.ScenicSpot;
import com.travel.entity.scenic.ScenicStatistics;
import com.travel.mapper.order.TicketOrderMapper;
import com.travel.mapper.scenic.ScenicReviewMapper;
import com.travel.mapper.scenic.ScenicSpotMapper;
import com.travel.mapper.scenic.ScenicStatisticsMapper;
import com.travel.service.merchant.MerchantAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商家数据分析服务实现类
 */
@Slf4j
@Service
public class MerchantAnalysisServiceImpl implements MerchantAnalysisService {

    @Autowired
    private ScenicStatisticsMapper statisticsMapper;

    @Autowired
    private ScenicSpotMapper scenicSpotMapper;

    @Autowired
    private ScenicReviewMapper scenicReviewMapper;

    @Autowired
    private TicketOrderMapper ticketOrderMapper;

    @Override
    public Map<String, Object> getAnalysisOverview(Long scenicId, LocalDate startDate, LocalDate endDate) {
        log.info("获取数据概览: scenicId={}, 日期范围={} to {}", scenicId, startDate, endDate);

        // 当前期间数据
        List<ScenicStatistics> currentStats = queryStatistics(scenicId, startDate, endDate);
        
        // 对比期间数据（前一个同等长度的时间段）
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        LocalDate prevStartDate = startDate.minusDays(days + 1);
        LocalDate prevEndDate = startDate.minusDays(1);
        List<ScenicStatistics> prevStats = queryStatistics(scenicId, prevStartDate, prevEndDate);

        // 计算当前期间指标
        int totalVisitors = currentStats.stream().mapToInt(ScenicStatistics::getVisitorCount).sum();
        BigDecimal totalRevenue = currentStats.stream()
                .map(ScenicStatistics::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double avgStayTime = currentStats.stream()
                .mapToDouble(s -> s.getAvgStayTime() != null ? s.getAvgStayTime().doubleValue() : 0.0)
                .average()
                .orElse(0.0);
        
        // 计算回游率（模拟：收藏数增长占总游客的比例）
        int totalFavorites = currentStats.stream().mapToInt(ScenicStatistics::getFavoritesCount).sum();
        double returnRate = totalVisitors > 0 ? (totalFavorites * 100.0 / totalVisitors) : 0.0;

        // 计算对比期间指标
        int prevTotalVisitors = prevStats.stream().mapToInt(ScenicStatistics::getVisitorCount).sum();
        BigDecimal prevTotalRevenue = prevStats.stream()
                .map(ScenicStatistics::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double prevAvgStayTime = prevStats.stream()
                .mapToDouble(s -> s.getAvgStayTime() != null ? s.getAvgStayTime().doubleValue() : 0.0)
                .average()
                .orElse(0.0);
        int prevTotalFavorites = prevStats.stream().mapToInt(ScenicStatistics::getFavoritesCount).sum();
        double prevReturnRate = prevTotalVisitors > 0 ? (prevTotalFavorites * 100.0 / prevTotalVisitors) : 0.0;

        // ----------------- 获取实时的购票订单数据以叠加 -----------------
        // 当前期间在线售票与收入附加
        LambdaQueryWrapper<TicketOrder> currentOrderQ = new LambdaQueryWrapper<TicketOrder>()
                .between(TicketOrder::getCreatedAt, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay())
                .in(TicketOrder::getStatus, "pending", "paid", "used");
        if (scenicId != null) currentOrderQ.eq(TicketOrder::getScenicId, scenicId);
        List<TicketOrder> currentOrders = ticketOrderMapper.selectList(currentOrderQ);
        
        int currentOnlineTickets = currentOrders.stream().mapToInt(o -> o.getTicketCount() != null ? o.getTicketCount() : 1).sum();
        BigDecimal currentOrderRevenue = currentOrders.stream().map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 把实际订单量和金额加到统计项上
        totalVisitors += currentOnlineTickets;
        totalRevenue = totalRevenue.add(currentOrderRevenue);

        // 对比期间在线售票与收入附加
        LambdaQueryWrapper<TicketOrder> prevOrderQ = new LambdaQueryWrapper<TicketOrder>()
                .between(TicketOrder::getCreatedAt, prevStartDate.atStartOfDay(), prevEndDate.plusDays(1).atStartOfDay())
                .in(TicketOrder::getStatus, "pending", "paid", "used");
        if (scenicId != null) prevOrderQ.eq(TicketOrder::getScenicId, scenicId);
        List<TicketOrder> prevOrders = ticketOrderMapper.selectList(prevOrderQ);
        
        int prevOnlineTickets = prevOrders.stream().mapToInt(o -> o.getTicketCount() != null ? o.getTicketCount() : 1).sum();
        BigDecimal prevOrderRevenue = prevOrders.stream().map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        
        prevTotalVisitors += prevOnlineTickets;
        prevTotalRevenue = prevTotalRevenue.add(prevOrderRevenue);
        // -----------------------------------------------------------

        // 计算变化率
        double visitorChange = calculateChangeRate(totalVisitors, prevTotalVisitors);
        double revenueChange = calculateChangeRate(totalRevenue.doubleValue(), prevTotalRevenue.doubleValue());
        double stayTimeChange = calculateChangeRate(avgStayTime, prevAvgStayTime);
        double returnRateChange = calculateChangeRate(returnRate, prevReturnRate);
        double ticketChange = calculateChangeRate(currentOnlineTickets, prevOnlineTickets);

        Map<String, Object> result = new HashMap<>();
        result.put("totalVisitors", totalVisitors);
        result.put("visitorChange", round(visitorChange, 1));
        result.put("totalRevenue", totalRevenue.doubleValue());
        result.put("revenueChange", round(revenueChange, 1));
        result.put("onlineTickets", currentOnlineTickets);
        result.put("ticketChange", round(ticketChange, 1));
        result.put("avgStayTime", round(avgStayTime, 1));
        result.put("stayTimeChange", round(stayTimeChange, 1));
        result.put("returnRate", round(returnRate, 1));
        result.put("returnRateChange", round(returnRateChange, 1));

        return result;
    }

    @Override
    public Map<String, Object> getVisitorTrend(Long scenicId, LocalDate startDate, LocalDate endDate, String metric) {
        log.info("获取游客量趋势: scenicId={}, 日期范围={} to {}, metric={}", scenicId, startDate, endDate, metric);

        List<ScenicStatistics> stats = queryStatistics(scenicId, startDate, endDate);

        List<String> labels = new ArrayList<>();
        List<Integer> data = new ArrayList<>();

        if ("daily".equals(metric)) {
            // 日数据
            Map<LocalDate, Integer> dailyVisitors = stats.stream()
                    .collect(Collectors.groupingBy(
                            ScenicStatistics::getStatDate,
                            Collectors.summingInt(ScenicStatistics::getVisitorCount)
                    ));

            List<LocalDate> dateList = new ArrayList<>(dailyVisitors.keySet());
            Collections.sort(dateList);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d");
            for (LocalDate date : dateList) {
                labels.add(date.format(formatter));
                data.add(dailyVisitors.get(date));
            }
        } else if ("weekly".equals(metric)) {
            // 周数据
            Map<String, Integer> weeklyVisitors = new TreeMap<>();
            for (ScenicStatistics stat : stats) {
                String weekKey = stat.getStatDate().getYear() + "-W" + 
                                String.format("%02d", getWeekOfYear(stat.getStatDate()));
                weeklyVisitors.merge(weekKey, stat.getVisitorCount(), Integer::sum);
            }

            int weekNum = 1;
            for (Map.Entry<String, Integer> entry : weeklyVisitors.entrySet()) {
                labels.add("第" + weekNum++ + "周");
                data.add(entry.getValue());
            }
        } else if ("monthly".equals(metric)) {
            // 月数据
            Map<String, Integer> monthlyVisitors = new TreeMap<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
            for (ScenicStatistics stat : stats) {
                String monthKey = stat.getStatDate().format(formatter);
                monthlyVisitors.merge(monthKey, stat.getVisitorCount(), Integer::sum);
            }

            DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("M月");
            for (Map.Entry<String, Integer> entry : monthlyVisitors.entrySet()) {
                LocalDate date = LocalDate.parse(entry.getKey() + "-01");
                labels.add(date.format(labelFormatter));
                data.add(entry.getValue());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        return result;
    }

    @Override
    public Map<String, Object> getRevenueAnalysis(Long scenicId, LocalDate startDate, LocalDate endDate, String type) {
        log.info("获取收入分析: scenicId={}, 日期范围={} to {}, type={}", scenicId, startDate, endDate, type);

        List<ScenicStatistics> stats = queryStatistics(scenicId, startDate, endDate);

        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();

        // 按日期分组收入
        Map<LocalDate, BigDecimal> dailyRevenue = stats.stream()
                .collect(Collectors.groupingBy(
                        ScenicStatistics::getStatDate,
                        Collectors.reducing(BigDecimal.ZERO, ScenicStatistics::getRevenue, BigDecimal::add)
                ));

        List<LocalDate> dateList = new ArrayList<>(dailyRevenue.keySet());
        Collections.sort(dateList);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d");
        for (LocalDate date : dateList) {
            labels.add(date.format(formatter));
            BigDecimal revenue = dailyRevenue.get(date);
            
            // 按类型筛选：ticket类型使用门票订单真实收入，其他类型使用统计表总收入
            if ("ticket".equals(type)) {
                // 查询当天门票订单真实收入
                LambdaQueryWrapper<TicketOrder> oq = new LambdaQueryWrapper<TicketOrder>()
                        .between(TicketOrder::getCreatedAt,
                                date.atStartOfDay(), date.plusDays(1).atStartOfDay())
                        .in(TicketOrder::getStatus, "paid", "used");
                if (scenicId != null) oq.eq(TicketOrder::getScenicId, scenicId);
                List<TicketOrder> dayOrders = ticketOrderMapper.selectList(oq);
                revenue = dayOrders.stream()
                        .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            // "all" / "food" / "souvenir" 使用统计表总收入（无独立数据源时保持原值）
            
            data.add(revenue.setScale(2, RoundingMode.HALF_UP));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        return result;
    }

    @Override
    public Map<String, Object> getVisitorSource(Long scenicId, LocalDate startDate, LocalDate endDate) {
        log.info("获取游客来源分析: scenicId={}, 日期范围={} to {}", scenicId, startDate, endDate);

        // 从门票订单统计票种分布作为游客来源构成
        LambdaQueryWrapper<TicketOrder> orderWrapper = new LambdaQueryWrapper<TicketOrder>()
                .between(TicketOrder::getCreatedAt,
                        startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        if (scenicId != null) {
            orderWrapper.eq(TicketOrder::getScenicId, scenicId);
        }
        List<TicketOrder> orders = ticketOrderMapper.selectList(orderWrapper);

        // 按票种分组统计人数
        int adultCount = 0, childCount = 0, studentCount = 0, elderCount = 0;
        for (TicketOrder order : orders) {
            int count = order.getTicketCount() != null ? order.getTicketCount() : 1;
            String type = order.getTicketType() != null ? order.getTicketType() : "adult";
            switch (type) {
                case "child": childCount += count; break;
                case "student": studentCount += count; break;
                case "elder": elderCount += count; break;
                default: adultCount += count; break;
            }
        }

        List<Map<String, Object>> sourceData = new ArrayList<>();
        sourceData.add(createSourceItem("成人票", adultCount, "#36DBFF"));
        sourceData.add(createSourceItem("学生票", studentCount, "#9F87FF"));
        sourceData.add(createSourceItem("儿童票", childCount, "#FF7ECB"));
        sourceData.add(createSourceItem("老人票", elderCount, "#4FFBDF"));

        Map<String, Object> result = new HashMap<>();
        result.put("data", sourceData);
        return result;
    }

    @Override
    public Map<String, Object> getAgeDistribution(Long scenicId, LocalDate startDate, LocalDate endDate) {
        log.info("获取年龄分布: scenicId={}, 日期范围={} to {}", scenicId, startDate, endDate);

        // 从门票订单的票种推断年龄段分布
        LambdaQueryWrapper<TicketOrder> orderWrapper = new LambdaQueryWrapper<TicketOrder>()
                .between(TicketOrder::getCreatedAt,
                        startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        if (scenicId != null) {
            orderWrapper.eq(TicketOrder::getScenicId, scenicId);
        }
        List<TicketOrder> orders = ticketOrderMapper.selectList(orderWrapper);

        int childCount = 0, studentCount = 0, adultCount = 0, elderCount = 0;
        for (TicketOrder order : orders) {
            int count = order.getTicketCount() != null ? order.getTicketCount() : 1;
            String type = order.getTicketType() != null ? order.getTicketType() : "adult";
            switch (type) {
                case "child": childCount += count; break;
                case "student": studentCount += count; break;
                case "elder": elderCount += count; break;
                default: adultCount += count; break;
            }
        }

        List<String> labels = Arrays.asList("儿童(18岁以下)", "学生(18-25岁)", "成人(26-55岁)", "老人(56岁以上)");
        List<Integer> data = Arrays.asList(childCount, studentCount, adultCount, elderCount);

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        return result;
    }

    @Override
    public Map<String, Object> getSatisfactionAnalysis(Long scenicId, LocalDate startDate, LocalDate endDate) {
        log.info("获取满意度分析: scenicId={}, 日期范围={} to {}", scenicId, startDate, endDate);

        // 从评价表获取真实评分分布
        LambdaQueryWrapper<ScenicReview> reviewWrapper = new LambdaQueryWrapper<ScenicReview>()
                .between(ScenicReview::getCreatedAt,
                        startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay())
                .eq(ScenicReview::getStatus, "published");
        if (scenicId != null) {
            reviewWrapper.eq(ScenicReview::getScenicId, scenicId);
        }
        List<ScenicReview> reviews = scenicReviewMapper.selectList(reviewWrapper);

        // 按评分(1-5)分组统计
        int rating5 = 0, rating4 = 0, rating3 = 0, rating2 = 0, rating1 = 0;
        for (ScenicReview review : reviews) {
            if (review.getRating() == null) continue;
            switch (review.getRating()) {
                case 5: rating5++; break;
                case 4: rating4++; break;
                case 3: rating3++; break;
                case 2: rating2++; break;
                case 1: rating1++; break;
            }
        }

        int totalReviews = reviews.size();
        double avgRating = reviews.stream()
                .filter(r -> r.getRating() != null)
                .mapToInt(ScenicReview::getRating)
                .average().orElse(0.0);
        double satisfactionRate = totalReviews > 0 ? (avgRating / 5.0) * 100.0 : 0.0;

        List<String> labels = Arrays.asList("非常满意(5星)", "满意(4星)", "一般(3星)", "不满意(2星)", "非常不满意(1星)");
        List<Integer> data = Arrays.asList(rating5, rating4, rating3, rating2, rating1);

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        result.put("totalReviews", totalReviews);
        result.put("avgRating", round(avgRating, 1));
        result.put("satisfactionRate", round(satisfactionRate, 1));
        return result;
    }

    @Override
    public Map<String, Object> getSpotHotRanking(Long scenicId, LocalDate startDate, LocalDate endDate) {
        log.info("获取景点热度排行: scenicId={}, 日期范围={} to {}", scenicId, startDate, endDate);

        // 如果指定了景区ID，只查询该景区
        LambdaQueryWrapper<ScenicSpot> wrapper = new LambdaQueryWrapper<ScenicSpot>()
                .eq(ScenicSpot::getStatus, "ACTIVE");
        if (scenicId != null) {
            wrapper.eq(ScenicSpot::getId, scenicId);
        }
        List<ScenicSpot> scenicSpots = scenicSpotMapper.selectList(wrapper);

        List<Map<String, Object>> spotList = new ArrayList<>();
        
        for (ScenicSpot scenic : scenicSpots) {
            List<ScenicStatistics> stats = queryStatistics(scenic.getId(), startDate, endDate);
            
            if (stats.isEmpty()) {
                continue;
            }
            
            int totalVisitors = stats.stream().mapToInt(ScenicStatistics::getVisitorCount).sum();
            double avgStayTime = stats.stream()
                    .mapToDouble(s -> s.getAvgStayTime() != null ? s.getAvgStayTime().doubleValue() : 0.0)
                    .average()
                    .orElse(0.0);
            
            // 计算趋势（最近一周vs前一周）
            LocalDate oneWeekAgo = endDate.minusDays(7);
            int recentVisitors = stats.stream()
                    .filter(s -> s.getStatDate().isAfter(oneWeekAgo))
                    .mapToInt(ScenicStatistics::getVisitorCount)
                    .sum();
            int prevWeekVisitors = stats.stream()
                    .filter(s -> s.getStatDate().isBefore(oneWeekAgo) || s.getStatDate().isEqual(oneWeekAgo))
                    .mapToInt(ScenicStatistics::getVisitorCount)
                    .sum();
            double trend = calculateChangeRate(recentVisitors, prevWeekVisitors);
            
            // 计算满意度（基于收藏数）
            int totalFavorites = stats.stream().mapToInt(ScenicStatistics::getFavoritesCount).sum();
            double satisfaction = 4.0 + (totalFavorites * 1.0 / totalVisitors) * 0.5; // 4.0-4.5分
            satisfaction = Math.min(5.0, satisfaction); // 不超过5分
            
            // 根据订单时间计算高峰时段
            String peakHours = calculatePeakHours(scenic.getId(), startDate, endDate);
            
            Map<String, Object> spotInfo = new HashMap<>();
            spotInfo.put("name", scenic.getName());
            spotInfo.put("image", scenic.getImageUrl());
            spotInfo.put("visitors", totalVisitors);
            spotInfo.put("avgStayTime", round(avgStayTime, 1) + "小时");
            spotInfo.put("peakHours", peakHours);
            spotInfo.put("satisfaction", round(satisfaction, 1));
            spotInfo.put("trend", round(trend, 1));
            
            spotList.add(spotInfo);
        }
        
        // 按游客量排序
        spotList.sort((a, b) -> Integer.compare((Integer)b.get("visitors"), (Integer)a.get("visitors")));

        Map<String, Object> result = new HashMap<>();
        result.put("spots", spotList);
        return result;
    }

    // ==================== 辅助方法 ====================

    /**
     * 查询统计数据，无数据时返回空列表（不再生成模拟数据）
     */
    private List<ScenicStatistics> queryStatistics(Long scenicId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<ScenicStatistics> wrapper = new LambdaQueryWrapper<ScenicStatistics>()
                .between(ScenicStatistics::getStatDate, startDate, endDate)
                .orderByAsc(ScenicStatistics::getStatDate);
        
        if (scenicId != null) {
            wrapper.eq(ScenicStatistics::getScenicId, scenicId);
        }
        
        List<ScenicStatistics> stats = statisticsMapper.selectList(wrapper);
        
        if (stats.isEmpty()) {
            log.info("数据库无统计数据: scenicId={}, {} to {}", scenicId, startDate, endDate);
        }
        
        return stats;
    }

    /**
     * 计算变化率
     */
    private double calculateChangeRate(double current, double previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((current - previous) / previous) * 100.0;
    }

    /**
     * 四舍五入
     */
    private double round(double value, int scale) {
        return new BigDecimal(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 创建来源项
     */
    private Map<String, Object> createSourceItem(String name, double value, String color) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("value", (int)value);
        item.put("itemStyle", Map.of("color", color));
        return item;
    }

    /**
     * 获取年份中的周数
     */
    private int getWeekOfYear(LocalDate date) {
        return (date.getDayOfYear() - 1) / 7 + 1;
    }

    /**
     * 根据订单创建时间分布推算高峰时段
     */
    private String calculatePeakHours(Long scenicId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<TicketOrder> wrapper = new LambdaQueryWrapper<TicketOrder>()
                .between(TicketOrder::getCreatedAt,
                        startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        if (scenicId != null) {
            wrapper.eq(TicketOrder::getScenicId, scenicId);
        }
        List<TicketOrder> orders = ticketOrderMapper.selectList(wrapper);

        if (orders.isEmpty()) {
            return "10:00-16:00";
        }

        // 统计每小时订单数
        int[] hourCounts = new int[24];
        for (TicketOrder order : orders) {
            if (order.getCreatedAt() != null) {
                hourCounts[order.getCreatedAt().getHour()]++;
            }
        }

        // 找到最高峰的连续时段
        int maxSum = 0, peakStart = 10;
        for (int i = 0; i <= 20; i++) {
            int sum = 0;
            for (int j = i; j < i + 4 && j < 24; j++) {
                sum += hourCounts[j];
            }
            if (sum > maxSum) {
                maxSum = sum;
                peakStart = i;
            }
        }
        int peakEnd = Math.min(peakStart + 4, 23);
        return String.format("%02d:00-%02d:00", peakStart, peakEnd);
    }
}


