package com.travel.service.scenic;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.scenic.Activity;

import java.util.Map;

/**
 * 活动服务接口
 */
public interface ActivityService {
    
    /**
     * 分页查询商家的活动列表
     */
    Page<Activity> getMerchantActivities(Long merchantId, int page, int size, String status, String category);
    
    /**
     * 根据ID获取活动详情
     */
    Activity getActivityById(Long id);
    
    /**
     * 创建活动
     */
    Activity createActivity(Activity activity, Long merchantId);
    
    /**
     * 更新活动
     */
    Activity updateActivity(Long id, Activity activity, Long merchantId);
    
    /**
     * 删除活动
     */
    void deleteActivity(Long id, Long merchantId);
    
    /**
     * 推广活动到新闻资讯
     */
    void promoteActivity(Long id, Long merchantId);
    
    /**
     * 取消推广
     */
    void unpromoteActivity(Long id, Long merchantId);
    
    /**
     * 获取活动统计信息
     */
    Map<String, Object> getActivityStats(Long merchantId);
    
    /**
     * 用户端查询正在进行的活动
     */
    Page<Activity> getOngoingActivities(Long scenicId, int page, int size);
}

