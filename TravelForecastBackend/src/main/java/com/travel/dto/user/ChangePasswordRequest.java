package com.travel.dto.user;

import lombok.Data;

/**
 * 管理员修改密码请求
 */
@Data
public class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
}

