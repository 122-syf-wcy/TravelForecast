package com.travel.service.merchant;

import com.travel.entity.merchant.MerchantAvailableScenic;

import java.util.List;

/**
 * 商家可申请景区服务接口
 */
public interface MerchantAvailableScenicService {
    
    /**
     * 获取所有可申请景区列表
     */
    List<MerchantAvailableScenic> getAvailableList();
    
    /**
     * 管理员：获取所有景区配置（包括未开放的）
     */
    List<MerchantAvailableScenic> getAllList();
    
    /**
     * 添加可申请景区
     */
    MerchantAvailableScenic add(Long scenicId, Integer maxMerchants);
    
    /**
     * 更新景区配置
     */
    boolean update(Long id, Boolean isAvailable, Integer maxMerchants, Integer priority);
    
    /**
     * 删除
     */
    boolean delete(Long id);
    
    /**
     * 检查景区是否已满
     */
    boolean isFull(Long scenicId);
    
    /**
     * 增加当前商家数量
     */
    boolean incrementMerchantCount(Long scenicId);
    
    /**
     * 减少当前商家数量
     */
    boolean decrementMerchantCount(Long scenicId);
}

