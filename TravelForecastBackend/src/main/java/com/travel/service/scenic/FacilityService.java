package com.travel.service.scenic;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.scenic.Facility;

import java.util.Map;

/**
 * 设施服务接口
 */
public interface FacilityService {
    
    /**
     * 分页查询商家的设施列表
     */
    Page<Facility> getMerchantFacilities(Long merchantId, int page, int size, String status, String category);
    
    /**
     * 根据ID获取设施详情
     */
    Facility getFacilityById(Long id);
    
    /**
     * 创建设施
     */
    Facility createFacility(Facility facility, Long merchantId);
    
    /**
     * 更新设施
     */
    Facility updateFacility(Long id, Facility facility, Long merchantId);
    
    /**
     * 删除设施
     */
    void deleteFacility(Long id, Long merchantId);
    
    /**
     * 获取设施统计信息
     */
    Map<String, Object> getFacilityStats(Long merchantId);
    
    /**
     * 用户端查询景区设施列表
     */
    Page<Facility> getScenicFacilities(Long scenicId, String category, int page, int size);
}

