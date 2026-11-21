import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true }
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
        meta: { title: '首页', icon: 'Odometer' }
      },
      {
        path: 'apartment',
        name: 'Apartment',
        component: () => import('@/views/apartment/index.vue'),
        meta: { title: '公寓管理', icon: 'OfficeBuilding' }
      },
      {
        path: 'room',
        name: 'Room',
        component: () => import('@/views/room/index.vue'),
        meta: { title: '房间管理', icon: 'House' }
      },
      {
        path: 'appointment',
        name: 'Appointment',
        component: () => import('@/views/appointment/index.vue'),
        meta: { title: '预约看房', icon: 'Calendar' }
      },
      {
        path: 'agreement',
        name: 'Agreement',
        component: () => import('@/views/agreement/index.vue'),
        meta: { title: '租约管理', icon: 'Document' }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '用户管理', icon: 'User' }
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
            meta: { title: '标签管理' }
          },
          {
            path: 'facility',
            name: 'DictFacility',
            component: () => import('@/views/dict/facility.vue'),
            meta: { title: '配套管理' }
          },
          {
            path: 'attr',
            name: 'DictAttr',
            component: () => import('@/views/dict/attr.vue'),
            meta: { title: '属性管理' }
          },
          {
            path: 'fee',
            name: 'DictFee',
            component: () => import('@/views/dict/fee.vue'),
            meta: { title: '杂费管理' }
          },
          {
            path: 'payment',
            name: 'DictPayment',
            component: () => import('@/views/dict/payment.vue'),
            meta: { title: '支付方式' }
          },
          {
            path: 'term',
            name: 'DictTerm',
            component: () => import('@/views/dict/term.vue'),
            meta: { title: '租期管理' }
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
            meta: { title: '员工管理' }
          },
          {
            path: 'post',
            name: 'SystemPost',
            component: () => import('@/views/system/post.vue'),
            meta: { title: '岗位管理' }
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

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('admin_token')
  if (to.meta.public) {
    next()
    return
  }
  if (!token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  next()
})

router.afterEach(to => {
  const title = (to.meta.title as string) || ''
  document.title = title ? `${title} · 尚庭公寓后台` : '尚庭公寓后台'
})

export default router
