import { defineStore } from 'pinia'
import { authApi } from '@/api'
import { TOKEN_KEY } from '@/utils/request'
import type { UserInfoVo } from '@/types/api'

export const useUserStore = defineStore('pc-user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: null as UserInfoVo | null
  }),

  getters: {
    isLogin: state => Boolean(state.token),
    nickname: state => state.userInfo?.nickname || '未登录'
  },

  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem(TOKEN_KEY, token)
    },

    /** 账号密码登录 */
    async login(form: { username: string; password: string }) {
      const res = await authApi.login(form)
      this.setToken(res.data)
      await this.fetchInfo()
    },

    /** 注册（成功后直接带上 token，免登录） */
    async register(form: { username: string; password: string; phone: string; nickname?: string }) {
      const res = await authApi.register(form)
      this.setToken(res.data)
      await this.fetchInfo()
    },

    /** 拉取当前用户信息 */
    async fetchInfo() {
      if (!this.token) return
      try {
        const res = await authApi.info()
        this.userInfo = res.data
      } catch {
        // 拦截器已提示，这里静默
      }
    },

    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem(TOKEN_KEY)
    }
  }
})
