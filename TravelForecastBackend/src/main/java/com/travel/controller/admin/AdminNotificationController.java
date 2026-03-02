package com.travel.controller.admin;

import com.travel.entity.system.SystemNotification;
import com.travel.service.system.SystemNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员端系统通知Controller
 */
@Slf4j
@RestController
@RequestMapping("/admin/notifications")
public class AdminNotificationController {
    
    @Autowired
    private SystemNotificationService notificationService;
    
    /**
     * 获取所有系统通知
     */
    @GetMapping
    public Map<String, Object> getAllNotifications(@RequestAttribute Long userId) {
        log.info("管理员获取所有系统通知: userId={}", userId);
        
        List<SystemNotification> notifications = notificationService.getAllNotifications();
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", notifications);
        return result;
    }
    
    /**
     * 创建系统通知
     */
    @PostMapping
    public Map<String, Object> createNotification(@RequestAttribute Long userId,
                                                  @RequestBody SystemNotification notification) {
        log.info("管理员创建系统通知: userId={}, title={}", userId, notification.getTitle());
        
        SystemNotification created = notificationService.createNotification(notification);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "创建成功");
        result.put("data", created);
        return result;
    }
    
    /**
     * 更新系统通知
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateNotification(@RequestAttribute Long userId,
                                                  @PathVariable Long id,
                                                  @RequestBody SystemNotification notification) {
        log.info("管理员更新系统通知: userId={}, notificationId={}", userId, id);
        
        notification.setId(id);
        SystemNotification updated = notificationService.updateNotification(notification);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "更新成功");
        result.put("data", updated);
        return result;
    }
    
    /**
     * 删除系统通知
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteNotification(@RequestAttribute Long userId,
                                                  @PathVariable Long id) {
        log.info("管理员删除系统通知: userId={}, notificationId={}", userId, id);
        
        notificationService.deleteNotification(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "删除成功");
        return result;
    }
}

