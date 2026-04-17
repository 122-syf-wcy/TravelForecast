package com.travel.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求DTO
 * 
 * @author Travel Team
 * @date 2025-10-10
 */
@Data
public class LoginRequest {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 验证码ID
     */
    private String captchaId;

    /**
     * 验证码
     */
    private String captchaCode;
    
    /**
     * 客户端IP地址（由Controller自动填充）
     */
    private String ipAddress;
    
    /**
     * 用户代理（由Controller自动填充）
     */
    private String userAgent;
}

