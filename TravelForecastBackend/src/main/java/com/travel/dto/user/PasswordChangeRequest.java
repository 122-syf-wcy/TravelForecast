package com.travel.dto.user;

import lombok.Data;

/**
 * 用户修改密码请求
 */
@Data
public class PasswordChangeRequest {
    private String oldPassword;
    private String newPassword;
}

