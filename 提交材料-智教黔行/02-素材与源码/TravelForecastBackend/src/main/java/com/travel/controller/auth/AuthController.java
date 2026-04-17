package com.travel.controller.auth;

import com.travel.common.Result;
import com.travel.dto.auth.LoginRequest;
import com.travel.dto.auth.LoginResponse;
import com.travel.dto.auth.RegisterRequest;
import com.travel.entity.user.User;
import com.travel.entity.system.SystemLog;
import com.travel.mapper.system.SystemLogMapper;
import com.travel.service.auth.AuthService;
import com.travel.service.system.SystemConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 
 * @author Travel Team
 * @date 2025-10-10
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private SystemLogMapper systemLogMapper;
    
    @Autowired
    private SystemConfigService systemConfigService;
    
    /**
     * 用户注册
     * 
     * POST /api/auth/register
     * 
     * @param request 注册请求对象
     * @return 注册成功的用户信息
     */
    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody RegisterRequest request) {
        // 如果前端误把商家注册打到通用注册接口，根据 userType 兜底分流
        if ("MERCHANT".equalsIgnoreCase(request.getUserType())) {
            User user = authService.registerMerchant(request);
            return Result.success(user, "商家注册成功，待审核");
        }
        User user = authService.register(request);
        return Result.success(user, "注册成功");
    }

    /**
     * 商家注册
     *
     * POST /api/auth/register/merchant
     *
     * @param request 注册请求对象（同用户注册）
     * @return 注册成功的用户信息（角色为 merchant，状态为 pending）
     */
    @PostMapping("/register/merchant")
    public Result<User> registerMerchant(@Valid @RequestBody RegisterRequest request) {
        User user = authService.registerMerchant(request);
        return Result.success(user, "商家注册成功，待审核");
    }
    
    /**
     * 用户登录
     * 
     * POST /api/auth/login
     * 
     * @param request 登录请求对象
     * @param httpRequest HTTP请求对象（用于获取IP和UserAgent）
     * @return 登录响应对象（包含JWT令牌）
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        // 获取客户端IP地址
        String ipAddress = getClientIpAddress(httpRequest);
        request.setIpAddress(ipAddress);
        
        // 获取User-Agent
        String userAgent = httpRequest.getHeader("User-Agent");
        request.setUserAgent(userAgent);
        
        LoginResponse response = authService.login(request);
        
        // 记录登录日志（含IP）
        try {
            SystemLog sysLog = new SystemLog();
            sysLog.setLogTime(java.time.LocalDateTime.now());
            sysLog.setLogLevel("info");
            sysLog.setModule("用户管理");
            sysLog.setMessage((response.getRole() != null ? response.getRole() : "用户") + "登录成功");
            sysLog.setUsername(response.getUsername() != null ? response.getUsername() : request.getUsername());
            sysLog.setIpAddress(ipAddress);
            if (response.getUserId() != null) {
                sysLog.setUserId(response.getUserId());
            }
            systemLogMapper.insert(sysLog);
        } catch (Exception e) {
            log.warn("记录登录日志失败", e);
        }
        
        return Result.success(response, "登录成功");
    }
    
    /**
     * 检查登录状态
     * 
     * GET /api/auth/status
     * 
     * @param authorization Authorization请求头（格式：Bearer <token>）
     * @return 当前登录的用户信息
     */
    @GetMapping("/status")
    public Result<User> checkStatus(@RequestHeader("Authorization") String authorization) {
        // 从Authorization头中提取Token
        String token = extractToken(authorization);
        
        User user = authService.checkLoginStatus(token);
        return Result.success(user);
    }
    
    /**
     * 用户登出
     * 
     * POST /api/auth/logout
     * 
     * @return 登出成功消息
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
        String token = extractToken(authorization);
        authService.logout(token);
        return Result.success(null, "登出成功");
    }

    /**
     * 退出所有设备
     */
    @PostMapping("/logout-all")
    public Result<Void> logoutAll(@RequestHeader("Authorization") String authorization) {
        String token = extractToken(authorization);
        Long userId = authService.checkLoginStatus(token).getUserId();
        authService.logoutAll(userId);
        return Result.success(null, "已退出所有设备");
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/refresh-token")
    public Result<LoginResponse> refreshToken(@RequestBody com.travel.dto.auth.TokenRefreshRequest request) {
        return Result.success(authService.refreshToken(request.getRefreshToken()));
    }

    /**
     * 忘记密码
     */
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@RequestBody com.travel.dto.auth.ForgotPasswordRequest request) {
        authService.sendResetPasswordToken(request.getUsernameOrEmail());
        return Result.success(null, "重置链接已发送，如未收到请检查垃圾邮箱");
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody com.travel.dto.auth.ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return Result.success(null, "密码已重置，请使用新密码登录");
    }
    
    /**
     * 获取登录页公开配置（验证码开关等）
     * 无需token，白名单接口
     */
    @GetMapping("/login-config")
    public Result<java.util.Map<String, Object>> getLoginConfig() {
        java.util.Map<String, Object> config = new java.util.HashMap<>();
        String enableCaptcha = systemConfigService.getConfig("enable_captcha");
        config.put("enableCaptcha", "true".equals(enableCaptcha));
        return Result.success(config);
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 处理多个IP的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
    
    /**
     * 从Authorization头中提取Token
     */
    private String extractToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }
}


