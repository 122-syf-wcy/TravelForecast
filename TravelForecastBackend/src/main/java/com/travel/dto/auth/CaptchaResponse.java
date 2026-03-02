package com.travel.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码响应DTO
 * 
 * @author Travel Team
 * @date 2025-10-10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponse {

    /**
     * 验证码ID（用于后续验证）
     */
    private String captchaId;

    /**
     * 验证码图片（Base64编码）
     */
    private String imageBase64;
}

