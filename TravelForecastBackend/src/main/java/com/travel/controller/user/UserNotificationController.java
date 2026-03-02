package com.travel.controller.user;

import com.travel.mapper.user.UserMapper;
import com.travel.service.system.SystemNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户端系统通知Controller
 */
@Slf4j
@RestController
@RequestMapping("/user/notifications")
public class UserNotificationController {
    
    @Autowired
    private SystemNotificationService notificationService;
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 获取用户的系统通知列表
     */
    @GetMapping
    public Map<String, Object> getNotifications(@RequestAttribute(required = false) Long userId) {
        log.info("获取用户系统通知: userId={}", userId);
        
        // 如果未登录，使用默认的user角色
        String userRole = "user";
        if (userId != null) {
            try {
                var user = userMapper.selectById(userId);
                if (user != null) {
                    userRole = user.getRole();
                }
            } catch (Exception e) {
                log.warn("获取用户角色失败: userId={}, 使用默认角色user", userId, e);
            }
        }
        
        List<Map<String, Object>> notifications = notificationService.getUserNotifications(
            userId != null ? userId : 0L, 
            userRole
        );
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", notifications);
        return result;
    }
    
    /**
     * 标记通知为已读
     */
    @PostMapping("/{id}/read")
    public Map<String, Object> markAsRead(@RequestAttribute(required = false) Long userId,
                                         @PathVariable Long id) {
        log.info("标记通知为已读: userId={}, notificationId={}", userId, id);
        
        Map<String, Object> result = new HashMap<>();
        
        // 检查是否登录
        if (userId == null) {
            result.put("code", 401);
            result.put("message", "请先登录");
            return result;
        }
        
        notificationService.markAsRead(id, userId);
        
        result.put("code", 200);
        result.put("message", "操作成功");
        return result;
    }
}

