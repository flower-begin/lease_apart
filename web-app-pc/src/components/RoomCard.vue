<template>
  <div class="room-card card" @click="go">
    <div class="cover">
      <img v-if="cover" :src="cover" :alt="room.roomNumber" loading="lazy" />
      <div v-else class="cover-placeholder">暂无图片</div>
      <span v-if="room.isCheckIn" class="badge">已出租</span>
    </div>

    <div class="body">
      <h3 class="title">{{ title }}</h3>
      <p class="region">{{ regionText }}</p>

      <div class="labels">
        <el-tag
          v-for="label in labels"
          :key="label.id"
          size="small"
          effect="plain"
          type="success"
          class="label"
        >
          {{ label.name }}
        </el-tag>
        <span v-if="!labels.length" class="muted">—</span>
      </div>

      <div class="footer">
        <span class="price">
          <small>￥</small>{{ room.rent }}<small> /月</small>
        </span>
        <el-button size="small" type="primary" round @click.stop="go">查看详情</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { RoomItemVo } from '@/types/api'
import { firstImage } from '@/utils/format'

const props = defineProps<{ room: RoomItemVo }>()
const router = useRouter()

const cover = computed(() => firstImage(props.room.graphVoList))

const title = computed(() => {
  const name = props.room.apartmentInfo?.name || ''
  return `${name} ${props.room.roomNumber}`.trim()
})

const regionText = computed(() => {
  const a = props.room.apartmentInfo
  if (!a) return ''
  return [a.provinceName, a.cityName, a.districtName].filter(Boolean).join(' · ')
})

const labels = computed(() => (props.room.labelInfoList || []).slice(0, 4))

function go() {
  router.push(`/room/${props.room.id}`)
}
</script>

<style scoped>
.room-card {
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  display: flex;
  flex-direction: column;
}

.room-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.1);
}

.cover {
  position: relative;
  height: 170px;
  background: #f3efea;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-3);
  font-size: 13px;
}

.badge {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 2px 10px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 12px;
}

.body {
  padding: 14px 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.region {
  margin: 0;
  font-size: 12px;
  color: var(--text-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.labels {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 24px;
}

.label {
  background: #fdf3ef !important;
  border-color: #f2cc8f !important;
  color: #c9654c !important;
}

.footer {
  margin-top: auto;
  padding-top: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.price {
  color: var(--price);
  font-size: 20px;
  font-weight: 700;
}

.price small {
  font-size: 12px;
  font-weight: 400;
}
</style>
