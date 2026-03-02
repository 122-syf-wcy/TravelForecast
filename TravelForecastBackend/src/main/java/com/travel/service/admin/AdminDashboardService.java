package com.travel.service.admin;

import java.util.Map;

/**
 * 管理员端总览服务接口
 */
public interface AdminDashboardService {
    
    /**
     * 获取总览统计数据
     */
    Map<String, Object> getOverviewStatistics();
    
    /**
     * 获取平台运营趋势数据
     * @param timeRange 时间范围: week/month/quarter
     */
    Map<String, Object> getTrendData(String timeRange);
    
    /**
     * 获取用户分布数据
     * @param distributionType 分布类型: region/age/gender
     */
    Map<String, Object> getDistributionData(String distributionType);
    
    /**
     * 获取热门景区排行
     */
    Map<String, Object> getHotScenicRankings();
    
    /**
     * 获取管理员通知列表
     */
    Map<String, Object> getAdminNotices();
    
    /**
     * 获取待办事项列表
     */
    Map<String, Object> getPendingTasks();
    
    /**
     * 获取平台活动日志
     */
    Map<String, Object> getActivityLogs();
    
    /**
     * 标记待办事项为已处理
     */
    void markTaskAsProcessed(Long taskId, String processedBy);
    
    /**
     * 创建待办事项
     */
    void createTask(String title, String description, String submitter);
    
    /**
     * 删除待办事项
     */
    void deleteTask(Long taskId);
    
    /**
     * 获取景区热度排行TOP10（数据分析页面使用）
     */
    Map<String, Object> getScenicHotRankings();
    
    /**
     * 获取用户活跃度分布（基于登录次数）
     */
    Map<String, Object> getUserActivityDistribution();
    
    /**
     * 获取核心指标数据（数据分析页面使用）
     * @param timeRange 时间范围: today/week/month/year
     */
    Map<String, Object> getAnalyticsMetrics(String timeRange);
}

