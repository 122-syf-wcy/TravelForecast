package com.travel.controller.scenic;

import com.travel.common.Result;
import com.travel.entity.scenic.ScenicSpot;
import com.travel.service.scenic.ScenicHeatmapService;
import com.travel.service.scenic.ScenicSpotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 景区热力图控制器
 */
@Slf4j
@RestController
@RequestMapping("/scenic/heatmap")
@Tag(name = "景区热力图", description = "景区客流热力图相关接口")
public class ScenicHeatmapController {
    
    @Autowired
    private ScenicHeatmapService scenicHeatmapService;
    
    @Autowired
    private ScenicSpotService scenicSpotService;
    
    /**
     * 获取景区客流热力图数据（支持ID或景区编码）
     */
    @GetMapping("/{idOrCode}")
    @Operation(summary = "获取景区客流热力图数据")
    public Result<Map<String, Object>> getScenicHeatmapData(@PathVariable String idOrCode) {
        log.info("请求获取景区 {} 的热力图数据", idOrCode);
        
        try {
            // 先获取景区信息（支持ID或编码）
            ScenicSpot spot = scenicSpotService.getByIdOrCode(idOrCode);
            if (spot == null) {
                return Result.error("景区不存在");
            }
            
            Map<String, Object> heatmapData = scenicHeatmapService.getScenicHeatmapData(spot.getId());
            return Result.success(heatmapData);
        } catch (Exception e) {
            log.error("获取景区热力图数据失败", e);
            return Result.error("获取热力图数据失败: " + e.getMessage());
        }
    }
}

