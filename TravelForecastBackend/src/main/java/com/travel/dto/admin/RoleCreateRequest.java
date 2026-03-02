package com.travel.dto.admin;

import lombok.Data;

import java.util.List;

/**
 * 创建角色请求
 */
@Data
public class RoleCreateRequest {
    private String roleName;
    private String roleCode;
    private String description;
    private String status;
    private List<Long> permissionIds;
}

