import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { TOKEN_KEY } from '@/utils/request'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/search',
    children: [
      {
        path: 'search',
        name: 'search',
        component: () => import('@/views/search/index.vue'),
        meta: { title: '找房' }
      },
      {
        path: 'apartment/:id',
        name: 'apartmentDetail',
        component: () => import('@/views/apartment/detail.vue'),
        meta: { title: '公寓详情' }
      },
      {
        path: 'room/:id',
        name: 'roomDetail',
        component: () => import('@/views/room/detail.vue'),
        meta: { title: '房间详情' }
      },
      {
        path: 'appointment/create',
        name: 'appointmentCreate',
        component: () => import('@/views/appointment/create.vue'),
        meta: { title: '预约看房', auth: true }
      },
      {
        path: 'appointment/mine',
        name: 'appointmentMine',
        component: () => import('@/views/appointment/mine.vue'),
        meta: { title: '我的预约', auth: true }
      },
      // 注意：sign 必须排在 :id 之前，否则 /agreement/sign 会被当成 id
      {
        path: 'agreement/sign',
        name: 'agreementSign',
        component: () => import('@/views/agreement/sign.vue'),
        meta: { title: '签约', auth: true }
      },
      {
        path: 'agreement/mine',
        name: 'agreementMine',
        component: () => import('@/views/agreement/mine.vue'),
        meta: { title: '我的租约', auth: true }
      },
      {
        path: 'agreement/:id',
        name: 'agreementDetail',
        component: () => import('@/views/agreement/detail.vue'),
        meta: { title: '租约详情', auth: true }
      },
      {
        path: 'history',
        name: 'history',
        component: () => import('@/views/history/index.vue'),
        meta: { title: '浏览历史', auth: true }
      },
      {
        path: 'user',
        name: 'user',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '个人中心', auth: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'notFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  // 用 hash 模式：刷新、直接访问子路径都不会 404，部署到静态托管也不用配 rewrite
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(to => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (to.meta?.auth && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && token) {
    return { path: '/search' }
  }
  return true
})

router.afterEach(to => {
  const title = (to.meta?.title as string) || ''
  document.title = title ? `${title} · 归心寓` : '归心寓'
})

export default router
