<template>
  <div v-loading="loading" class="page-wrap">
    <template v-if="detail">
      <!-- 图片 -->
      <div class="card gallery">
        <el-carousel v-if="images.length" height="380px" :interval="4000" indicator-position="outside">
          <el-carousel-item v-for="(img, i) in images" :key="i">
            <img :src="img.url" class="gallery-img" :alt="img.name || detail.name" />
          </el-carousel-item>
        </el-carousel>
        <div v-else class="gallery-empty">暂无图片</div>
      </div>

      <div class="detail-layout">
        <!-- 左侧主体 -->
        <div class="main-col">
          <div class="card card-pad">
            <h1 class="detail-title">{{ detail.name }}</h1>
            <p class="detail-addr">
              <el-icon><Location /></el-icon>
              {{ address }}
            </p>
            <p class="detail-phone">
              <el-icon><Phone /></el-icon>
              联系电话：{{ detail.phone || '暂无' }}
            </p>

            <div class="tag-row">
              <el-tag
                v-for="label in detail.labelInfoList || []"
                :key="label.id"
                type="success"
                effect="plain"
              >
                {{ label.name }}
              </el-tag>
              <span v-if="!(detail.labelInfoList || []).length" class="muted">暂无标签</span>
            </div>

            <p v-if="detail.introduction" class="detail-intro">{{ detail.introduction }}</p>
          </div>

          <div class="card card-pad">
            <h2 class="section-title">公寓配套</h2>
            <div class="facility-row">
              <span
                v-for="item in detail.facilityInfoList || []"
                :key="item.id"
                class="facility-item"
              >
                <el-icon><CircleCheck /></el-icon>
                {{ item.name }}
              </span>
              <span v-if="!(detail.facilityInfoList || []).length" class="muted">暂无配套信息</span>
            </div>
          </div>

          <div class="card card-pad">
            <h2 class="section-title">
              在租房源
              <span class="muted count-text">共 {{ roomTotal }} 间</span>
            </h2>
            <div v-loading="roomLoading" class="room-grid">
              <RoomCard v-for="room in rooms" :key="room.id" :room="room" />
            </div>
            <el-empty
              v-if="!roomLoading && !rooms.length"
              description="该公寓暂无可租房间"
              class="empty-box"
            />
            <div v-if="roomTotal > roomSize" class="pagination-bar">
              <el-pagination
                v-model:current-page="roomCurrent"
                :page-size="roomSize"
                :total="roomTotal"
                background
                layout="prev, pager, next, total"
                @current-change="fetchRooms"
              />
            </div>
          </div>
        </div>

        <!-- 右侧操作卡 -->
        <aside class="side-col">
          <div class="card card-pad side-card">
            <p class="side-label">最低租金</p>
            <p class="side-price"><small>￥</small>{{ detail.minRent }}<small> /月起</small></p>
            <el-button type="primary" size="large" class="side-btn" @click="goAppointment">
              预约看房
            </el-button>
            <el-button size="large" class="side-btn" @click="router.push('/search')">
              再看看别的
            </el-button>
          </div>
        </aside>
      </div>
    </template>

    <el-empty v-else-if="!loading" description="公寓不存在" class="empty-box" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CircleCheck, Location, Phone } from '@element-plus/icons-vue'
import RoomCard from '@/components/RoomCard.vue'
import { apartmentApi, roomApi } from '@/api'
import type { ApartmentDetailVo, RoomItemVo } from '@/types/api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref<ApartmentDetailVo | null>(null)

const images = computed(() => detail.value?.graphVoList || [])

const address = computed(() => {
  const d = detail.value
  if (!d) return ''
  return [d.provinceName, d.cityName, d.districtName, d.addressDetail].filter(Boolean).join(' ')
})

/* ------------------------------- 房间列表 ------------------------------- */
const rooms = ref<RoomItemVo[]>([])
const roomCurrent = ref(1)
const roomSize = ref(6)
const roomTotal = ref(0)
const roomLoading = ref(false)

async function fetchRooms() {
  const id = Number(route.params.id)
  if (!id) return
  roomLoading.value = true
  try {
    const res = await roomApi.pageByApartment({
      current: roomCurrent.value,
      size: roomSize.value,
      id
    })
    rooms.value = res.data?.records || []
    roomTotal.value = Number(res.data?.total || 0)
  } catch {
    rooms.value = []
  } finally {
    roomLoading.value = false
  }
}

/* -------------------------------- 动作 -------------------------------- */
function goAppointment() {
  router.push({ path: '/appointment/create', query: { apartmentId: String(route.params.id) } })
}

async function fetchDetail() {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    const res = await apartmentApi.detail(id)
    detail.value = res.data
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDetail()
  fetchRooms()
})
</script>

<style scoped>
.gallery {
  overflow: hidden;
  margin-bottom: 18px;
}

.gallery-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.gallery-empty {
  height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-3);
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 18px;
  align-items: start;
}

.main-col {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-title {
  margin: 0 0 12px;
  font-size: 24px;
  font-weight: 700;
}

.detail-addr,
.detail-phone {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--text-2);
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 14px 0 0;
}

.detail-intro {
  margin: 16px 0 0;
  font-size: 14px;
  line-height: 1.9;
  color: var(--text-2);
  white-space: pre-wrap;
}

.facility-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 22px;
}

.facility-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-2);
}

.facility-item .el-icon {
  color: var(--brand);
}

.count-text {
  font-size: 13px;
  font-weight: 400;
}

.room-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  min-height: 60px;
}

.pagination-bar {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

/* 右侧 */
.side-col {
  position: sticky;
  top: 84px;
}

.side-card {
  text-align: center;
}

.side-label {
  margin: 0;
  font-size: 13px;
  color: var(--text-3);
}

.side-price {
  margin: 6px 0 18px;
  font-size: 28px;
  font-weight: 700;
  color: var(--price);
}

.side-price small {
  font-size: 13px;
  font-weight: 400;
}

.side-btn {
  width: 100%;
  margin: 0 0 10px !important;
}

@media (max-width: 1000px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
  .side-col {
    position: static;
  }
  .room-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
