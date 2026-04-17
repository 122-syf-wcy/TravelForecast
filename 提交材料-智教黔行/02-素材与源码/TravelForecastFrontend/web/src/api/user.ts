import request from '@/utils/request'
import type { UserInfo } from '@/store/user'

interface LoginParams {
    username: string
    password: string
    role: 'user' | 'business' | 'admin'
}

interface LoginResult {
    token: string
    userInfo: UserInfo
}

// 登录接口
export function login(data: LoginParams) {
    return request<LoginResult>({
        url: '/auth/login',
        method: 'post',
        data
    })
}

// 登出接口
export function logout() {
    return request({
        url: '/auth/logout',
        method: 'post'
    })
}

// 获取用户信息
export function getUserInfo() {
    return request<UserInfo>({
        url: '/users/me',
        method: 'get'
    })
}

// 修改密码
export function changePassword(userId: number, data: { oldPassword: string, newPassword: string }) {
    return request({
        url: `/users/${userId}/password`,
        method: 'put',
        data
    })
}

// 更新用户信息
export function updateUserInfo(userId: number, data: { nickname?: string, avatar?: string }) {
    return request({
        url: `/users/${userId}`,
        method: 'put',
        data
    })
}

// Sessions 列表
export function listSessions(userId: number) {
    return request<unknown>({
        url: `/users/${userId}/sessions`,
        method: 'get'
    })
}

export function revokeSession(userId: number, sessionId: string) {
    return request({
        url: `/users/${userId}/sessions/${sessionId}`,
        method: 'delete'
    })
}

// 偏好与隐私
export function updatePreferences(userId: number, data: Record<string, unknown>) {
    return request({
        url: `/users/${userId}/preferences`,
        method: 'put',
        data
    })
}

export function updatePrivacy(userId: number, data: Record<string, unknown>) {
    return request({
        url: `/users/${userId}/privacy`,
        method: 'put',
        data
    })
}

// 忘记/重置密码
export function forgotPassword(identifier: string) {
    return request({
        url: '/auth/forgot-password',
        method: 'post',
        data: { identifier }
    })
}

export function resetPassword(token: string, newPassword: string) {
    return request({
        url: '/auth/reset-password',
        method: 'post',
        data: { token, newPassword }
    })
}