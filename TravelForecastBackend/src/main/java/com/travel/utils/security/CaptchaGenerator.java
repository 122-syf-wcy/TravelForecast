package com.travel.utils.security;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.lang.UUID;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 验证码工具类
 * 使用Hutool的验证码生成器
 * 
 * @author Travel Team
 * @date 2025-10-10
 */
@Component
public class CaptchaGenerator {

    /**
     * 验证码宽度
     */
    private static final int WIDTH = 120;

    /**
     * 验证码高度
     */
    private static final int HEIGHT = 40;

    /**
     * 验证码字符个数
     */
    private static final int CODE_COUNT = 4;

    /**
     * 干扰线数量
     */
    private static final int LINE_COUNT = 50;

    /**
     * 生成验证码
     * 
     * @return Map包含验证码图片（Base64）和验证码内容
     */
    public static Map<String, String> generateCaptcha() {
        // 创建线段干扰验证码
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(WIDTH, HEIGHT, CODE_COUNT, LINE_COUNT);
        
        // 获取验证码文本
        String code = captcha.getCode();
        
        // 获取验证码图片Base64
        String imageBase64 = captcha.getImageBase64Data();
        
        Map<String, String> result = new HashMap<>();
        result.put("image", imageBase64);
        result.put("code", code);
        
        return result;
    }

    /**
     * 生成指定长度的验证码
     * 
     * @param length 验证码长度
     * @return Map包含验证码图片（Base64）和验证码内容
     */
    public static Map<String, String> generateCaptcha(int length) {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(WIDTH, HEIGHT, length, LINE_COUNT);
        
        String code = captcha.getCode();
        String imageBase64 = captcha.getImageBase64Data();
        
        Map<String, String> result = new HashMap<>();
        result.put("image", imageBase64);
        result.put("code", code);
        
        return result;
    }
}

