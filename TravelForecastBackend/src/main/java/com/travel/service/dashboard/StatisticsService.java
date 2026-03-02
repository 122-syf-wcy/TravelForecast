package com.travel.service.dashboard;

import java.util.Map;

/**
 * 统计报表服务接口
 */
public interface StatisticsService {
    
    /**
     * 获取旅游收入统计
     * @param range 时间范围: 1m/3m/6m/12m
     * @return 收入统计数据
     */
    Map<String, Object> getTourismIncome(String range);
    
    /**
     * 获取游客来源分析
     * @param range 时间范围: 1m/3m/6m/12m
     * @return 游客来源数据
     */
    Map<String, Object> getVisitorSource(String range);
    
    /**
     * 获取景区客流对比
     * @param year 年份
     * @return 景区对比数据
     */
    Map<String, Object> getScenicComparison(String year);
    
    /**
     * 获取详细统计数据（分页）
     * @param page 页码
     * @param pageSize 每页大小
     * @param searchKeyword 搜索关键词
     * @return 详细数据列表
     */
    Map<String, Object> getDetailedData(int page, int pageSize, String searchKeyword);
}

