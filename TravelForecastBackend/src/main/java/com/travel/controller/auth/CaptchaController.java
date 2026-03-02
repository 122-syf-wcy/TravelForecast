package com.travel.controller.auth;

import com.travel.common.Result;
import com.travel.dto.auth.CaptchaResponse;
import com.travel.service.auth.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证码控制器
 * 
 * @author Travel Team
 * @date 2025-10-10
 */
@Slf4j
@RestController
@RequestMapping("/captcha")
public class CaptchaController {
    
    @Autowired
    private AuthService authService;
    
    /**
     * 生成图形验证码
     * 
     * GET /api/captcha
     * 
     * @return 验证码响应对象
     */
    @GetMapping
    public Result<CaptchaResponse> generateCaptcha() {
        CaptchaResponse response = authService.generateCaptcha();
        return Result.success(response);
    }
}


