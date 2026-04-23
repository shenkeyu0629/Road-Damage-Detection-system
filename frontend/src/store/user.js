import { defineStore } from 'pinia'
import { login, logout, getUserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null'),
    simulateRole: localStorage.getItem('simulateRole') || ''
  }),

  getters: {
    currentRole: (state) => {
      if (state.simulateRole) {
        return state.simulateRole
      }
      const roles = state.userInfo?.roles
      if (Array.isArray(roles)) {
        return roles[0] || 'admin'
      }
      return roles || 'admin'
    },
    
    rolePermissions: () => {
      return {
        admin: [
          'road:manage', 'section:manage',
          'monitor:view', 'monitor:detect',
          'inspection:view', 'inspection:manage',
          'record:view',
          'damage:history', 'damage:disposal', 'damage:analysis',
          'user:manage', 'role:manage', 'audit:manage', 'log:manage'
        ],
        reviewer: [
          'monitor:view',
          'inspection:view',
          'record:view',
          'damage:history', 'damage:disposal', 'damage:analysis',
          'audit:manage'
        ],
        inspector: [
          'monitor:view',
          'inspection:view',
          'record:view',
          'damage:history'
        ]
      }
    },
    
    currentPermissions: (state) => {
      let role = state.simulateRole
      if (!role) {
        const roles = state.userInfo?.roles
        if (Array.isArray(roles)) {
          role = roles[0] || 'admin'
        } else {
          role = roles || 'admin'
        }
      }
      const permissions = {
        admin: [
          'road:manage', 'section:manage',
          'monitor:view', 'monitor:detect',
          'inspection:view', 'inspection:manage',
          'record:view',
          'damage:history', 'damage:disposal', 'damage:analysis',
          'user:manage', 'role:manage', 'audit:manage', 'log:manage'
        ],
        reviewer: [
          'monitor:view',
          'inspection:view',
          'record:view',
          'damage:history', 'damage:disposal', 'damage:analysis',
          'audit:manage'
        ],
        inspector: [
          'monitor:view',
          'inspection:view',
          'record:view',
          'damage:history'
        ]
      }
      return permissions[role] || permissions.admin
    }
  },

  actions: {
    async login(loginForm) {
      const res = await login(loginForm)
      const data = res.data || res
      this.token = data.token
      localStorage.setItem('token', data.token)
      this.userInfo = {
        id: data.id,
        username: data.username,
        realName: data.realName,
        avatar: data.avatar,
        roles: data.roles || ['admin']
      }
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      return data
    },

    async getUserInfo() {
      const res = await getUserInfo()
      this.userInfo = res.data || res
      return this.userInfo
    },

    async logout() {
      try {
        await logout()
      } catch (e) {}
      this.token = ''
      this.userInfo = null
      this.simulateRole = ''
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('simulateRole')
    },
    
    setSimulateRole(role) {
      this.simulateRole = role
      localStorage.setItem('simulateRole', role)
    },
    
    clearSimulateRole() {
      this.simulateRole = ''
      localStorage.removeItem('simulateRole')
    }
  }
})
