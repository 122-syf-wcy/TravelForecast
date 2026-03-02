package com.travel.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.admin.Role;
import com.travel.entity.admin.RolePermission;
import com.travel.mapper.admin.RoleMapper;
import com.travel.mapper.admin.RolePermissionMapper;
import com.travel.service.admin.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @Override
    public Page<Role> getRoleList(int page, int size, String roleName, String status) {
        Page<Role> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        
        if (roleName != null && !roleName.isEmpty()) {
            wrapper.like(Role::getRoleName, roleName)
                   .or()
                   .like(Role::getRoleCode, roleName);
        }
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Role::getStatus, status);
        }
        
        wrapper.orderByDesc(Role::getCreatedAt);
        
        return roleMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public Role getRoleById(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    @Transactional
    public void createRole(Role role) {
        if (role.getStatus() == null) {
            role.setStatus("active");
        }
        if (role.getUserCount() == null) {
            role.setUserCount(0);
        }
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.insert(role);
        log.info("创建角色: {}", role.getRoleName());
    }

    @Override
    @Transactional
    public void updateRole(Role role) {
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.updateById(role);
        log.info("更新角色: {}", role.getRoleName());
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        // 删除角色权限关联
        LambdaQueryWrapper<RolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermission::getRoleId, id);
        rolePermissionMapper.delete(wrapper);
        
        // 删除角色
        roleMapper.deleteById(id);
        log.info("删除角色: {}", id);
    }

    @Override
    @Transactional
    public void updateRoleStatus(Long id, String status) {
        Role role = roleMapper.selectById(id);
        if (role != null) {
            role.setStatus(status);
            role.setUpdatedAt(LocalDateTime.now());
            roleMapper.updateById(role);
            log.info("更新角色状态: {} -> {}", id, status);
        }
    }

    @Override
    public List<Long> getRolePermissions(Long roleId) {
        LambdaQueryWrapper<RolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermission::getRoleId, roleId);
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(wrapper);
        return rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveRolePermissions(Long roleId, List<Long> permissionIds) {
        // 删除现有权限
        LambdaQueryWrapper<RolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermission::getRoleId, roleId);
        rolePermissionMapper.delete(wrapper);
        
        // 添加新权限
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permissionId : permissionIds) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleId(roleId);
                rolePermission.setPermissionId(permissionId);
                rolePermission.setCreatedAt(LocalDateTime.now());
                rolePermissionMapper.insert(rolePermission);
            }
        }
        
        log.info("保存角色权限: roleId={}, permissionIds={}", roleId, permissionIds);
    }
}

