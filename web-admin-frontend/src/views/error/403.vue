<template>
  <div class="forbidden">
    <el-result icon="error" title="403" sub-title="抱歉，你没有访问该页面的权限，请联系管理员分配角色">
      <template #extra>
        <el-button type="primary" @click="router.replace('/dashboard')">回到首页</el-button>
        <el-button @click="onLogout">切换账号</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

function onLogout() {
  userStore.logout()
  router.replace('/login')
}
</script>

<style scoped>
.forbidden {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f9f7f2;
}
</style>
