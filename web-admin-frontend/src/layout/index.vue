<template>
  <div class="layout-wrapper">
    <!-- ============ 侧边栏 ============ -->
    <aside class="layout-sidebar" :class="{ 'is-collapse': collapsed }">
      <div class="logo">
        <img class="logo-icon" src="/logo.svg" alt="归心寓" />
        <transition name="fade">
          <span v-show="!collapsed" class="logo-title">归心寓</span>
        </transition>
      </div>

      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          :collapse-transition="false"
          background-color="#3e3229"
          text-color="rgba(255,255,255,0.95)"
          active-text-color="#fff"
          router
          unique-opened
        >
          <template v-for="item in menus" :key="item.fullPath">
            <el-sub-menu v-if="item.children.length" :index="item.fullPath">
              <template #title>
                <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
                <span>{{ item.title }}</span>
              </template>
              <el-menu-item v-for="child in item.children" :key="child.path" :index="child.fullPath">
                {{ child.title }}
              </el-menu-item>
            </el-sub-menu>

            <el-menu-item v-else :index="item.fullPath">
              <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
              <template #title>{{ item.title }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </aside>

    <!-- ============ 右侧主体 ============ -->
    <div class="layout-main" :class="{ 'is-collapse': collapsed }">
      <header class="layout-header">
        <!-- 顶部导航条 60px -->
        <div class="nav-bar">
          <div class="nav-left">
            <el-icon class="action-icon" @click="collapsed = !collapsed">
              <component :is="collapsed ? 'Expand' : 'Fold'" />
            </el-icon>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item v-if="parentTitle">{{ parentTitle }}</el-breadcrumb-item>
              <el-breadcrumb-item v-if="currentTitle && currentTitle !== '首页'">
                {{ currentTitle }}
              </el-breadcrumb-item>
            </el-breadcrumb>
          </div>

          <div class="nav-right">
            <el-tooltip content="刷新当前页" placement="bottom">
              <el-icon class="action-icon" @click="refresh"><Refresh /></el-icon>
            </el-tooltip>
            <el-tooltip content="全屏" placement="bottom">
              <el-icon class="action-icon" @click="toggleFullscreen">
                <component :is="isFullscreen ? 'Aim' : 'FullScreen'" />
              </el-icon>
            </el-tooltip>

            <el-dropdown trigger="click" @command="onCommand">
              <div class="user">
                <el-avatar :size="30" :src="userStore.userInfo?.avatarUrl">
                  {{ (userStore.userInfo?.name || 'A').slice(0, 1) }}
                </el-avatar>
                <span class="user-name">{{ userStore.userInfo?.name || '管理员' }}</span>
                <el-icon><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <!-- 多标签页 55px -->
        <div class="tabs-bar">
          <el-scrollbar>
            <div class="tabs-inner">
              <div
                v-for="tab in tabsStore.tabs"
                :key="tab.path"
                class="tab-item"
                :class="{ active: tab.path === route.path }"
                @click="goTab(tab.path)"
              >
                <span class="dot" />
                <span class="tab-text">{{ tab.title }}</span>
                <el-icon v-if="tab.closable" class="tab-close" @click.stop="closeTab(tab.path)">
                  <Close />
                </el-icon>
              </div>
            </div>
          </el-scrollbar>

          <el-dropdown trigger="click" class="tabs-more" @command="onTabsCommand">
            <el-icon class="action-icon"><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="others">关闭其他</el-dropdown-item>
                <el-dropdown-item command="all">关闭全部</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区 -->
      <main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" :key="`${route.fullPath}-${refreshKey}`" />
          </transition>
        </router-view>
      </main>

      <!-- 页脚 55px -->
      <footer class="layout-footer">
        Copyright © {{ year }} 归心寓 · 后台管理系统
      </footer>
    </div>

    <!-- 个人信息弹窗 -->
    <el-dialog v-model="profileVisible" title="个人信息" width="420px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="姓名">{{ userStore.userInfo?.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="头像">
          <el-avatar :size="48" :src="userStore.userInfo?.avatarUrl" />
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="profileVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useTabsStore } from '@/stores/tabs'
import { authApi } from '@/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const tabsStore = useTabsStore()

const collapsed = ref(false)
const profileVisible = ref(false)
const isFullscreen = ref(false)
const refreshKey = ref(0)
const year = new Date().getFullYear()

interface MenuChild {
  path: string
  fullPath: string
  title: string
}

interface MenuItem {
  id: number | string
  path: string
  fullPath: string
  title: string
  icon: string
  children: MenuChild[]
}

function joinPath(parent: string, child?: string) {
  const base = parent.startsWith('/') ? parent : `/${parent}`
  if (!child) return base
  return `${base}/${child}`.replace(/\/+/g, '/')
}

/**
 * 侧边栏菜单：优先用后端按权限过滤后的菜单树（sys_menu + sys_role_menu）。
 * 目录节点没有真实路由，用 `dir-<id>` 当 el-sub-menu 的 index，避免多个目录都用 '/' 撞 key。
 * 拿不到后端菜单时回退到静态路由，并按 meta.perm 再过滤一遍。
 */
const menus = computed<MenuItem[]>(() => {
  const fromServer = userStore.menus || []
  if (fromServer.length) {
    return fromServer
      .map<MenuItem>(m => {
        const isDir = m.type === 1
        const dirKey = `dir-${m.id}`
        return {
          id: m.id,
          path: isDir ? dirKey : m.path || '',
          fullPath: isDir ? dirKey : m.path || '',
          title: m.name,
          icon: m.icon || '',
          children: (m.children || [])
            .filter(c => c.type === 2 && Boolean(c.path))
            .map(c => ({ path: c.path!, fullPath: c.path!, title: c.name }))
        }
      })
      // 目录下一个可见菜单都没有，就不显示这个目录
      .filter(m => m.children.length > 0 || !m.path.startsWith('dir-'))
  }

  const layout = router.options.routes.find(r => r.path === '/')
  const items = (layout?.children || [])
    .filter(child => Boolean(child.meta?.title))
    .filter(child => userStore.hasPerm(child.meta?.perm as string | string[] | undefined))
    .map<MenuItem>(child => ({
      id: child.path,
      path: child.path,
      fullPath: joinPath(child.path),
      title: String(child.meta?.title ?? ''),
      icon: String(child.meta?.icon ?? ''),
      children: (child.children || [])
        .filter(grand => Boolean(grand.meta?.title))
        .filter(grand => userStore.hasPerm(grand.meta?.perm as string | string[] | undefined))
        .map(grand => ({
          path: grand.path,
          fullPath: joinPath(child.path, grand.path),
          title: String(grand.meta?.title ?? '')
        }))
    }))

  return items.filter(item => {
    const src = (layout?.children || []).find(c => c.path === item.path)
    return !src?.children?.length || item.children.length > 0
  })
})

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => (route.meta.title as string) || '')
const parentTitle = computed(() => {
  const matched = route.matched
  if (matched.length >= 2) {
    return (matched[matched.length - 2].meta?.title as string) || ''
  }
  return ''
})

/** 路由变化时同步标签页 */
watch(
  () => route.path,
  () => {
    const title = (route.meta.title as string) || ''
    if (!title) return
    tabsStore.addTab({
      path: route.path,
      title,
      closable: route.path !== '/dashboard'
    })
  },
  { immediate: true }
)

function goTab(path: string) {
  if (path !== route.path) router.push(path)
}

function closeTab(path: string) {
  const next = tabsStore.closeTab(path)
  if (next) router.push(next)
}

function onTabsCommand(command: string) {
  if (command === 'others') {
    tabsStore.closeOthers(route.path)
    return
  }
  if (command === 'all') {
    const next = tabsStore.closeAll()
    router.push(next)
  }
}

function refresh() {
  refreshKey.value++
  ElMessage.success('已刷新')
}

async function toggleFullscreen() {
  try {
    if (!document.fullscreenElement) {
      await document.documentElement.requestFullscreen()
      isFullscreen.value = true
    } else {
      await document.exitFullscreen()
      isFullscreen.value = false
    }
  } catch {
    ElMessage.warning('当前浏览器不支持全屏')
  }
}

// 窄屏自动折叠
function onResize() {
  const width = document.body.clientWidth
  if (width < 1200 && !collapsed.value) collapsed.value = true
  if (width >= 1200 && collapsed.value) collapsed.value = false
}

onMounted(async () => {
  window.addEventListener('resize', onResize)
  onResize()

  if (!userStore.userInfo) {
    try {
      const res = await authApi.info()
      if (res.data.data) userStore.setUserInfo(res.data.data)
    } catch {
      /* 拦截器已处理 */
    }
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
})

async function onCommand(command: string) {
  if (command === 'profile') {
    profileVisible.value = true
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    } catch {
      return
    }
    userStore.logout()
    tabsStore.reset()
    ElMessage.success('已退出')
    router.replace('/login')
  }
}
</script>

<style scoped>
/* ---------------- 设计变量（对齐 rentHouseAdmin） ---------------- */
.layout-wrapper {
  --sidebar-width: 256px;
  --sidebar-width-min: 64px;
  --logo-height: 75px;
  --navbar-height: 60px;
  --tabsbar-height: 55px;
  --footer-height: 55px;
  --menu-bg: #3e3229;
  --menu-bg-active: #e07a5f;
  --menu-sub-bg: #332a22;
  --content-bg: #f9f7f2;
  --base-shadow: 0 1px 4px rgb(70 55 40 / 10%);
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

/* ---------------- 侧边栏 ---------------- */
.layout-sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 1001;
  width: var(--sidebar-width);
  background: var(--menu-bg);
  display: flex;
  flex-direction: column;
  transition: width 0.28s;
  overflow: hidden;
}
.layout-sidebar.is-collapse {
  width: var(--sidebar-width-min);
}

.logo {
  height: var(--logo-height);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  white-space: nowrap;
  border-bottom: 1px solid rgb(255 255 255 / 6%);
  background: #332a22;
}
.logo-icon {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  display: block;
}
.logo-title {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #fff;
}

.menu-scroll {
  flex: 1;
  overflow: hidden;
}
.layout-sidebar :deep(.el-menu) {
  border-right: none;
}
.layout-sidebar :deep(.el-menu-item),
.layout-sidebar :deep(.el-sub-menu__title) {
  height: 50px;
  line-height: 50px;
  font-size: 14px;
}
.layout-sidebar :deep(.el-sub-menu .el-menu-item) {
  background-color: var(--menu-sub-bg) !important;
}
.layout-sidebar :deep(.el-menu-item:hover),
.layout-sidebar :deep(.el-sub-menu__title:hover) {
  background-color: #4a3c31 !important;
}
.layout-sidebar :deep(.el-menu-item.is-active) {
  background-color: var(--menu-bg-active) !important;
  color: #fff !important;
}

/* ---------------- 主体 ---------------- */
.layout-main {
  margin-left: var(--sidebar-width);
  height: 100vh;
  display: flex;
  flex-direction: column;
  transition: margin-left 0.28s;
  background: var(--content-bg);
}
.layout-main.is-collapse {
  margin-left: var(--sidebar-width-min);
}

.layout-header {
  flex-shrink: 0;
  background: #fff;
  box-shadow: var(--base-shadow);
  z-index: 999;
}

/* 顶部导航条 */
.nav-bar {
  height: var(--navbar-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #f0f0f0;
}
.nav-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.nav-right {
  display: flex;
  align-items: center;
  gap: 6px;
}
.action-icon {
  font-size: 18px;
  color: #5a5e66;
  cursor: pointer;
  padding: 6px;
  border-radius: 4px;
  transition: all 0.2s;
}
.action-icon:hover {
  background: #f6f6f6;
  color: #e07a5f;
}

.user {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 10px;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  outline: none;
  transition: background 0.2s;
}
.user:hover {
  background: #f6f6f6;
}
.user-name {
  font-size: 14px;
  color: #303133;
}

/* 多标签页 */
.tabs-bar {
  height: var(--tabsbar-height);
  display: flex;
  align-items: center;
  padding: 0 10px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}
.tabs-inner {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 0;
}
.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 10px;
  font-size: 13px;
  color: #5a5e66;
  background: #fff;
  border: 1px solid #d8dce5;
  border-radius: 2px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}
.tab-item .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #b4bccc;
}
.tab-item:hover {
  color: #e07a5f;
  border-color: #e07a5f;
}
.tab-item.active {
  color: #fff;
  background: #e07a5f;
  border-color: #e07a5f;
}
.tab-item.active .dot {
  background: #fff;
}
.tab-close {
  font-size: 12px;
  border-radius: 50%;
}
.tab-close:hover {
  background: rgb(0 0 0 / 15%);
}
.tabs-more {
  margin-left: auto;
  padding-left: 8px;
}

/* 内容区 */
.app-main {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

/* 页脚 */
.layout-footer {
  flex-shrink: 0;
  height: var(--footer-height);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #909399;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

/* 过渡 */
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.2s;
}
.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-12px);
}
.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(12px);
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
