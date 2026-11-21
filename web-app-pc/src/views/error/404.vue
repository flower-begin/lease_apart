<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在，或者已经被搬走了</p>
    <el-button type="primary" round @click="router.replace('/search')">回到首页</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.not-found {
  min-height: 70vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--text-2);
}

.not-found h1 {
  margin: 0;
  font-size: 72px;
  color: var(--brand);
  letter-spacing: 4px;
}

.not-found p {
  margin: 0 0 8px;
  font-size: 14px;
  color: var(--text-3);
}
</style>
