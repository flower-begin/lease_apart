import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { SystemUserInfoVo } from '@/types/api'

export const useUserStore = defineStore('adminUser', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const userInfo = ref<SystemUserInfoVo | null>(readUser())

  function readUser(): SystemUserInfoVo | null {
    const raw = localStorage.getItem('admin_user')
    if (!raw) return null
    try {
      return JSON.parse(raw)
    } catch {
      return null
    }
  }

  function setToken(value: string) {
    token.value = value
    localStorage.setItem('admin_token', value)
  }

  function setUserInfo(info: SystemUserInfoVo) {
    userInfo.value = info
    localStorage.setItem('admin_user', JSON.stringify(info))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_user')
  }

  return { token, userInfo, setToken, setUserInfo, logout }
})
