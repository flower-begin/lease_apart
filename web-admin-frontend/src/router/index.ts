import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

/**
 * 路由 meta.perm：进入该页面需要的权限串（对应 sys_menu.perms）。
 * 不写 perm 表示「登录即可访问」。没有权限时跳 /403。
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无权访问', public: true }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'Odometer', perm: 'dashboard:view' }
      },
      {
        path: 'apartment',
        name: 'Apartment',
        component: () => import('@/views/apartment/index.vue'),
        meta: { title: '公寓管理', icon: 'OfficeBuilding', perm: 'apartment:list' }
      },
      {
        path: 'room',
        name: 'Room',
        component: () => import('@/views/room/index.vue'),
        meta: { title: '房间管理', icon: 'House', perm: 'room:list' }
      },
      {
        path: 'appointment',
        name: 'Appointment',
        component: () => import('@/views/appointment/index.vue'),
        meta: { title: '预约看房', icon: 'Calendar', perm: 'appointment:list' }
      },
      {
        path: 'agreement',
        name: 'Agreement',
        component: () => import('@/views/agreement/index.vue'),
        meta: { title: '租约管理', icon: 'Document', perm: 'agreement:list' }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '用户管理', icon: 'User', perm: 'user:list' }
      },
      {
        path: 'dict',
        name: 'Dict',
        meta: { title: '字典管理', icon: 'Collection' },
        children: [
          {
            path: 'label',
            name: 'DictLabel',
            component: () => import('@/views/dict/label.vue'),
            meta: { title: '标签管理', perm: 'dict:label:list' }
          },
          {
            path: 'facility',
            name: 'DictFacility',
            component: () => import('@/views/dict/facility.vue'),
            meta: { title: '配套管理', perm: 'dict:facility:list' }
          },
          {
            path: 'attr',
            name: 'DictAttr',
            component: () => import('@/views/dict/attr.vue'),
            meta: { title: '属性管理', perm: 'dict:attr:list' }
          },
          {
            path: 'fee',
            name: 'DictFee',
            component: () => import('@/views/dict/fee.vue'),
            meta: { title: '杂费管理', perm: 'dict:fee:list' }
          },
          {
            path: 'payment',
            name: 'DictPayment',
            component: () => import('@/views/dict/payment.vue'),
            meta: { title: '支付方式', perm: 'dict:payment:list' }
          },
          {
            path: 'term',
            name: 'DictTerm',
            component: () => import('@/views/dict/term.vue'),
            meta: { title: '租期管理', perm: 'dict:term:list' }
          }
        ]
      },
      {
        path: 'system',
        name: 'System',
        meta: { title: '系统管理', icon: 'Setting' },
        children: [
          {
            path: 'user',
            name: 'SystemUser',
            component: () => import('@/views/system/user.vue'),
            meta: { title: '员工管理', perm: 'system:user:list' }
          },
          {
            path: 'post',
            name: 'SystemPost',
            component: () => import('@/views/system/post.vue'),
            meta: { title: '岗位管理', perm: 'system:post:list' }
          },
          {
            path: 'role',
            name: 'SystemRole',
            component: () => import('@/views/system/role.vue'),
            meta: { title: '角色管理', perm: 'system:role:list' }
          }
        ]
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { public: true }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach(async (to, _from, next) => {
  const token = localStorage.getItem('admin_token')

  if (to.meta.public) {
    next()
    return
  }
  if (!token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  const userStore = useUserStore()

  // 刷新页面后 perms/menus 可能还没拉到，先补一次（失败说明登录态失效，拦截器会跳登录页）
  if (!userStore.userInfo?.perms?.length) {
    try {
      await userStore.loadInfo()
    } catch {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }

  // 页面级权限：手敲 URL 也进不去
  const perm = to.meta.perm as string | string[] | undefined
  if (perm && !userStore.hasPerm(perm)) {
    next({ path: '/403' })
    return
  }

  next()
})

router.afterEach(to => {
  const title = (to.meta.title as string) || ''
  document.title = title ? `${title} · 归心寓后台` : '归心寓后台'
})

export default router
