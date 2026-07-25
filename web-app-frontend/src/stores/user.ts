import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfoVo } from '@/types/api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfoVo | null>(null)
  const isLoggedIn = ref(!!token.value)

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
    isLoggedIn.value = true
  }

  function setUserInfo(info: UserInfoVo) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    isLoggedIn.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  // Restore user info from localStorage
  const savedInfo = localStorage.getItem('userInfo')
  if (savedInfo) {
    try {
      userInfo.value = JSON.parse(savedInfo)
    } catch {}
  }

  return { token, userInfo, isLoggedIn, setToken, setUserInfo, logout }
})
