package com.travel.controller.merchant;

import com.travel.mapper.user.UserMapper;
import com.travel.service.system.SystemNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家端系统通知Controller
 */
@Slf4j
@RestController
@RequestMapping("/merchant/notifications")
public class MerchantNotificationController {
    
    @Autowired
    private SystemNotificationService notificationService;
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 获取商家的系统通知列表
     */
    @GetMapping
    public Map<String, Object> getNotifications(@RequestAttribute Long userId) {
        log.info("获取商家系统通知: userId={}", userId);
        
        // 获取用户角色
        String userRole = userMapper.selectById(userId).getRole();
        
        List<Map<String, Object>> notifications = notificationService.getUserNotifications(userId, userRole);
        
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
    public Map<String, Object> markAsRead(@RequestAttribute Long userId,
                                         @PathVariable Long id) {
        log.info("标记通知为已读: userId={}, notificationId={}", userId, id);
        
        notificationService.markAsRead(id, userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "操作成功");
        return result;
    }
}

