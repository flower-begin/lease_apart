<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="账号 / 昵称"
          clearable
          style="width: 180px"
          @keyup.enter="search"
        />
        <el-input
          v-model="query.phone"
          placeholder="手机号"
          clearable
          style="width: 160px"
          @keyup.enter="search"
        />
        <el-select v-model="query.roleCode" placeholder="全部角色" clearable style="width: 140px">
          <el-option label="管理员" value="admin" />
          <el-option label="租客" value="tenant" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>

        <div class="stat">
          共 <b>{{ stats.total }}</b> 个注册用户 ·
          管理员 {{ stats.admin }} · 租客 {{ stats.tenant }}
        </div>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="头像" width="80" align="center">
          <template #default="{ row }">
            <el-avatar :size="34" :src="resolveUrl(row.avatarUrl)">
              {{ (row.nickname || 'U').slice(0, 1) }}
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="昵称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="username" label="登录账号" width="130" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="角色" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.roleCode === 'admin' ? 'danger' : 'info'" size="small" effect="light">
              {{ row.roleCode === 'admin' ? '管理员' : '租客' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="dictType(BASE_STATUS, row.status)" size="small">
              {{ dictLabel(BASE_STATUS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button
              v-perm="'user:save'"
              link
              type="primary"
              @click="toggleRole(row)"
            >
              {{ row.roleCode === 'admin' ? '设为租客' : '设为管理员' }}
            </el-button>
            <el-button v-perm="'user:save'" link type="warning" @click="resetPwd(row)">重置密码</el-button>
            <el-button link :type="Number(row.status) === 1 ? 'danger' : 'success'" @click="toggle(row)">
              {{ Number(row.status) === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userApi } from '@/api'
import { BASE_STATUS, dictLabel, dictType } from '@/utils/dict'
import type { UserInfo } from '@/types/api'

const loading = ref(false)
const total = ref(0)
const rows = ref<UserInfo[]>([])

const query = reactive({
  current: 1,
  size: 10,
  keyword: '',
  phone: '',
  roleCode: ''
})

/** 顶部统计：注册用户总数 / 管理员 / 租客 */
const stats = reactive({ total: 0, admin: 0, tenant: 0 })

onMounted(() => {
  load()
  loadStatistics()
})

async function loadStatistics() {
  try {
    const res = await userApi.statistics()
    const data = res.data.data
    stats.total = data?.total ?? 0
    stats.admin = data?.admin ?? 0
    stats.tenant = data?.tenant ?? 0
  } catch {
    /* 拦截器已提示 */
  }
}

async function load() {
  loading.value = true
  try {
    const res = await userApi.page({ ...query })
    const data = res.data.data
    rows.value = data?.records || []
    total.value = data?.total || 0
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.current = 1
  load()
}

function reset() {
  query.current = 1
  query.keyword = ''
  query.phone = ''
  query.roleCode = ''
  load()
}

function resolveUrl(url?: string) {
  if (!url) return ''
  if (/^(https?:)?\/\//i.test(url) || url.startsWith('data:')) return url
  return url.startsWith('/') ? url : `/${url}`
}

async function toggle(row: UserInfo) {
  const next = Number(row.status) === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      `确定要${next === 0 ? '禁用' : '启用'}用户「${row.nickname || row.phone}」吗？`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await userApi.updateStatus(row.id!, next)
    ElMessage.success('操作成功')
    load()
  } catch {
    /* 拦截器已提示 */
  }
}

/** 设为管理员 / 设为租客 */
async function toggleRole(row: UserInfo) {
  const nextRole = row.roleCode === 'admin' ? 'tenant' : 'admin'
  const label = nextRole === 'admin' ? '管理员' : '租客'
  try {
    await ElMessageBox.confirm(
      nextRole === 'admin'
        ? `把「${row.nickname || row.username}」设为管理员？他将能登录后台管理系统。`
        : `把「${row.nickname || row.username}」设为租客？他将失去后台登录权限。`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await userApi.updateRole(row.id!, nextRole)
    ElMessage.success(`已设为${label}`)
    load()
    loadStatistics()
  } catch {
    /* 拦截器已提示 */
  }
}

/** 重置密码为 123456 */
async function resetPwd(row: UserInfo) {
  try {
    await ElMessageBox.confirm(
      `把「${row.nickname || row.username}」的密码重置为 123456？`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await userApi.resetPassword(row.id!)
    ElMessage.success('密码已重置为 123456')
  } catch {
    /* 拦截器已提示 */
  }
}
</script>

<style scoped>
.stat {
  margin-left: auto;
  font-size: 13px;
  color: #9c928a;
}
.stat b {
  color: #e07a5f;
  font-size: 15px;
}
</style>
