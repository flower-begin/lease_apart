<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-toolbar">
        <el-input v-model="query.phone" placeholder="手机号" clearable style="width: 180px" @keyup.enter="search" />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
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
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="账号状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="dictType(BASE_STATUS, row.status)" size="small">
              {{ dictLabel(BASE_STATUS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
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
  phone: ''
})

onMounted(load)

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
  query.phone = ''
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
</script>
