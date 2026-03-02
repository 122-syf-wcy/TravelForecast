package com.travel.service.user;

import com.travel.entity.user.User;
import com.travel.entity.user.UserSession;
import java.util.List;
import java.util.Map;

/**
 * 用户Service接口
 */
public interface UserService {
    
    /**
     * 获取用户信息（不包含密码）
     */
    User getUserById(Long userId);
    
    /**
     * 更新用户信息
     */
    User updateUser(Long userId, Map<String, Object> updates);
    
    /**
     * 修改密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
    
    /**
     * 获取用户会话列表
     */
    List<UserSession> getUserSessions(Long userId);
    
    /**
     * 撤销指定会话
     */
    void revokeSession(Long userId, Long sessionId);
    
    /**
     * 更新用户偏好
     */
    void updatePreferences(Long userId, Map<String, Object> preferences);
    
    /**
     * 获取用户偏好
     */
    Map<String, Object> getPreferences(Long userId);
    
    /**
     * 更新隐私设置
     */
    void updatePrivacy(Long userId, Map<String, Object> privacy);
    
    /**
     * 获取隐私设置
     */
    Map<String, Object> getPrivacy(Long userId);
}

