<template>
  <div class="home">
    <van-nav-bar title="租赁平台" fixed />
    <div class="content">
      <van-search v-model="searchText" placeholder="搜索公寓、房间" shape="round" />
      <van-swipe :autoplay="3000" class="banner">
        <van-swipe-item v-for="(img, i) in banners" :key="i">
          <div class="banner-img" :style="{ backgroundColor: img.color }">{{ img.text }}</div>
        </van-swipe-item>
      </van-swipe>
      <div class="section-title">热门公寓</div>
      <van-grid :column-num="2" :gutter="10">
        <van-grid-item v-for="item in hotApartments" :key="item.id" @click="goDetail(item.id)">
          <van-image :src="item.image" width="100%" height="120" fit="cover" />
          <div class="grid-name">{{ item.name }}</div>
          <div class="grid-price">¥{{ item.minRent }}/月起</div>
        </van-grid-item>
      </van-grid>
      <div class="section-title">快速入口</div>
      <van-grid :column-num="3">
        <van-grid-item icon="clock-o" text="浏览历史" to="/profile" />
        <van-grid-item icon="calendar-o" text="我的预约" to="/appointments" />
        <van-grid-item icon="description-o" text="我的租约" to="/agreements" />
      </van-grid>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getRoomPage } from '@/api/room'
const router = useRouter()
const searchText = ref('')

const banners = [
  { color: '#1989fa', text: '品质好房 安心租住' },
  { color: '#07c160', text: '海量房源 即刻预约' },
  { color: '#ee0a24', text: 'AI助手 智能推荐' }
]

interface HotApartment {
  id: number
  name: string
  image: string
  minRent: number
}

const hotApartments = ref<HotApartment[]>([])

onMounted(async () => {
  try {
    const res = await getRoomPage(1, 4, { orderType: 'desc' })
    const rooms = res.data.data?.records || []
    const aptMap = new Map<number, HotApartment>()
    for (const room of rooms) {
      if (aptMap.size >= 6) break
      const apt = room.apartmentInfo
      if (apt && !aptMap.has(apt.id)) {
        aptMap.set(apt.id, {
          ...apt,
          image: room.graphVoList?.[0]?.url || 'https://via.placeholder.com/300x200?text=Apartment',
          minRent: room.rent
        })
      }
    }
    hotApartments.value = Array.from(aptMap.values())
  } catch {
    showToast('加载失败')
  }
})

function goDetail(id: number) {
  router.push(`/apartment/${id}`)
}
</script>

<style scoped>
.home { padding-bottom: 50px; }
.content { padding-top: 46px; }
.banner { margin: 10px 15px; border-radius: 8px; overflow: hidden; }
.banner-img { height: 160px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 22px; font-weight: bold; }
.section-title { font-size: 16px; font-weight: bold; padding: 15px 15px 10px; }
.grid-name { font-size: 13px; margin-top: 5px; text-align: center; }
.grid-price { font-size: 12px; color: #ee0a24; margin-top: 3px; text-align: center; }
</style>
