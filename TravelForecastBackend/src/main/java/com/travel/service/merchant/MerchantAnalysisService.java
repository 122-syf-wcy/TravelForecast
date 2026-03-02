package com.travel.service.merchant;

import java.time.LocalDate;
import java.util.Map;

/**
 * 商家数据分析服务接口
 */
public interface MerchantAnalysisService {

    /**
     * 获取数据概览
     * @param scenicId 景区ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 包含总游客量、总收入、平均停留时间、回游率的Map
     */
    Map<String, Object> getAnalysisOverview(Long scenicId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取游客量趋势数据
     * @param scenicId 景区ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param metric 指标类型 (daily, weekly, monthly)
     * @return 包含日期和游客量数据的Map
     */
    Map<String, Object> getVisitorTrend(Long scenicId, LocalDate startDate, LocalDate endDate, String metric);

    /**
     * 获取收入分析数据
     * @param scenicId 景区ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param type 类型 (all, ticket, food, souvenir)
     * @return 包含收入数据的Map
     */
    Map<String, Object> getRevenueAnalysis(Long scenicId, LocalDate startDate, LocalDate endDate, String type);

    /**
     * 获取游客来源分析
     * @param scenicId 景区ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 包含游客来源分布的Map
     */
    Map<String, Object> getVisitorSource(Long scenicId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取年龄分布数据
     * @param scenicId 景区ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 包含年龄分布数据的Map
     */
    Map<String, Object> getAgeDistribution(Long scenicId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取满意度分析
     * @param scenicId 景区ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 包含满意度数据的Map
     */
    Map<String, Object> getSatisfactionAnalysis(Long scenicId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取景点热度排行
     * @param scenicId 景区ID（如果为null则返回所有景区）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 景点列表
     */
    Map<String, Object> getSpotHotRanking(Long scenicId, LocalDate startDate, LocalDate endDate);
}


