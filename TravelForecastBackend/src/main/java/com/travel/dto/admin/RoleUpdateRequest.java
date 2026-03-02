package com.travel.dto.admin;

import lombok.Data;

import java.util.List;

/**
 * 更新角色请求
 */
@Data
public class RoleUpdateRequest {
    private String roleName;
    private String roleCode;
    private String description;
    private String status;
    private List<Long> permissionIds;
}

