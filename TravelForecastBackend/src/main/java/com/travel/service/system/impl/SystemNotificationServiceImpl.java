package com.travel.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travel.entity.system.NotificationRead;
import com.travel.entity.system.SystemNotification;
import com.travel.mapper.system.NotificationReadMapper;
import com.travel.mapper.system.SystemNotificationMapper;
import com.travel.service.system.SystemNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统通知服务实现
 */
@Slf4j
@Service
public class SystemNotificationServiceImpl extends ServiceImpl<SystemNotificationMapper, SystemNotification> 
        implements SystemNotificationService {
    
    @Autowired
    private NotificationReadMapper notificationReadMapper;
    
    @Override
    public List<Map<String, Object>> getUserNotifications(Long userId, String userRole) {
        // 查询用户可见的通知
        LambdaQueryWrapper<SystemNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemNotification::getStatus, "active")
               .and(w -> w.eq(SystemNotification::getTargetRole, "all")
                          .or()
                          .eq(SystemNotification::getTargetRole, userRole))
               .and(w -> w.isNull(SystemNotification::getExpiresAt)
                          .or()
                          .gt(SystemNotification::getExpiresAt, LocalDateTime.now()))
               .orderByDesc(SystemNotification::getPriority)
               .orderByDesc(SystemNotification::getCreatedAt);
        
        List<SystemNotification> notifications = list(wrapper);
        
        // 查询用户已读的通知ID列表
        LambdaQueryWrapper<NotificationRead> readWrapper = new LambdaQueryWrapper<>();
        readWrapper.eq(NotificationRead::getUserId, userId);
        List<NotificationRead> readRecords = notificationReadMapper.selectList(readWrapper);
        Set<Long> readNotificationIds = readRecords.stream()
                .map(NotificationRead::getNotificationId)
                .collect(Collectors.toSet());
        
        // 转换为VO并标记是否已读
        return notifications.stream().map(notification -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", notification.getId());
            map.put("title", notification.getTitle());
            map.put("content", notification.getContent());
            map.put("type", notification.getType());
            map.put("priority", notification.getPriority());
            map.put("createdAt", notification.getCreatedAt());
            map.put("isRead", readNotificationIds.contains(notification.getId()));
            
            // 计算时间差
            LocalDateTime createdAt = notification.getCreatedAt();
            LocalDateTime now = LocalDateTime.now();
            long hours = java.time.Duration.between(createdAt, now).toHours();
            long days = hours / 24;
            
            String timeAgo;
            if (days > 0) {
                timeAgo = days + "天前";
            } else if (hours > 0) {
                timeAgo = hours + "小时前";
            } else {
                long minutes = java.time.Duration.between(createdAt, now).toMinutes();
                timeAgo = (minutes > 0 ? minutes : 1) + "分钟前";
            }
            map.put("timeAgo", timeAgo);
            
            return map;
        }).collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        // 检查是否已经读过
        LambdaQueryWrapper<NotificationRead> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationRead::getNotificationId, notificationId)
               .eq(NotificationRead::getUserId, userId);
        
        NotificationRead existingRead = notificationReadMapper.selectOne(wrapper);
        if (existingRead == null) {
            NotificationRead readRecord = new NotificationRead();
            readRecord.setNotificationId(notificationId);
            readRecord.setUserId(userId);
            readRecord.setReadAt(LocalDateTime.now()); // 设置阅读时间
            notificationReadMapper.insert(readRecord);
            log.info("标记通知为已读: notificationId={}, userId={}", notificationId, userId);
        }
    }
    
    @Override
    @Transactional
    public SystemNotification createNotification(SystemNotification notification) {
        if (notification.getStatus() == null) {
            notification.setStatus("active");
        }
        if (notification.getPriority() == null) {
            notification.setPriority(0);
        }
        if (notification.getType() == null) {
            notification.setType("info");
        }
        if (notification.getTargetRole() == null) {
            notification.setTargetRole("all");
        }
        
        save(notification);
        log.info("创建系统通知成功: title={}, targetRole={}", 
                notification.getTitle(), notification.getTargetRole());
        return notification;
    }
    
    @Override
    @Transactional
    public SystemNotification updateNotification(SystemNotification notification) {
        updateById(notification);
        log.info("更新系统通知成功: id={}", notification.getId());
        return notification;
    }
    
    @Override
    @Transactional
    public void deleteNotification(Long notificationId) {
        removeById(notificationId);
        
        // 同时删除相关的阅读记录
        LambdaQueryWrapper<NotificationRead> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationRead::getNotificationId, notificationId);
        notificationReadMapper.delete(wrapper);
        
        log.info("删除系统通知成功: id={}", notificationId);
    }
    
    @Override
    public List<SystemNotification> getAllNotifications() {
        LambdaQueryWrapper<SystemNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SystemNotification::getCreatedAt);
        return list(wrapper);
    }
}

