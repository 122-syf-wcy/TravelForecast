package com.travel.dto.admin;

import lombok.Data;

import java.util.List;

/**
 * 保存权限请求
 */
@Data
public class PermissionSaveRequest {
    private List<Long> permissionIds;
}

