package com.travel.service.scenic;

import com.travel.dto.scenic.ScenicStatusDTO;

/**
 * 景区状态评估服务接口
 */
public interface ScenicStatusService {
    
    /**
     * 获取景区实时运营状态（基于历史数据和预测模型）
     * 
     * @param scenicId 景区ID
     * @return 景区状态信息
     */
    ScenicStatusDTO getScenicStatus(Long scenicId);
    
    /**
     * 评估景区状态
     * 
     * @param scenicId 景区ID
     * @param currentFlow 当前客流量
     * @param predictedFlow 预测客流量
     * @param maxCapacity 最大容量
     * @return 景区状态
     */
    String evaluateStatus(Long scenicId, Integer currentFlow, Integer predictedFlow, Integer maxCapacity);
}

