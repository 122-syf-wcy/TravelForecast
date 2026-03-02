package com.travel.controller.merchant;

import com.travel.common.Result;
import com.travel.entity.merchant.MerchantAvailableScenic;
import com.travel.entity.scenic.ScenicSpot;
import com.travel.service.merchant.MerchantAvailableScenicService;
import com.travel.service.scenic.ScenicSpotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商家端-景区相关Controller
 */
@Slf4j
@RestController
@RequestMapping("/merchant/scenics")
@RequiredArgsConstructor
public class MerchantScenicController {
    
    private final MerchantAvailableScenicService merchantAvailableScenicService;
    private final ScenicSpotService scenicSpotService;
    
    /**
     * 商家端：获取可申请的景区列表（含景区详情和申请状态）
     * GET /merchant/scenics/available
     */
    @GetMapping("/available")
    public Result<List<Map<String, Object>>> getAvailableList() {
        log.info("商家查询可申请景区列表");
        
        // 获取所有可申请的景区配置
        List<MerchantAvailableScenic> availableList = merchantAvailableScenicService.getAvailableList();
        
        if (availableList.isEmpty()) {
            log.warn("暂无可申请景区");
            return Result.success(new ArrayList<>(), "暂无可申请景区");
        }
        
        // 获取景区ID列表
        List<Long> scenicIds = availableList.stream()
                .map(MerchantAvailableScenic::getScenicId)
                .collect(Collectors.toList());
        
        // 批量获取景区详情
        List<ScenicSpot> spots = new ArrayList<>();
        for (Long scenicId : scenicIds) {
            ScenicSpot spot = scenicSpotService.getById(scenicId);
            if (spot != null) {
                spots.add(spot);
            }
        }
        
        // 建立景区ID到景区详情的映射
        Map<Long, ScenicSpot> spotMap = spots.stream()
                .collect(Collectors.toMap(ScenicSpot::getId, spot -> spot));
        
        // 组装返回数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (MerchantAvailableScenic available : availableList) {
            ScenicSpot spot = spotMap.get(available.getScenicId());
            if (spot == null) {
                continue;
            }
            
            // 检查是否已满
            boolean isFull = available.getCurrentMerchants() >= available.getMaxMerchants();
            
            Map<String, Object> item = new HashMap<>();
            item.put("id", spot.getId());
            item.put("name", spot.getName());
            item.put("city", spot.getCity());
            item.put("category", spot.getCategory());
            item.put("level", spot.getLevel());
            item.put("address", spot.getAddress());
            item.put("description", spot.getDescription());
            item.put("imageUrl", spot.getImageUrl());
            item.put("maxMerchants", available.getMaxMerchants());
            item.put("currentMerchants", available.getCurrentMerchants());
            item.put("isFull", isFull);
            item.put("isAvailable", !isFull);
            
            result.add(item);
        }
        
        log.info("商家可申请景区数量: {}", result.size());
        return Result.success(result);
    }
}

