package com.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WechatUserInfoResponse {
    private Long userId;
    private String nickname;
    private String avatarUrl;
}
