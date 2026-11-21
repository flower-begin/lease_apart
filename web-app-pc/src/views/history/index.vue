<template>
  <div class="page-wrap">
    <div class="head-row">
      <h2 class="section-title">浏览历史</h2>
      <span class="muted">共 {{ total }} 条</span>
    </div>

    <div v-loading="loading" class="grid">
      <div
        v-for="item in list"
        :key="item.id"
        class="card history-card"
        @click="router.push(`/room/${item.roomId}`)"
      >
        <div class="cover">
          <img v-if="item.roomGraphVoList?.length" :src="item.roomGraphVoList[0].url" :alt="item.roomNumber" />
          <div v-else class="cover-ph">暂无图片</div>
        </div>
        <div class="info">
          <h3 class="name">{{ item.apartmentName }} {{ item.roomNumber }}</h3>
          <p class="region">
            {{ [item.provinceName, item.cityName, item.districtName].filter(Boolean).join(' · ') || '-' }}
          </p>
          <p class="price"><small>￥</small>{{ item.rent }}<small> /月</small></p>
        </div>
      </div>
    </div>

    <el-empty
      v-if="!loading && !list.length"
      description="还没有浏览记录，去看看房子吧"
      class="empty-box"
    />

    <div v-if="total > size" class="pagination-bar">
      <el-pagination
        v-model:current-page="current"
        :page-size="size"
        :total="total"
        background
        layout="prev, pager, next, total"
        @current-change="fetchList"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { historyApi } from '@/api'
import type { HistoryItemVo } from '@/types/api'

const router = useRouter()

const list = ref<HistoryItemVo[]>([])
const current = ref(1)
const size = ref(12)
const total = ref(0)
const loading = ref(false)

async function fetchList() {
  loading.value = true
  try {
    const res = await historyApi.page({ current: current.value, size: size.value })
    list.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)
</script>

<style scoped>
.head-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 16px;
}

.head-row .section-title {
  margin: 0;
}

.grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  min-height: 60px;
}

.history-card {
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.history-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.1);
}

.cover {
  height: 140px;
  background: #f3efea;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-ph {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--text-3);
}

.info {
  padding: 12px 14px 14px;
}

.name {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.region {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--text-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--price);
}

.price small {
  font-size: 12px;
  font-weight: 400;
}

.pagination-bar {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

@media (max-width: 1100px) {
  .grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
