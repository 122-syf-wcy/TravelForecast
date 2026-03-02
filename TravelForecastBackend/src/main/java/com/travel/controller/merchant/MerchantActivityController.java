package com.travel.controller.merchant;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.scenic.Activity;
import com.travel.service.scenic.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 商家端活动管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/merchant/activities")
@RequiredArgsConstructor
public class MerchantActivityController {
    
    private final ActivityService activityService;
    
    /**
     * 获取商家活动列表
     */
    @GetMapping
    public Map<String, Object> getMerchantActivities(
            @RequestAttribute Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        
        log.info("商家获取活动列表: userId={}, page={}, size={}, status={}, category={}", 
                userId, page, size, status, category);
        
        Page<Activity> result = activityService.getMerchantActivities(userId, page, size, status, category);
        
        Map<String, Object> response = new HashMap<>();
        response.put("activities", result.getRecords());
        response.put("total", result.getTotal());
        response.put("page", result.getCurrent());
        response.put("size", result.getSize());
        
        return response;
    }
    
    /**
     * 获取活动详情
     */
    @GetMapping("/{id}")
    public Activity getActivityDetail(@PathVariable Long id, @RequestAttribute Long userId) {
        log.info("获取活动详情: id={}, userId={}", id, userId);
        return activityService.getActivityById(id);
    }
    
    /**
     * 创建活动
     */
    @PostMapping
    public Activity createActivity(@RequestBody Activity activity, @RequestAttribute Long userId) {
        log.info("创建活动: userId={}, name={}", userId, activity.getName());
        return activityService.createActivity(activity, userId);
    }
    
    /**
     * 更新活动
     */
    @PutMapping("/{id}")
    public Activity updateActivity(
            @PathVariable Long id, 
            @RequestBody Activity activity, 
            @RequestAttribute Long userId) {
        
        log.info("更新活动: id={}, userId={}", id, userId);
        return activityService.updateActivity(id, activity, userId);
    }
    
    /**
     * 删除活动
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteActivity(@PathVariable Long id, @RequestAttribute Long userId) {
        log.info("删除活动: id={}, userId={}", id, userId);
        activityService.deleteActivity(id, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "删除成功");
        
        return response;
    }
    
    /**
     * 推广活动到新闻资讯
     */
    @PostMapping("/{id}/promote")
    public Map<String, Object> promoteActivity(@PathVariable Long id, @RequestAttribute Long userId) {
        log.info("推广活动: id={}, userId={}", id, userId);
        activityService.promoteActivity(id, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "推广成功");
        
        return response;
    }
    
    /**
     * 取消推广
     */
    @PostMapping("/{id}/unpromote")
    public Map<String, Object> unpromoteActivity(@PathVariable Long id, @RequestAttribute Long userId) {
        log.info("取消推广活动: id={}, userId={}", id, userId);
        activityService.unpromoteActivity(id, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "取消推广成功");
        
        return response;
    }
    
    /**
     * 获取活动统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getActivityStats(@RequestAttribute Long userId) {
        log.info("获取活动统计: userId={}", userId);
        return activityService.getActivityStats(userId);
    }
}

