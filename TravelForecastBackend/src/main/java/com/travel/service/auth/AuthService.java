package com.travel.service.auth;

import com.travel.dto.auth.CaptchaResponse;
import com.travel.dto.auth.LoginRequest;
import com.travel.dto.auth.LoginResponse;
import com.travel.dto.auth.RegisterRequest;
import com.travel.entity.user.User;

/**
 * 认证服务接口
 * 
 * @author Travel Team
 * @date 2025-10-10
 */
public interface AuthService {
    
    /**
     * 生成图形验证码
     * 
     * @return 验证码响应对象（包含Base64编码的图片和验证码ID）
     */
    CaptchaResponse generateCaptcha();
    
    /**
     * 用户注册
     * 
     * @param request 注册请求对象
     * @return 注册成功的用户信息
     */
    User register(RegisterRequest request);
    
    /**
     * 商家注册（角色 merchant，状态 pending）
     */
    User registerMerchant(RegisterRequest request);
    
    /**
     * 用户登录
     * 
     * @param request 登录请求对象
     * @return 登录响应对象（包含JWT令牌和用户信息）
     */
    LoginResponse login(LoginRequest request);
    
    /**
     * 检查用户登录状态
     * 
     * @param token JWT令牌
     * @return 当前登录的用户信息
     */
    User checkLoginStatus(String token);

    /**
     * 退出登录（当前设备）
     */
    void logout(String token);

    /**
     * 退出所有设备
     */
    void logoutAll(Long userId);

    /**
     * 刷新Token
     */
    LoginResponse refreshToken(String refreshToken);

    /**
     * 忘记密码：发送重置链接/验证码
     */
    void sendResetPasswordToken(String usernameOrEmail);

    /**
     * 重置密码
     */
    void resetPassword(String token, String newPassword);
}


