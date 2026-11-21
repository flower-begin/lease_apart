<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col :xs="12" :sm="12" :md="6" v-for="card in cards" :key="card.label">
        <div class="stat-card">
          <div class="stat-icon" :style="{ background: card.bg }">
            <el-icon :size="22"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :md="14" :xs="24">
        <div class="page-card">
          <div class="page-title">房源概览</div>
          <el-table :data="apartmentRows" v-loading="loading" size="small">
            <el-table-column prop="name" label="公寓" min-width="140" show-overflow-tooltip />
            <el-table-column prop="cityName" label="城市" width="90" />
            <el-table-column label="房间" width="130">
              <template #default="{ row }">
                <span class="text-success">{{ row.freeRoomCount ?? 0 }}</span>
                <span class="text-muted"> / {{ row.totalRoomCount ?? 0 }} 空闲</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="dictType(RELEASE_STATUS, row.isRelease)" size="small">
                  {{ dictLabel(RELEASE_STATUS, row.isRelease) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>

      <el-col :md="10" :xs="24">
        <div class="page-card">
          <div class="page-title">最近预约</div>
          <el-table :data="recentAppointments" v-loading="loading" size="small">
            <el-table-column prop="name" label="客户" width="90" show-overflow-tooltip />
            <el-table-column label="公寓" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ row.apartmentInfo?.name || '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="88">
              <template #default="{ row }">
                <el-tag :type="dictType(APPOINTMENT_STATUS, row.appointmentStatus)" size="small">
                  {{ dictLabel(APPOINTMENT_STATUS, row.appointmentStatus) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>

    <div class="page-card" style="margin-top: 16px">
      <div class="page-title">快捷操作</div>
      <div class="quick-links">
        <el-button @click="router.push('/apartment')">公寓管理</el-button>
        <el-button @click="router.push('/room')">房间管理</el-button>
        <el-button @click="router.push('/agreement')">租约管理</el-button>
        <el-button @click="router.push('/appointment')">预约看房</el-button>
        <el-button @click="router.push('/dict/label')">字典管理</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { agreementApi, apartmentApi, appointmentApi, roomApi, userApi } from '@/api'
import { APPOINTMENT_STATUS, RELEASE_STATUS, dictLabel, dictType } from '@/utils/dict'
import type { ApartmentItemVo, AppointmentVo } from '@/types/api'

const router = useRouter()
const loading = ref(true)

const apartments = ref<ApartmentItemVo[]>([])
const recentAppointments = ref<AppointmentVo[]>([])

const counts = ref({ apartments: 0, rooms: 0, appointments: 0, agreements: 0, users: 0 })

const apartmentRows = computed(() => apartments.value.slice(0, 6))

const cards = computed(() => [
  { label: '公寓总数', value: counts.value.apartments, icon: 'OfficeBuilding', bg: 'linear-gradient(135deg,#c3a181,#e6d5c3)' },
  { label: '房间总数', value: counts.value.rooms, icon: 'House', bg: 'linear-gradient(135deg,#e07a5f,#f2b39c)' },
  { label: '待看房预约', value: counts.value.appointments, icon: 'Calendar', bg: 'linear-gradient(135deg,#e6b980,#f2cc8f)' },
  { label: '生效租约', value: counts.value.agreements, icon: 'Document', bg: 'linear-gradient(135deg,#b3624c,#d68e78)' }
])

onMounted(load)

async function load() {
  loading.value = true
  try {
    const [aptRes, roomRes, aptStatusRes, agrRes, userRes] = await Promise.allSettled([
      apartmentApi.page({ current: 1, size: 100 }),
      roomApi.page({ current: 1, size: 100 }),
      appointmentApi.page({ current: 1, size: 8 }),
      agreementApi.page({ current: 1, size: 100 }),
      userApi.page({ current: 1, size: 1 })
    ])

    if (aptRes.status === 'fulfilled') {
      apartments.value = aptRes.value.data.data?.records || []
      counts.value.apartments = aptRes.value.data.data?.total || 0
    }
    if (roomRes.status === 'fulfilled') {
      counts.value.rooms = roomRes.value.data.data?.total || 0
    }
    if (aptStatusRes.status === 'fulfilled') {
      const data = aptStatusRes.value.data.data
      recentAppointments.value = (data?.records || []).slice(0, 6)
      counts.value.appointments = (data?.records || []).filter(r => Number(r.appointmentStatus) === 1).length
    }
    if (agrRes.status === 'fulfilled') {
      const records = agrRes.value.data.data?.records || []
      counts.value.agreements = records.filter(r => Number(r.status) === 2).length
    }
    if (userRes.status === 'fulfilled') {
      counts.value.users = userRes.value.data.data?.total || 0
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fff;
  border-radius: 8px;
  padding: 18px 16px;
  margin-bottom: 8px;
}
.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.1;
}
.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.quick-links {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
