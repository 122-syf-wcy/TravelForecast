import request from '@/utils/request'

/**
 * 角色管理API
 */

// 获取角色列表
export const getRoleList = (params?: {
  page?: number
  size?: number
  roleName?: string
  status?: string
}) => {
  return request({
    url: '/admin/roles',
    method: 'get',
    params
  })
}

// 获取角色详情
export const getRoleDetail = (id: number) => {
  return request({
    url: `/admin/roles/${id}`,
    method: 'get'
  })
}

// 创建角色
export const createRole = (data: {
  roleName: string
  roleCode: string
  description?: string
  status?: string
  permissionIds?: number[]
}) => {
  return request({
    url: '/admin/roles',
    method: 'post',
    data
  })
}

// 更新角色
export const updateRole = (id: number, data: {
  roleName: string
  roleCode: string
  description?: string
  status?: string
  permissionIds?: number[]
}) => {
  return request({
    url: `/admin/roles/${id}`,
    method: 'put',
    data
  })
}

// 删除角色
export const deleteRole = (id: number) => {
  return request({
    url: `/admin/roles/${id}`,
    method: 'delete'
  })
}

// 更新角色状态
export const updateRoleStatus = (id: number, status: string) => {
  return request({
    url: `/admin/roles/${id}/status`,
    method: 'patch',
    data: { status }
  })
}

// 获取角色的权限列表
export const getRolePermissions = (id: number) => {
  return request({
    url: `/admin/roles/${id}/permissions`,
    method: 'get'
  })
}

// 保存角色权限
export const saveRolePermissions = (id: number, permissionIds: number[]) => {
  return request({
    url: `/admin/roles/${id}/permissions`,
    method: 'put',
    data: { permissionIds }
  })
}

