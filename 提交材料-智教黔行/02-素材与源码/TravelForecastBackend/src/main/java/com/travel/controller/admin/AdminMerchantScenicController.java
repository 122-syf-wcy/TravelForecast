package com.travel.controller.admin;

import com.travel.common.Result;
import com.travel.entity.merchant.MerchantAvailableScenic;
import com.travel.entity.scenic.ScenicSpot;
import com.travel.service.merchant.MerchantAvailableScenicService;
import com.travel.service.scenic.ScenicSpotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员端-商家可申请景区管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/admin/merchant-scenics")
@RequiredArgsConstructor
public class AdminMerchantScenicController {
    
    private final MerchantAvailableScenicService merchantAvailableScenicService;
    private final ScenicSpotService scenicSpotService;
    
    /**
     * 获取所有可申请景区配置（含景区详情）
     * GET /admin/merchant-scenics
     */
    @GetMapping
    public Result<List<Map<String, Object>>> getList() {
        log.info("管理员查询可申请景区列表");
        
        List<MerchantAvailableScenic> availableList = merchantAvailableScenicService.getAllList();
        
        // 获取所有景区详情
        List<ScenicSpot> allSpots = scenicSpotService.getAllActive();
        Map<Long, ScenicSpot> spotMap = allSpots.stream()
                .collect(Collectors.toMap(ScenicSpot::getId, spot -> spot));
        
        // 组装数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (MerchantAvailableScenic available : availableList) {
            ScenicSpot spot = spotMap.get(available.getScenicId());
            if (spot == null) {
                continue;
            }
            
            Map<String, Object> item = new HashMap<>();
            item.put("id", available.getId());
            item.put("scenicId", available.getScenicId());
            item.put("scenicName", spot.getName());
            item.put("scenicCity", spot.getCity());
            item.put("scenicLevel", spot.getLevel());
            item.put("isAvailable", available.getIsAvailable());
            item.put("maxMerchants", available.getMaxMerchants());
            item.put("currentMerchants", available.getCurrentMerchants());
            item.put("priority", available.getPriority());
            item.put("createdAt", available.getCreatedAt());
            item.put("updatedAt", available.getUpdatedAt());
            
            result.add(item);
        }
        
        return Result.success(result);
    }
    
    /**
     * 添加可申请景区
     * POST /admin/merchant-scenics
     * Body: { "scenicId": 1, "maxMerchants": 1 }
     */
    @PostMapping
    public Result<MerchantAvailableScenic> add(@RequestBody Map<String, Object> params) {
        Long scenicId = Long.valueOf(params.get("scenicId").toString());
        Integer maxMerchants = params.containsKey("maxMerchants") 
                ? Integer.valueOf(params.get("maxMerchants").toString()) 
                : 1;
        
        log.info("管理员添加可申请景区: scenicId={}, maxMerchants={}", scenicId, maxMerchants);
        
        // 检查景区是否存在
        ScenicSpot spot = scenicSpotService.getByIdOrCode(scenicId.toString());
        if (spot == null) {
            return Result.error("景区不存在");
        }
        
        MerchantAvailableScenic result = merchantAvailableScenicService.add(scenicId, maxMerchants);
        return Result.success(result, "添加成功");
    }
    
    /**
     * 更新可申请景区配置
     * PUT /admin/merchant-scenics/{id}
     * Body: { "isAvailable": true, "maxMerchants": 2, "priority": 10 }
     */
    @PutMapping("/{id}")
    public Result<String> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        
        Boolean isAvailable = params.containsKey("isAvailable") 
                ? (Boolean) params.get("isAvailable") 
                : null;
        Integer maxMerchants = params.containsKey("maxMerchants") 
                ? Integer.valueOf(params.get("maxMerchants").toString()) 
                : null;
        Integer priority = params.containsKey("priority") 
                ? Integer.valueOf(params.get("priority").toString()) 
                : null;
        
        log.info("管理员更新可申请景区: id={}, isAvailable={}, maxMerchants={}, priority={}", 
                id, isAvailable, maxMerchants, priority);
        
        boolean success = merchantAvailableScenicService.update(id, isAvailable, maxMerchants, priority);
        
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败");
        }
    }
    
    /**
     * 删除可申请景区
     * DELETE /admin/merchant-scenics/{id}
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("管理员删除可申请景区: id={}", id);
        
        boolean success = merchantAvailableScenicService.delete(id);
        
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }
    
    /**
     * 获取未添加的景区列表（用于添加时选择）
     * GET /admin/merchant-scenics/available-to-add
     */
    @GetMapping("/available-to-add")
    public Result<List<ScenicSpot>> getAvailableToAdd() {
        log.info("管理员查询可添加的景区列表");
        
        // 获取所有景区
        List<ScenicSpot> allSpots = scenicSpotService.getAllActive();
        
        // 获取已添加的景区ID
        List<MerchantAvailableScenic> existingList = merchantAvailableScenicService.getAllList();
        List<Long> existingScenicIds = existingList.stream()
                .map(MerchantAvailableScenic::getScenicId)
                .collect(Collectors.toList());
        
        // 过滤出未添加的景区
        List<ScenicSpot> result = allSpots.stream()
                .filter(spot -> !existingScenicIds.contains(spot.getId()))
                .collect(Collectors.toList());
        
        return Result.success(result);
    }
}

