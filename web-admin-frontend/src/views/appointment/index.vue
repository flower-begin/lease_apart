<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-toolbar">
        <el-input v-model="query.name" placeholder="客户姓名" clearable style="width: 160px" @keyup.enter="search" />
        <el-input v-model="query.phone" placeholder="手机号" clearable style="width: 160px" @keyup.enter="search" />
        <el-select v-model="query.apartmentId" placeholder="公寓" clearable filterable style="width: 200px">
          <el-option v-for="a in apartments" :key="a.id" :label="a.name" :value="a.id!" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="name" label="客户" width="110" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="预约公寓" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.apartmentInfo?.name || '-' }}</template>
        </el-table-column>
        <el-table-column label="看房时间" width="170">
          <template #default="{ row }">{{ formatTime(row.appointmentTime) }}</template>
        </el-table-column>
        <el-table-column prop="additionalInfo" label="备注" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.additionalInfo || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="dictType(APPOINTMENT_STATUS, row.appointmentStatus)" size="small">
              {{ dictLabel(APPOINTMENT_STATUS, row.appointmentStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="success"
              :disabled="Number(row.appointmentStatus) === 3"
              @click="changeStatus(row, 3, '标记为已看房')"
            >
              已看房
            </el-button>
            <el-button
              link
              type="warning"
              :disabled="Number(row.appointmentStatus) === 2"
              @click="changeStatus(row, 2, '取消预约')"
            >
              取消
            </el-button>
            <el-button
              link
              type="primary"
              :disabled="Number(row.appointmentStatus) === 1"
              @click="changeStatus(row, 1, '恢复为待看房')"
            >
              待看房
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
import { apartmentApi, appointmentApi } from '@/api'
import { APPOINTMENT_STATUS, dictLabel, dictType } from '@/utils/dict'
import type { ApartmentItemVo, AppointmentVo } from '@/types/api'

const loading = ref(false)
const total = ref(0)
const rows = ref<AppointmentVo[]>([])
const apartments = ref<ApartmentItemVo[]>([])

const query = reactive({
  current: 1,
  size: 10,
  name: '',
  phone: '',
  apartmentId: undefined as number | undefined
})

onMounted(async () => {
  try {
    const res = await apartmentApi.page({ current: 1, size: 200 })
    apartments.value = res.data.data?.records || []
  } catch {
    /* ignore */
  }
  await load()
})

async function load() {
  loading.value = true
  try {
    const res = await appointmentApi.page({ ...query })
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
  query.name = ''
  query.phone = ''
  query.apartmentId = undefined
  load()
}

function formatTime(value?: string) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

async function changeStatus(row: AppointmentVo, status: number, action: string) {
  try {
    await ElMessageBox.confirm(`确定要${action}（${row.name || row.phone}）吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await appointmentApi.updateStatus(row.id!, status)
    ElMessage.success('操作成功')
    load()
  } catch {
    /* 拦截器已提示 */
  }
}
</script>
