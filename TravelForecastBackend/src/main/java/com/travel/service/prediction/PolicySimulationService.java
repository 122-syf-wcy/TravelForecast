package com.travel.service.prediction;

import java.util.Map;

/**
 * 政策沙盘模拟服务接口
 */
public interface PolicySimulationService {
    
    /**
     * 模拟政策效果
     * @param discount 联票折扣率（%）
     * @param subsidy 交通补贴（元/人）
     * @param capacity 容量上限（人/日）
     * @return 包含客流指数、收入指数、碳足迹指数及7日模拟数据
     */
    Map<String, Object> simulatePolicyEffect(double discount, double subsidy, int capacity);
}

