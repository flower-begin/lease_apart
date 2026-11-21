<template>
  <div class="app-shell">
    <!-- 顶部导航 -->
    <header class="app-header">
      <div class="header-inner">
        <router-link to="/search" class="logo">
          <img class="logo-badge" src="/logo.svg" alt="归心寓" />
          <span class="logo-text">归心寓</span>
        </router-link>

        <nav class="nav">
          <router-link
            v-for="item in navs"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            active-class="is-active"
          >
            {{ item.title }}
          </router-link>
        </nav>

        <div class="header-right">
          <template v-if="userStore.isLogin">
            <el-dropdown trigger="click" @command="onCommand">
              <span class="user-entry">
                <el-avatar :size="30" :src="userStore.userInfo?.avatarUrl">
                  {{ userStore.nickname.slice(0, 1) }}
                </el-avatar>
                <span class="user-name">{{ userStore.nickname }}</span>
                <el-icon class="caret"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="user">个人中心</el-dropdown-item>
                  <el-dropdown-item command="appointment">我的预约</el-dropdown-item>
                  <el-dropdown-item command="agreement">我的租约</el-dropdown-item>
                  <el-dropdown-item command="history">浏览历史</el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isAdmin" command="admin" divided>
                    进入后台管理
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" :divided="!userStore.isAdmin">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" round @click="goLogin">登录 / 注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <!-- 内容 -->
    <main class="app-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- 页脚 -->
    <footer class="app-footer">
      <div class="footer-inner">
        <p>归心寓 · 找房 / 预约看房 / 在线签约</p>
        <p class="muted">© {{ year }} 归心寓　本站为课程练习项目</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const year = new Date().getFullYear()

const navs = [
  { path: '/search', title: '找房' },
  { path: '/appointment/mine', title: '我的预约' },
  { path: '/agreement/mine', title: '我的租约' },
  { path: '/history', title: '浏览历史' }
]

function goLogin() {
  router.push({ path: '/login', query: { redirect: '/search' } })
}

async function onCommand(command: string) {
  // 管理员入口：后台管理端是独立的一个站（5174），这里直接开新窗口
  if (command === 'admin') {
    window.open(`${window.location.protocol}//${window.location.hostname}:5174`, '_blank')
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    } catch {
      return
    }
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/search')
    return
  }
  const map: Record<string, string> = {
    user: '/user',
    appointment: '/appointment/mine',
    agreement: '/agreement/mine',
    history: '/history'
  }
  if (map[command]) {
    router.push(map[command])
  }
}

onMounted(() => {
  if (userStore.isLogin) {
    userStore.fetchInfo()
  }
})
</script>

<style scoped>
.app-shell {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

/* ---------------- 顶部 ---------------- */
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--bg-header);
  box-shadow: 0 1px 8px rgba(120, 100, 80, 0.08);
}

.header-inner {
  max-width: var(--page-width);
  margin: 0 auto;
  height: 64px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 40px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.logo-badge {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  display: block;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: var(--brand);
  letter-spacing: 2px;
}

.nav {
  display: flex;
  align-items: center;
  gap: 28px;
  flex: 1;
}

.nav-item {
  font-size: 15px;
  color: var(--text-2);
  padding: 6px 0;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.nav-item:hover {
  color: var(--brand);
}

.nav-item.is-active {
  color: var(--brand);
  font-weight: 600;
  border-bottom-color: var(--brand);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 20px;
  transition: background 0.2s;
  outline: none;
}

.user-entry:hover {
  background: #f3e7db;
}

.user-name {
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-1);
}

.caret {
  color: var(--text-3);
  font-size: 12px;
}

/* ---------------- 主体 ---------------- */
.app-main {
  flex: 1;
}

/* ---------------- 页脚 ---------------- */
.app-footer {
  background: var(--bg-header);
  border-top: 1px solid var(--border);
  padding: 24px 20px;
  text-align: center;
  color: var(--text-2);
  font-size: 13px;
  line-height: 1.9;
}

.app-footer p {
  margin: 0;
}

/* 路由过渡 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.18s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
