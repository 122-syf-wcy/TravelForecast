package com.travel.dto.user;

import lombok.Data;

/**
 * 更新管理员账户信息请求
 */
@Data
public class UpdateAccountRequest {
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
}

