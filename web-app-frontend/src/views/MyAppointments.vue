<template>
  <div class="appointments">
    <van-nav-bar title="我的预约" left-text="返回" left-arrow @click-left="router.back" />
    <van-list v-model:loading="loading" :finished="finished" @load="onLoad">
      <van-cell v-for="item in items" :key="item.id" @click="goDetail(item.id)">
        <template #title>
          <span>{{ item.apartmentName }}</span>
          <van-tag :type="statusType(item.appointmentStatus)" style="margin-left: 8px;">
            {{ statusText(item.appointmentStatus) }}
          </van-tag>
        </template>
        <template #label>预约时间: {{ item.appointmentTime }}</template>
        <template #icon>
          <van-image :src="item.graphVoList?.[0]?.url || ''" width="48" height="48" fit="cover" style="margin-right:10px;border-radius:4px;" />
        </template>
      </van-cell>
    </van-list>
    <van-empty v-if="!loading && items.length === 0" description="暂无预约" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listAppointments } from '@/api/appointment'
import type { AppointmentItemVo } from '@/types/api'

const router = useRouter()
const items = ref<AppointmentItemVo[]>([])
const loading = ref(false)
const finished = ref(true)

onMounted(async () => {
  loading.value = true
  try {
    const res = await listAppointments()
    items.value = res.data.data || []
  } catch {} finally {
    loading.value = false
  }
})

function onLoad() { finished.value = true }

function goDetail(_id: number) {}

function statusType(status: number): 'warning' | 'danger' | 'success' | 'default' {
  const map = { 1: 'warning' as const, 2: 'danger' as const, 3: 'success' as const }
  return map[status as keyof typeof map] || 'default'
}

function statusText(status: number) {
  const map: Record<number, string> = { 1: '待看房', 2: '已取消', 3: '已看房' }
  return map[status] || '未知'
}
</script>
