package com.travel.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.Result;
import com.travel.dto.admin.PermissionSaveRequest;
import com.travel.dto.admin.RoleCreateRequest;
import com.travel.dto.admin.RoleUpdateRequest;
import com.travel.dto.admin.StatusUpdateRequest;
import com.travel.entity.admin.Role;
import com.travel.service.admin.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员角色管理接口
 */
@Slf4j
@RestController
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final RoleService roleService;

    /**
     * 分页查询角色列表
     */
    @GetMapping
    public Result<Page<Role>> getRoleList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String status) {

        Page<Role> result = roleService.getRoleList(page, size, roleName, status);
        return Result.success(result);
    }

    /**
     * 获取角色详情
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getRoleDetail(@PathVariable Long id) {
        Role role = roleService.getRoleById(id);
        if (role == null) {
            return Result.error("角色不存在");
        }

        List<Long> permissionIds = roleService.getRolePermissions(id);

        Map<String, Object> data = new HashMap<>();
        data.put("role", role);
        data.put("permissionIds", permissionIds);

        return Result.success(data);
    }

    /**
     * 创建角色
     */
    @PostMapping
    public Result<Void> createRole(@RequestBody RoleCreateRequest request) {
        Role role = new Role();
        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus() != null ? request.getStatus() : "active");

        roleService.createRole(role);

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            roleService.saveRolePermissions(role.getId(), request.getPermissionIds());
        }

        log.info("创建角色成功: {}", role.getRoleName());
        return Result.success(null, "角色创建成功");
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody RoleUpdateRequest request) {
        Role role = roleService.getRoleById(id);
        if (role == null) {
            return Result.error("角色不存在");
        }

        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            role.setStatus(request.getStatus());
        }

        roleService.updateRole(role);

        if (request.getPermissionIds() != null) {
            roleService.saveRolePermissions(id, request.getPermissionIds());
        }

        log.info("更新角色成功: {}", role.getRoleName());
        return Result.success(null, "角色更新成功");
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        Role role = roleService.getRoleById(id);
        if (role == null) {
            return Result.error("角色不存在");
        }

        if ("ADMIN".equals(role.getRoleCode()) ||
            "BUSINESS".equals(role.getRoleCode()) ||
            "USER".equals(role.getRoleCode())) {
            return Result.error("系统核心角色不能删除");
        }

        roleService.deleteRole(id);
        log.info("删除角色成功: {}", role.getRoleName());
        return Result.success(null, "角色删除成功");
    }

    /**
     * 更新角色状态
     */
    @PatchMapping("/{id}/status")
    public Result<Void> updateRoleStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        roleService.updateRoleStatus(id, request.getStatus());
        log.info("更新角色状态: {} -> {}", id, request.getStatus());
        return Result.success(null, "状态更新成功");
    }

    /**
     * 获取角色的权限列表
     */
    @GetMapping("/{id}/permissions")
    public Result<List<Long>> getRolePermissions(@PathVariable Long id) {
        List<Long> permissionIds = roleService.getRolePermissions(id);
        return Result.success(permissionIds);
    }

    /**
     * 保存角色权限
     */
    @PutMapping("/{id}/permissions")
    public Result<Void> saveRolePermissions(@PathVariable Long id, @RequestBody PermissionSaveRequest request) {
        roleService.saveRolePermissions(id, request.getPermissionIds());
        log.info("保存角色权限成功: roleId={}", id);
        return Result.success(null, "权限保存成功");
    }
}
