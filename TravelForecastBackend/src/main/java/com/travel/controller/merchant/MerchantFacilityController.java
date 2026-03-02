package com.travel.controller.merchant;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.scenic.Facility;
import com.travel.service.scenic.FacilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 商家端设施管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/merchant/facilities")
@RequiredArgsConstructor
public class MerchantFacilityController {
    
    private final FacilityService facilityService;
    
    /**
     * 获取商家设施列表
     */
    @GetMapping
    public Map<String, Object> getMerchantFacilities(
            @RequestAttribute Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        
        log.info("商家获取设施列表: userId={}, page={}, size={}, status={}, category={}", 
                userId, page, size, status, category);
        
        Page<Facility> result = facilityService.getMerchantFacilities(userId, page, size, status, category);
        
        Map<String, Object> response = new HashMap<>();
        response.put("facilities", result.getRecords());
        response.put("total", result.getTotal());
        response.put("page", result.getCurrent());
        response.put("size", result.getSize());
        
        return response;
    }
    
    /**
     * 获取设施详情
     */
    @GetMapping("/{id}")
    public Facility getFacilityDetail(@PathVariable Long id, @RequestAttribute Long userId) {
        log.info("获取设施详情: id={}, userId={}", id, userId);
        return facilityService.getFacilityById(id);
    }
    
    /**
     * 创建设施
     */
    @PostMapping
    public Facility createFacility(@RequestBody Facility facility, @RequestAttribute Long userId) {
        log.info("创建设施: userId={}, name={}", userId, facility.getName());
        return facilityService.createFacility(facility, userId);
    }
    
    /**
     * 更新设施
     */
    @PutMapping("/{id}")
    public Facility updateFacility(
            @PathVariable Long id, 
            @RequestBody Facility facility, 
            @RequestAttribute Long userId) {
        
        log.info("更新设施: id={}, userId={}", id, userId);
        return facilityService.updateFacility(id, facility, userId);
    }
    
    /**
     * 删除设施
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteFacility(@PathVariable Long id, @RequestAttribute Long userId) {
        log.info("删除设施: id={}, userId={}", id, userId);
        facilityService.deleteFacility(id, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "删除成功");
        
        return response;
    }
    
    /**
     * 获取设施统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getFacilityStats(@RequestAttribute Long userId) {
        log.info("获取设施统计: userId={}", userId);
        return facilityService.getFacilityStats(userId);
    }
}

