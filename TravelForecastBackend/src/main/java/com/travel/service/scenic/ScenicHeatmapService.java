package com.travel.service.scenic;

import java.util.Map;

/**
 * 景区热力图服务接口
 */
public interface ScenicHeatmapService {
    
    /**
     * 获取景区客流热力图数据
     * @param scenicId 景区ID
     * @return 热力图数据
     */
    Map<String, Object> getScenicHeatmapData(Long scenicId);
}






