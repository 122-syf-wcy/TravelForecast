package com.travel.service.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.admin.Role;

import java.util.List;

public interface RoleService {
    /**
     * 分页查询角色列表
     */
    Page<Role> getRoleList(int page, int size, String roleName, String status);
    
    /**
     * 根据ID获取角色
     */
    Role getRoleById(Long id);
    
    /**
     * 创建角色
     */
    void createRole(Role role);
    
    /**
     * 更新角色
     */
    void updateRole(Role role);
    
    /**
     * 删除角色
     */
    void deleteRole(Long id);
    
    /**
     * 更新角色状态
     */
    void updateRoleStatus(Long id, String status);
    
    /**
     * 获取角色的权限ID列表
     */
    List<Long> getRolePermissions(Long roleId);
    
    /**
     * 保存角色权限
     */
    void saveRolePermissions(Long roleId, List<Long> permissionIds);
}

