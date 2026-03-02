package com.travel.controller.scenic;

import com.travel.dto.scenic.ScenicStatusDTO;
import com.travel.service.scenic.ScenicStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 景区状态Controller
 */
@Slf4j
@RestController
@RequestMapping("/scenic/status")
@RequiredArgsConstructor
public class ScenicStatusController {
    
    private final ScenicStatusService scenicStatusService;
    
    /**
     * 获取景区实时运营状态
     */
    @GetMapping("/{scenicId}")
    public Map<String, Object> getScenicStatus(@PathVariable Long scenicId) {
        log.info("获取景区状态: scenicId={}", scenicId);
        
        try {
            ScenicStatusDTO status = scenicStatusService.getScenicStatus(scenicId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取成功");
            response.put("data", status);
            
            return response;
            
        } catch (Exception e) {
            log.error("获取景区状态失败: scenicId={}", scenicId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "获取景区状态失败");
            
            return response;
        }
    }
}

