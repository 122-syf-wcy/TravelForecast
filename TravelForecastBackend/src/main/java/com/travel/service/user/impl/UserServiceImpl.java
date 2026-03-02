package com.travel.service.user.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.entity.user.User;
import com.travel.entity.user.UserPreference;
import com.travel.entity.user.UserPrivacy;
import com.travel.entity.user.UserSession;
import com.travel.exception.BusinessException;
import com.travel.mapper.user.UserMapper;
import com.travel.mapper.user.UserPreferenceMapper;
import com.travel.mapper.user.UserPrivacyMapper;
import com.travel.mapper.user.UserSessionMapper;
import com.travel.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserMapper userMapper;
    private final UserSessionMapper userSessionMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final UserPrivacyMapper userPrivacyMapper;
    
    @Override
    public User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 移除密码字段
        user.setPassword(null);
        return user;
    }
    
    @Override
    @Transactional
    public User updateUser(Long userId, Map<String, Object> updates) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 只允许更新特定字段
        if (updates.containsKey("nickname")) {
            user.setNickname((String) updates.get("nickname"));
        }
        if (updates.containsKey("avatar")) {
            user.setAvatar((String) updates.get("avatar"));
        }
        if (updates.containsKey("email")) {
            user.setEmail((String) updates.get("email"));
        }
        if (updates.containsKey("phone")) {
            user.setPhone((String) updates.get("phone"));
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 移除密码字段
        user.setPassword(null);
        return user;
    }
    
    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 验证旧密码（使用Hutool的BCrypt）
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        
        // 设置新密码（使用Hutool的BCrypt）
        user.setPassword(BCrypt.hashpw(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        
        log.info("用户修改密码成功: userId={}", userId);
    }
    
    @Override
    public List<UserSession> getUserSessions(Long userId) {
        return userSessionMapper.selectList(
            new LambdaQueryWrapper<UserSession>()
                .eq(UserSession::getUserId, userId)
                .orderByDesc(UserSession::getLoginAt)
        );
    }
    
    @Override
    @Transactional
    public void revokeSession(Long userId, Long sessionId) {
        UserSession session = userSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权撤销此会话");
        }
        
        userSessionMapper.deleteById(sessionId);
        log.info("用户撤销会话: userId={}, sessionId={}", userId, sessionId);
    }
    
    @Override
    @Transactional
    public void updatePreferences(Long userId, Map<String, Object> preferences) {
        UserPreference pref = userPreferenceMapper.selectOne(
            new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId)
        );
        
        String json = JSON.toJSONString(preferences);
        
        if (pref == null) {
            pref = new UserPreference();
            pref.setUserId(userId);
            pref.setPreferencesJson(json);
            pref.setCreatedAt(LocalDateTime.now());
            pref.setUpdatedAt(LocalDateTime.now());
            userPreferenceMapper.insert(pref);
        } else {
            pref.setPreferencesJson(json);
            pref.setUpdatedAt(LocalDateTime.now());
            userPreferenceMapper.updateById(pref);
        }
        
        log.info("用户更新偏好设置: userId={}", userId);
    }
    
    @Override
    public Map<String, Object> getPreferences(Long userId) {
        UserPreference pref = userPreferenceMapper.selectOne(
            new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId)
        );
        
        if (pref == null || pref.getPreferencesJson() == null) {
            return new HashMap<>();
        }
        
        return JSON.parseObject(pref.getPreferencesJson(), Map.class);
    }
    
    @Override
    @Transactional
    public void updatePrivacy(Long userId, Map<String, Object> privacy) {
        UserPrivacy priv = userPrivacyMapper.selectOne(
            new LambdaQueryWrapper<UserPrivacy>()
                .eq(UserPrivacy::getUserId, userId)
        );
        
        String json = JSON.toJSONString(privacy);
        
        if (priv == null) {
            priv = new UserPrivacy();
            priv.setUserId(userId);
            priv.setPrivacyJson(json);
            priv.setCreatedAt(LocalDateTime.now());
            priv.setUpdatedAt(LocalDateTime.now());
            userPrivacyMapper.insert(priv);
        } else {
            priv.setPrivacyJson(json);
            priv.setUpdatedAt(LocalDateTime.now());
            userPrivacyMapper.updateById(priv);
        }
        
        log.info("用户更新隐私设置: userId={}", userId);
    }
    
    @Override
    public Map<String, Object> getPrivacy(Long userId) {
        UserPrivacy priv = userPrivacyMapper.selectOne(
            new LambdaQueryWrapper<UserPrivacy>()
                .eq(UserPrivacy::getUserId, userId)
        );
        
        if (priv == null || priv.getPrivacyJson() == null) {
            return new HashMap<>();
        }
        
        return JSON.parseObject(priv.getPrivacyJson(), Map.class);
    }
}

