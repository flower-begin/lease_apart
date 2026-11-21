import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi } from '@/api'
import type { MenuVo, SystemUserInfoVo } from '@/types/api'

const TOKEN_KEY = 'admin_token'
const USER_KEY = 'admin_user'

export const useUserStore = defineStore('adminUser', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const userInfo = ref<SystemUserInfoVo | null>(readUser())

  function readUser(): SystemUserInfoVo | null {
    const raw = localStorage.getItem(USER_KEY)
    if (!raw) return null
    try {
      return JSON.parse(raw) as SystemUserInfoVo
    } catch {
      return null
    }
  }

  function setToken(value: string) {
    token.value = value
    localStorage.setItem(TOKEN_KEY, value)
  }

  function setUserInfo(info: SystemUserInfoVo) {
    userInfo.value = info
    localStorage.setItem(USER_KEY, JSON.stringify(info))
  }

  /** 角色编码，如 ['super_admin'] */
  const roles = computed<string[]>(() => userInfo.value?.roles || [])

  /** 权限串集合，超级管理员是 ['*'] */
  const perms = computed<string[]>(() => userInfo.value?.perms || [])

  /** 侧边栏菜单树（后端按权限过滤后返回） */
  const menus = computed<MenuVo[]>(() => userInfo.value?.menus || [])

  const isSuperAdmin = computed(() => roles.value.includes('super_admin'))

  /**
   * 是否拥有任一权限。
   * 超管的 perms 是 ['*']，这里统一短路，避免每处都判断角色。
   */
  function hasPerm(perm?: string | string[]): boolean {
    if (!perm) return true
    const own = perms.value
    if (own.includes('*')) return true
    const needed = Array.isArray(perm) ? perm : [perm]
    return needed.some(p => own.includes(p))
  }

  /** 重新拉取登录信息（刷新页面 / 权限变更后调用） */
  async function loadInfo() {
    const res = await authApi.info()
    if (res.data?.data) {
      setUserInfo(res.data.data)
    }
    return userInfo.value
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return {
    token,
    userInfo,
    roles,
    perms,
    menus,
    isSuperAdmin,
    hasPerm,
    loadInfo,
    setToken,
    setUserInfo,
    logout
  }
})
