package com.travel.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travel.entity.system.SystemNotification;

import java.util.List;
import java.util.Map;

/**
 * 系统通知服务接口
 */
public interface SystemNotificationService extends IService<SystemNotification> {
    
    /**
     * 获取用户可见的通知列表（根据角色和阅读状态）
     */
    List<Map<String, Object>> getUserNotifications(Long userId, String userRole);
    
    /**
     * 标记通知为已读
     */
    void markAsRead(Long notificationId, Long userId);
    
    /**
     * 创建系统通知（管理员）
     */
    SystemNotification createNotification(SystemNotification notification);
    
    /**
     * 更新系统通知（管理员）
     */
    SystemNotification updateNotification(SystemNotification notification);
    
    /**
     * 删除系统通知（管理员）
     */
    void deleteNotification(Long notificationId);
    
    /**
     * 获取所有通知列表（管理员）
     */
    List<SystemNotification> getAllNotifications();
}

