<template>
  <div class="forbidden">
    <el-result icon="error" :title="title" :sub-title="subTitle">
      <template #extra>
        <div class="actions">
          <el-button type="primary" @click="router.replace('/dashboard')">回到首页</el-button>
          <el-button @click="onLogout">切换账号</el-button>
        </div>

        <div class="debug">
          <div class="debug-title">当前登录信息（排查用）</div>
          <div>账号：{{ userStore.userInfo?.username || '—' }}</div>
          <div>角色：{{ userStore.roles.length ? userStore.roles.join('、') : '（未分配）' }}</div>
          <div>
            权限：{{ userStore.perms.includes('*') ? '* （超级管理员，全量权限）' : `${userStore.perms.length} 条` }}
          </div>
          <div v-if="need">该页面需要：{{ need }}</div>
          <div v-if="reason === 'norole'" class="debug-hint">
            没有角色时后端不会返回任何权限。请用管理员账号在
            「系统管理 → 员工管理 → 角色」里给该账号分配角色，或先执行
            <code>sql/upgrade-02-rbac.sql</code> 补全种子数据。
          </div>
        </div>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const reason = computed(() => String(route.query.reason || ''))
const need = computed(() => String(route.query.need || ''))

const title = computed(() => (reason.value === 'norole' ? '账号未分配角色' : '403'))

const subTitle = computed(() =>
  reason.value === 'norole'
    ? '该账号还没有被分配任何角色，所以拿不到任何菜单和权限'
    : '抱歉，你没有访问该页面的权限，请联系管理员分配角色'
)

function onLogout() {
  userStore.logout()
  router.replace('/login')
}
</script>

<style scoped>
.forbidden {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f9f7f2;
}
.actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
.debug {
  margin-top: 24px;
  padding: 14px 18px;
  border-radius: 8px;
  background: #fffdfa;
  border: 1px solid #e6d5c3;
  text-align: left;
  font-size: 13px;
  line-height: 1.9;
  color: #6b615a;
  max-width: 520px;
}
.debug-title {
  font-weight: 600;
  color: #3d3733;
  margin-bottom: 4px;
}
.debug-hint {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #e6d5c3;
  color: #9c928a;
}
.debug-hint code {
  background: #f9f7f2;
  padding: 1px 5px;
  border-radius: 3px;
}
</style>
