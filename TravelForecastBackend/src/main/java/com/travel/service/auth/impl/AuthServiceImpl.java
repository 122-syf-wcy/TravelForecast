package com.travel.service.auth.impl;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.dto.auth.CaptchaResponse;
import com.travel.dto.auth.LoginRequest;
import com.travel.dto.auth.LoginResponse;
import com.travel.dto.auth.RegisterRequest;
import com.travel.entity.auth.CaptchaRecord;
import com.travel.entity.user.User;
import com.travel.entity.user.UserSession;
import com.travel.exception.BusinessException;
import com.travel.mapper.auth.CaptchaRecordMapper;
import com.travel.mapper.user.UserMapper;
import com.travel.mapper.user.UserSessionMapper;
import com.travel.service.auth.AuthService;
import com.travel.service.system.SystemConfigService;
import com.travel.utils.security.CaptchaGenerator;
import com.travel.utils.security.JwtUtil;
import com.travel.utils.security.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 认证服务实现类
 * 
 * @author Travel Team
 * @date 2025-10-10
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private CaptchaRecordMapper captchaRecordMapper;
    
    @Autowired
    private UserSessionMapper userSessionMapper;
    
    @Autowired
    private SystemConfigService systemConfigService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    public CaptchaResponse generateCaptcha() {
        // 生成验证码
        Map<String, String> captchaData = CaptchaGenerator.generateCaptcha();
        String captchaKey = UUID.randomUUID().toString(true);
        
        // 保存验证码记录到数据库
        CaptchaRecord record = new CaptchaRecord();
        record.setCaptchaKey(captchaKey);
        record.setCaptchaCode(captchaData.get("code"));
        record.setExpireAt(LocalDateTime.now().plusMinutes(5)); // 5分钟有效期
        record.setUsed(false);
        captchaRecordMapper.insert(record);
        
        // 返回响应
        CaptchaResponse response = new CaptchaResponse();
        response.setCaptchaId(captchaKey);
        response.setImageBase64(captchaData.get("image"));
        
        log.info("生成验证码成功, captchaKey: {}", captchaKey);
        return response;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public User register(RegisterRequest request) {
        // 1. 验证验证码
        validateCaptcha(request.getCaptchaId(), request.getCaptchaCode());
        
        // 2. 检查用户名是否已存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, request.getUsername());
        User existingUser = userMapper.selectOne(queryWrapper);
        if (existingUser != null) {
            throw new BusinessException("用户名已存在");
        }
        
        // 3. 检查邮箱是否已存在（如果提供了邮箱）
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            LambdaQueryWrapper<User> emailQuery = new LambdaQueryWrapper<>();
            emailQuery.eq(User::getEmail, request.getEmail());
            User existingEmail = userMapper.selectOne(emailQuery);
            if (existingEmail != null) {
                throw new BusinessException("邮箱已被使用");
            }
        }
        
        // 4. 创建新用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(PasswordUtil.encode(request.getPassword())); // 加密密码
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setNickname(request.getNickname());
        user.setRole("user"); // 默认角色为普通用户
        user.setStatus("active"); // 默认状态为激活
        
        userMapper.insert(user);
        
        // 5. 使用后删除验证码
        deleteCaptcha(request.getCaptchaId());
        
        log.info("用户注册成功, username: {}, userId: {}", user.getUsername(), user.getUserId());
        
        // 返回时清除敏感信息
        user.setPassword(null);
        return user;
    }
    
    /**
     * 商家注册：角色 merchant，状态 pending
     */
    @Transactional(rollbackFor = Exception.class)
    public User registerMerchant(RegisterRequest request) {
        // 1. 验证验证码
        validateCaptcha(request.getCaptchaId(), request.getCaptchaCode());
        
        // 2. 检查用户名是否已存在
        LambdaQueryWrapper<User> usernameQuery = new LambdaQueryWrapper<>();
        usernameQuery.eq(User::getUsername, request.getUsername());
        if (userMapper.selectOne(usernameQuery) != null) {
            throw new BusinessException("用户名已存在");
        }
        
        // 3. 检查邮箱是否已存在（如果提供了邮箱）
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            LambdaQueryWrapper<User> emailQuery = new LambdaQueryWrapper<>();
            emailQuery.eq(User::getEmail, request.getEmail());
            if (userMapper.selectOne(emailQuery) != null) {
                throw new BusinessException("邮箱已被使用");
            }
        }
        
        // 4. 创建商家用户（待审核）
        User merchant = new User();
        merchant.setUsername(request.getUsername());
        merchant.setPassword(PasswordUtil.encode(request.getPassword()));
        merchant.setEmail(request.getEmail());
        merchant.setPhone(request.getPhone());
        merchant.setNickname(request.getNickname());
        merchant.setRole("merchant");
        merchant.setStatus("pending"); // 待审核
        
        userMapper.insert(merchant);
        
        // 5. 使用后删除验证码
        deleteCaptcha(request.getCaptchaId());
        
        log.info("商家注册成功(待审核), username: {}, userId: {}", merchant.getUsername(), merchant.getUserId());
        merchant.setPassword(null);
        return merchant;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request) {
        // 1. 验证验证码（根据系统配置决定是否验证）
        if (isCaptchaEnabled()) {
            validateCaptcha(request.getCaptchaId(), request.getCaptchaCode());
        }
        
        // 2. 查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(queryWrapper);
        
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        
        // 3. 验证密码
        if (!PasswordUtil.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        
        // 4. 检查用户状态
        if (!"active".equals(user.getStatus())) {
            if ("pending".equalsIgnoreCase(user.getStatus())) {
                throw new BusinessException("账号待审核，请联系管理员");
            }
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        
        // 5. 生成JWT令牌 & 刷新令牌
        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole());
        String refreshToken = UUID.randomUUID().toString(true);
        
        // 6. 保存会话记录
        UserSession session = new UserSession();
        session.setUserId(user.getUserId());
        session.setToken(token);
        session.setIpAddress(request.getIpAddress());
        session.setUserAgent(request.getUserAgent());
        session.setExpiresAt(LocalDateTime.now().plusDays(7)); // 7天有效期（与JWT一致）
        session.setRefreshToken(refreshToken);
        session.setRefreshExpireAt(LocalDateTime.now().plusDays(30));
        userSessionMapper.insert(session);
        
        // 7. 更新用户最后登录时间
        user.setLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 8. 使用后删除验证码
        if (isCaptchaEnabled() && request.getCaptchaId() != null) {
            deleteCaptcha(request.getCaptchaId());
        }
        
        log.info("用户登录成功, username: {}, userId: {}", user.getUsername(), user.getUserId());
        
        // 9. 构建响应
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setRefreshToken(refreshToken);
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setRole(user.getRole());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setAvatar(user.getAvatar());
        
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(String token) {
        if (token == null) return;
        userSessionMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserSession>()
                .eq(UserSession::getToken, token));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logoutAll(Long userId) {
        if (userId == null) return;
        userSessionMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserSession>()
                .eq(UserSession::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new BusinessException("refreshToken不能为空");
        }
        UserSession session = userSessionMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserSession>()
                .eq(UserSession::getRefreshToken, refreshToken));
        if (session == null || session.getRefreshExpireAt() == null || session.getRefreshExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("refreshToken无效或已过期");
        }
        User u = userMapper.selectById(session.getUserId());
        if (u == null || !"active".equals(u.getStatus())) {
            throw new BusinessException("账号不可用");
        }
        String newToken = jwtUtil.generateToken(u.getUserId(), u.getUsername(), u.getRole());
        session.setToken(newToken);
        session.setExpiresAt(LocalDateTime.now().plusDays(7));
        userSessionMapper.updateById(session);

        LoginResponse resp = new LoginResponse();
        resp.setToken(newToken);
        resp.setRefreshToken(refreshToken);
        resp.setUserId(u.getUserId());
        resp.setUsername(u.getUsername());
        resp.setNickname(u.getNickname());
        resp.setRole(u.getRole());
        resp.setEmail(u.getEmail());
        resp.setPhone(u.getPhone());
        resp.setAvatar(u.getAvatar());
        return resp;
    }

    @Autowired
    private com.travel.mapper.auth.PasswordResetTokenMapper passwordResetTokenMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendResetPasswordToken(String usernameOrEmail) {
        if (usernameOrEmail == null || usernameOrEmail.isEmpty()) {
            throw new BusinessException("用户名或邮箱不能为空");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, usernameOrEmail)
                .or()
                .eq(User::getEmail, usernameOrEmail));
        if (user == null) {
            return; // 避免用户枚举
        }
        String token = UUID.randomUUID().toString(true);
        com.travel.entity.auth.PasswordResetToken prt = new com.travel.entity.auth.PasswordResetToken();
        prt.setUserId(user.getUserId());
        prt.setToken(token);
        prt.setExpireAt(LocalDateTime.now().plusHours(1));
        prt.setUsed(false);
        passwordResetTokenMapper.insert(prt);
        // 这里可选发送邮件，沿用现有 mailSender 配置
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String token, String newPassword) {
        if (token == null || newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("参数无效");
        }
        com.travel.entity.auth.PasswordResetToken prt = passwordResetTokenMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.travel.entity.auth.PasswordResetToken>()
                        .eq(com.travel.entity.auth.PasswordResetToken::getToken, token));
        if (prt == null || Boolean.TRUE.equals(prt.getUsed()) || prt.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("重置链接无效或已过期");
        }
        User user = userMapper.selectById(prt.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(PasswordUtil.encode(newPassword));
        userMapper.updateById(user);
        prt.setUsed(true);
        passwordResetTokenMapper.updateById(prt);
        // 退出所有会话
        logoutAll(user.getUserId());
    }
    
    @Override
    public User checkLoginStatus(String token) {
        // 1. 验证Token
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException("登录已过期，请重新登录");
        }
        
        // 2. 从Token中获取用户ID
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        // 3. 查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 4. 检查用户状态
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }
        
        // 清除敏感信息
        user.setPassword(null);
        
        return user;
    }
    
    /**
     * 检查系统配置是否启用验证码
     */
    private boolean isCaptchaEnabled() {
        try {
            String val = systemConfigService.getConfig("enable_captcha");
            return !"false".equalsIgnoreCase(val);
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 验证验证码
     */
    private void validateCaptcha(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null) {
            throw new BusinessException("验证码不能为空");
        }
        
        // 查询验证码记录
        LambdaQueryWrapper<CaptchaRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CaptchaRecord::getCaptchaKey, captchaId);
        CaptchaRecord record = captchaRecordMapper.selectOne(queryWrapper);
        
        if (record == null) {
            throw new BusinessException("验证码无效");
        }
        
        // 检查是否过期
        if (record.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("验证码已过期");
        }
        
        // 检查是否已使用
        if (record.getUsed()) {
            throw new BusinessException("验证码已被使用");
        }
        
        // 验证码比对（不区分大小写）
        if (!captchaCode.equalsIgnoreCase(record.getCaptchaCode())) {
            throw new BusinessException("验证码错误");
        }
    }
    
    /**
     * 删除验证码（标记为已使用）
     */
    private void deleteCaptcha(String captchaId) {
        LambdaQueryWrapper<CaptchaRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CaptchaRecord::getCaptchaKey, captchaId);
        CaptchaRecord record = captchaRecordMapper.selectOne(queryWrapper);
        
        if (record != null) {
            record.setUsed(true);
            captchaRecordMapper.updateById(record);
        }
    }
}


