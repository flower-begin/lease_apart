<template>
  <div class="agreements">
    <van-nav-bar title="我的租约" left-text="返回" left-arrow @click-left="router.back" />
    <van-list v-model:loading="loading" :finished="finished" @load="onLoad">
      <van-cell v-for="item in items" :key="item.id" @click="goDetail(item.id)">
        <template #title>
          <span>{{ item.apartmentName }} - {{ item.roomNumber }}</span>
          <van-tag :type="leaseStatusType(item.leaseStatus)" style="margin-left: 8px;">
            {{ item.leaseStatus }}
          </van-tag>
        </template>
        <template #label>
          {{ item.leaseStartDate }} 至 {{ item.leaseEndDate }} | 租金: ¥{{ item.rent }}/月
        </template>
        <template #icon>
          <van-image :src="item.roomGraphVoList?.[0]?.url || ''" width="48" height="48" fit="cover" style="margin-right:10px;border-radius:4px;" />
        </template>
      </van-cell>
    </van-list>
    <van-empty v-if="!loading && items.length === 0" description="暂无租约" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listAgreements } from '@/api/agreement'
import type { AgreementItemVo } from '@/types/api'

const router = useRouter()
const items = ref<AgreementItemVo[]>([])
const loading = ref(false)
const finished = ref(true)

onMounted(async () => {
  loading.value = true
  try {
    const res = await listAgreements()
    items.value = res.data.data || []
  } catch {} finally {
    loading.value = false
  }
})

function onLoad() { finished.value = true }

function goDetail(_id: number) {}

function leaseStatusType(status: string): 'warning' | 'success' | 'danger' | 'default' | 'primary' {
  const map: Record<string, 'warning' | 'success' | 'danger' | 'default' | 'primary'> = {
    'SIGNING': 'warning',
    'SIGNED': 'success',
    'CANCELED': 'danger',
    'EXPIRED': 'default',
    'WITHDRAWING': 'warning',
    'WITHDRAWN': 'danger',
    'RENEWING': 'primary'
  }
  return map[status] || 'default'
}
</script>
