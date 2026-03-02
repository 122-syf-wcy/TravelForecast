package com.travel.controller.scenic;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.scenic.Activity;
import com.travel.service.scenic.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户端活动查询Controller
 */
@Slf4j
@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
public class UserActivityController {
    
    private final ActivityService activityService;
    
    /**
     * 用户端查询正在进行的活动
     */
    @GetMapping("/ongoing")
    public Map<String, Object> getOngoingActivities(
            @RequestParam(required = false) Long scenicId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("用户查询正在进行的活动: scenicId={}, page={}, size={}", scenicId, page, size);
        
        Page<Activity> result = activityService.getOngoingActivities(scenicId, page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("activities", result.getRecords());
        response.put("total", result.getTotal());
        response.put("page", result.getCurrent());
        response.put("size", result.getSize());
        
        return response;
    }
    
    /**
     * 用户端获取活动详情
     */
    @GetMapping("/{id}")
    public Activity getActivityDetail(@PathVariable Long id) {
        log.info("用户查询活动详情: id={}", id);
        return activityService.getActivityById(id);
    }
}

