package com.travel.controller.scenic;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.scenic.Facility;
import com.travel.service.scenic.FacilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户端设施查询Controller
 */
@Slf4j
@RestController
@RequestMapping("/facilities")
@RequiredArgsConstructor
public class UserFacilityController {
    
    private final FacilityService facilityService;
    
    /**
     * 用户端查询景区设施列表
     */
    @GetMapping
    public Map<String, Object> getScenicFacilities(
            @RequestParam(required = false) Long scenicId,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("用户查询景区设施: scenicId={}, category={}, page={}, size={}", 
                scenicId, category, page, size);
        
        Page<Facility> result = facilityService.getScenicFacilities(scenicId, category, page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("facilities", result.getRecords());
        response.put("total", result.getTotal());
        response.put("page", result.getCurrent());
        response.put("size", result.getSize());
        
        return response;
    }
    
    /**
     * 用户端获取设施详情
     */
    @GetMapping("/{id}")
    public Facility getFacilityDetail(@PathVariable Long id) {
        log.info("用户查询设施详情: id={}", id);
        return facilityService.getFacilityById(id);
    }
}

