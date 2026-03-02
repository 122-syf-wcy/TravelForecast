import { defineStore } from 'pinia'

export interface UserInfo {
    id?: number
    userId?: number  // 后端返回的字段
    username: string
    nickname?: string
    email?: string
    phone?: string
    avatar?: string
    role: 'user' | 'business' | 'merchant' | 'admin'
    status?: string
    createdAt?: string
    loginAt?: string
}

export const useUserStore = defineStore('user', {
    state: () => ({
        userInfo: null as UserInfo | null,
        token: '',
        isLoggedIn: false,
        isGuestMode: false
    }),

    getters: {
        getUserInfo: (state) => state.userInfo,
        getToken: (state) => state.token,
        isUser: (state) => state.userInfo?.role === 'user',
        isBusiness: (state) => state.userInfo?.role === 'business' || state.userInfo?.role === 'merchant',
        isAdmin: (state) => state.userInfo?.role === 'admin',
        getIsGuestMode: (state) => state.isGuestMode,
    },

    actions: {
    login(userInfo: UserInfo, token: string, refreshToken?: string) {
            this.userInfo = userInfo
            this.token = token
            this.isLoggedIn = true
            localStorage.setItem('token', token)
            localStorage.setItem('userInfo', JSON.stringify(userInfo))
      if (refreshToken) localStorage.setItem('refreshToken', refreshToken)
        },

        logout() {
            this.userInfo = null
            this.token = ''
            this.isLoggedIn = false
            this.isGuestMode = false
            localStorage.removeItem('token')
            localStorage.removeItem('userInfo')
            localStorage.removeItem('refreshToken')
            sessionStorage.removeItem('guestMode')
        },

        initUserFromStorage() {
            const token = localStorage.getItem('token')
            const userInfoStr = localStorage.getItem('userInfo')

            if (token && userInfoStr) {
                try {
                    const userInfo = JSON.parse(userInfoStr) as UserInfo
                    this.userInfo = userInfo
                    this.token = token
                    this.isLoggedIn = true
                    this.isGuestMode = false
                    return true
                } catch (e) {
                    this.logout()
                    return false
                }
            }
            
            // 检查访客模式
            const guestMode = sessionStorage.getItem('guestMode')
            if (guestMode === 'true') {
                this.isGuestMode = true
            }
            
            return false
        },

        enableGuestMode() {
            this.isGuestMode = true
            sessionStorage.setItem('guestMode', 'true')
        },

        disableGuestMode() {
            this.isGuestMode = false
            sessionStorage.removeItem('guestMode')
        },

        updateAvatar(avatarUrl: string) {
            if (this.userInfo) {
                this.userInfo.avatar = avatarUrl
                // 同步到localStorage
                localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
            }
        },

        updateUserInfo(updates: Partial<UserInfo>) {
            if (this.userInfo) {
                this.userInfo = { ...this.userInfo, ...updates }
                // 同步到localStorage
                localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
            }
        }
    }
}) 