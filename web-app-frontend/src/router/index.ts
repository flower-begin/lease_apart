import { createRouter, createWebHashHistory } from 'vue-router'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      redirect: '/home'
    },
    {
      path: '/home',
      name: 'Home',
      component: () => import('@/views/Home.vue')
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue')
    },
    {
      path: '/apartment',
      name: 'ApartmentList',
      component: () => import('@/views/ApartmentList.vue')
    },
    {
      path: '/apartment/:id',
      name: 'ApartmentDetail',
      component: () => import('@/views/ApartmentDetail.vue')
    },
    {
      path: '/room/:id',
      name: 'RoomDetail',
      component: () => import('@/views/RoomDetail.vue')
    },
    {
      path: '/ai',
      name: 'AiChat',
      component: () => import('@/views/AiChat.vue')
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/views/Profile.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/appointments',
      name: 'MyAppointments',
      component: () => import('@/views/MyAppointments.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/agreements',
      name: 'MyAgreements',
      component: () => import('@/views/MyAgreements.vue'),
      meta: { requiresAuth: true }
    }
  ]
})

router.beforeEach((to, _from, next) => {
  if (to.meta.requiresAuth) {
    const token = localStorage.getItem('token')
    if (!token) {
      next('/login')
      return
    }
  }
  next()
})

export default router
