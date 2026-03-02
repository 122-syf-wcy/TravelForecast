package com.travel.dto;

import lombok.Data;

@Data
public class WechatUserInfoRequest {
    private Long userId;
    private String openid;
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private String country;
    private String province;
    private String city;
    private String language;
}
