<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-toolbar">
        <el-select
          v-model="query.apartmentId"
          placeholder="选择公寓"
          clearable
          filterable
          style="width: 220px"
          @change="search"
        >
          <el-option v-for="a in apartments" :key="a.id" :label="a.name" :value="a.id!" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <div class="spacer"></div>
        <el-button type="primary" :icon="Plus" @click="openForm()">新增房间</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="roomNumber" label="房间号" width="110" />
        <el-table-column label="所属公寓" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.apartmentInfo?.name || '-' }}</template>
        </el-table-column>
        <el-table-column label="租金" width="120" align="right">
          <template #default="{ row }">
            <span class="text-danger">¥{{ Number(row.rent || 0).toFixed(0) }}</span>
            <span class="text-muted"> /月</span>
          </template>
        </el-table-column>
        <el-table-column label="入住状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isCheckIn" type="danger" size="small">已出租</el-tag>
            <el-tag v-else type="success" size="small">空闲</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="租约到期" width="130">
          <template #default="{ row }">
            <span v-if="row.leaseEndDate">{{ String(row.leaseEndDate).slice(0, 10) }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="发布状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="dictType(RELEASE_STATUS, row.isRelease)" size="small">
              {{ dictLabel(RELEASE_STATUS, row.isRelease) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row.id)">编辑</el-button>
            <el-button link type="primary" @click="toggleRelease(row)">
              {{ Number(row.isRelease) === 1 ? '下架' : '发布' }}
            </el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
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

    <room-form :apartments="apartments" @saved="load" ref="formRef" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { apartmentApi, roomApi } from '@/api'
import { RELEASE_STATUS, dictLabel, dictType } from '@/utils/dict'
import RoomForm from './components/RoomForm.vue'
import type { ApartmentItemVo, RoomItemVo } from '@/types/api'

const route = useRoute()

const loading = ref(false)
const total = ref(0)
const rows = ref<RoomItemVo[]>([])
const apartments = ref<ApartmentItemVo[]>([])
const formRef = ref<InstanceType<typeof RoomForm>>()

const query = reactive({
  current: 1,
  size: 10,
  apartmentId: undefined as number | undefined
})

onMounted(async () => {
  // 从公寓页"房间"按钮跳过来时带上 apartmentId
  const fromQuery = route.query.apartmentId
  if (fromQuery) {
    query.apartmentId = Number(fromQuery)
  }
  await loadApartments()
  await load()
})

async function loadApartments() {
  try {
    const res = await apartmentApi.page({ current: 1, size: 200 })
    apartments.value = res.data.data?.records || []
  } catch {
    /* 拦截器已提示 */
  }
}

async function load() {
  loading.value = true
  try {
    const res = await roomApi.page({ ...query })
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
  query.apartmentId = undefined
  load()
}

function openForm(id?: number) {
  formRef.value?.open(id, query.apartmentId)
}

async function toggleRelease(row: RoomItemVo) {
  const next = Number(row.isRelease) === 1 ? 0 : 1
  try {
    await roomApi.updateRelease(row.id!, next)
    ElMessage.success(next === 1 ? '已发布' : '已下架')
    load()
  } catch {
    /* 拦截器已提示 */
  }
}

async function remove(row: RoomItemVo) {
  try {
    await ElMessageBox.confirm(`确定删除房间「${row.roomNumber}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await roomApi.remove(row.id!)
    ElMessage.success('已删除')
    load()
  } catch {
    /* 拦截器已提示 */
  }
}
</script>
