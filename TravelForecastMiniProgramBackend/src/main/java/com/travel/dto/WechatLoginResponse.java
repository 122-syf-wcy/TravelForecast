package com.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WechatLoginResponse {
    private String token;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String openid;
    private String unionid;
    private String role;
    private Long loginTime;
    private Long expiresIn;
}
