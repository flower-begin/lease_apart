<template>
  <div class="room-card" @click="$emit('click')">
    <van-row gutter="10">
      <van-col span="8">
        <van-image
          :src="room.graphVoList?.[0]?.url || 'https://via.placeholder.com/150x120?text=Room'"
          width="100%"
          height="80"
          fit="cover"
          radius="6"
        />
      </van-col>
      <van-col span="16">
        <div class="room-info">
          <h3>{{ room.roomNumber }}</h3>
          <p class="apt-name" v-if="room.apartmentInfo?.name">{{ room.apartmentInfo.name }}</p>
          <p class="rent">¥{{ room.rent }}<span>/月</span></p>
          <div class="tags">
            <van-tag v-for="tag in (room.labelInfoList || []).slice(0, 3)" :key="tag.id" plain type="primary" style="margin-right:3px;">
              {{ tag.name }}
            </van-tag>
          </div>
        </div>
      </van-col>
    </van-row>
  </div>
</template>

<script setup lang="ts">
import type { RoomItemVo } from '@/types/api'

defineProps<{
  room: RoomItemVo
}>()

defineEmits<{
  click: []
}>()
</script>

<style scoped>
.room-card { padding: 10px 15px; background: #fff; border-bottom: 1px solid #f0f0f0; }
.room-info h3 { font-size: 15px; margin-bottom: 3px; }
.apt-name { color: #999; font-size: 12px; margin-bottom: 3px; }
.rent { font-size: 16px; color: #ee0a24; font-weight: bold; }
.rent span { font-size: 12px; font-weight: normal; color: #999; }
.tags { margin-top: 5px; }
</style>
