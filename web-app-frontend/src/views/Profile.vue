<template>
  <div class="profile">
    <van-nav-bar title="我的" />
    <div class="user-header" @click="goLogin" v-if="!userStore.isLoggedIn">
      <van-icon name="user-o" size="50" />
      <p>点击登录</p>
    </div>
    <div class="user-header" v-else>
      <van-image :src="userStore.userInfo?.avatarUrl || ''" width="60" height="60" round />
      <p>{{ userStore.userInfo?.nickname || '用户' }}</p>
    </div>
    <van-cell-group>
      <van-cell title="浏览历史" icon="clock-o" is-link to="/profile" @click="showToast('查看历史在下方')" />
      <van-cell title="我的预约" icon="calendar-o" is-link to="/appointments" />
      <van-cell title="我的租约" icon="description-o" is-link to="/agreements" />
    </van-cell-group>
    <van-button v-if="userStore.isLoggedIn" type="danger" block round style="margin-top: 30px;" @click="logout">退出登录</van-button>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { getHistoryPage } from '@/api/history'
import type { HistoryItemVo } from '@/types/api'

const router = useRouter()
const userStore = useUserStore()
const histories = ref<HistoryItemVo[]>([])

onMounted(async () => {
  if (userStore.isLoggedIn) {
    try {
      const res = await getHistoryPage(1, 5)
      histories.value = res.data.data?.records || []
    } catch {}
  }
})

function goLogin() {
  router.push('/login')
}

function logout() {
  userStore.logout()
  showToast('已退出')
  router.push('/home')
}
</script>

<style scoped>
.user-header { display: flex; flex-direction: column; align-items: center; padding: 30px; background: #fff; margin-bottom: 10px; }
.user-header p { margin-top: 10px; font-size: 16px; }
</style>
