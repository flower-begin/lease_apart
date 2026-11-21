<template>
  <div>
    <!-- 顶部整屏提示条 -->
    <section class="hero">
      <div class="hero-inner">
        <h1 class="hero-title">找到一个像家的地方</h1>
        <p class="hero-sub">真实房源 · 在线预约 · 一键签约</p>

        <div class="hero-search">
          <el-input
            v-model.trim="keyword"
            placeholder="搜公寓名 / 房间号 / 地址，例如：望京"
            size="large"
            clearable
            @keyup.enter="onSearch"
            @clear="onSearch"
          >
            <template #append>
              <el-button type="primary" :icon="Search" @click="onSearch">搜索</el-button>
            </template>
          </el-input>
        </div>
      </div>
    </section>

    <div class="page-wrap">
      <!-- 热门公寓 -->
      <section v-if="apartments.length" class="block">
        <h2 class="section-title">热门公寓</h2>
        <div class="apartment-row">
          <div
            v-for="item in apartments"
            :key="item.id"
            class="apartment-item card"
            @click="router.push(`/apartment/${item.id}`)"
          >
            <div class="apartment-cover">
              <img v-if="item.graphVoList?.length" :src="item.graphVoList[0].url" :alt="item.name" />
              <div v-else class="apartment-cover-ph">暂无图片</div>
            </div>
            <div class="apartment-info">
              <p class="apartment-name">{{ item.name }}</p>
              <p class="apartment-addr">{{ addressOf(item) }}</p>
              <p class="apartment-price">
                <span class="price"><small>￥</small>{{ item.minRent }}<small> 起/月</small></span>
              </p>
            </div>
          </div>
        </div>
      </section>

      <!-- 筛选 -->
      <section class="block">
        <h2 class="section-title">筛选房源</h2>
        <div class="card card-pad filter-bar">
          <div class="filter-item">
            <span class="filter-label">地区</span>
            <el-cascader
              v-model="regionPath"
              :options="regionOptions"
              :props="{ checkStrictly: true, expandTrigger: 'hover' }"
              placeholder="省 / 市 / 区"
              clearable
              class="filter-control"
              @change="onRegionChange"
            />
          </div>

          <div class="filter-item">
            <span class="filter-label">租金</span>
            <el-select
              v-model="rentRange"
              placeholder="不限"
              clearable
              class="filter-control"
              @change="onRentChange"
            >
              <el-option
                v-for="item in rentOptions"
                :key="item.label"
                :label="item.label"
                :value="item.label"
              />
            </el-select>
          </div>

          <div class="filter-item">
            <span class="filter-label">付款方式</span>
            <el-select v-model="paymentTypeId" placeholder="不限" clearable class="filter-control" @change="reload">
              <el-option
                v-for="item in paymentTypes"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </div>

          <div class="filter-item">
            <span class="filter-label">排序</span>
            <el-select v-model="orderType" placeholder="综合排序" clearable class="filter-control" @change="reload">
              <el-option label="租金从低到高" value="asc" />
              <el-option label="租金从高到低" value="desc" />
            </el-select>
          </div>

          <div class="filter-actions">
            <el-button @click="reset">重置</el-button>
            <el-button type="primary" @click="reload">筛选</el-button>
          </div>
        </div>
      </section>

      <!-- 房源列表 -->
      <section class="block">
        <h2 class="section-title">
          房源列表
          <span class="muted total-text">共 {{ total }} 套</span>
        </h2>

        <div v-loading="loading" class="room-grid">
          <RoomCard v-for="room in rooms" :key="room.id" :room="room" />
        </div>

        <el-empty
          v-if="!loading && !rooms.length"
          description="没有符合条件的房源，换个筛选条件试试"
          class="empty-box"
        />

        <div v-if="total > size" class="pagination-bar">
          <el-pagination
            v-model:current-page="current"
            :page-size="size"
            :total="total"
            background
            layout="prev, pager, next, total"
            @current-change="fetchRooms"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import RoomCard from '@/components/RoomCard.vue'
import { apartmentApi, buildRoomQuery, paymentApi, regionApi, roomApi } from '@/api'
import type {
  ApartmentItemVo,
  PaymentType,
  RegionNode,
  RoomItemVo,
  RoomQuery
} from '@/types/api'

const router = useRouter()

/* ------------------------------- 热门公寓 ------------------------------- */
const apartments = ref<ApartmentItemVo[]>([])

function addressOf(item: ApartmentItemVo) {
  return [item.cityName, item.districtName, item.addressDetail].filter(Boolean).join(' · ') || '地址待完善'
}

async function fetchApartments() {
  try {
    const res = await apartmentApi.list()
    apartments.value = res.data || []
  } catch {
    /* 忽略 */
  }
}

/* -------------------------------- 筛选条件 -------------------------------- */
const regionOptions = ref<{ value: number; label: string; children?: unknown[] }[]>([])
const regionPath = ref<number[]>([])
const paymentTypes = ref<PaymentType[]>([])

const rentRange = ref('')
const orderType = ref<'' | 'asc' | 'desc'>('')
const paymentTypeId = ref<number | ''>('')
const keyword = ref('')

const rentOptions = [
  { label: '2000 以下', min: '', max: 2000 },
  { label: '2000-4000', min: 2000, max: 4000 },
  { label: '4000-6000', min: 4000, max: 6000 },
  { label: '6000-10000', min: 6000, max: 10000 },
  { label: '10000 以上', min: 10000, max: '' }
]

const query = ref<RoomQuery>({})

function onRegionChange(value: unknown) {
  // el-cascader 的 change 值是 (string|number)[] | null，这里统一按 id 数组处理
  const path = Array.isArray(value) ? (value as (string | number)[]) : []
  const [provinceId, cityId, districtId] = path
  query.value = {
    ...query.value,
    provinceId: (provinceId as number) || '',
    cityId: (cityId as number) || '',
    districtId: (districtId as number) || ''
  }
  reload()
}

function onRentChange(label: string) {
  const hit = rentOptions.find(item => item.label === label)
  query.value = {
    ...query.value,
    minRent: hit ? hit.min : '',
    maxRent: hit ? hit.max : ''
  }
  reload()
}

function reset() {
  regionPath.value = []
  rentRange.value = ''
  orderType.value = ''
  paymentTypeId.value = ''
  keyword.value = ''
  query.value = {}
  reload()
}

function onSearch() {
  reload()
}

/* -------------------------------- 房源列表 -------------------------------- */
const rooms = ref<RoomItemVo[]>([])
const current = ref(1)
const size = ref(9)
const total = ref(0)
const loading = ref(false)

async function fetchRooms() {
  loading.value = true
  try {
    const params = {
      current: current.value,
      size: size.value,
      ...buildRoomQuery({
        ...query.value,
        orderType: orderType.value,
        paymentTypeId: paymentTypeId.value,
        keyword: keyword.value
      })
    }
    const res = await roomApi.page(params)
    rooms.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } catch {
    rooms.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function reload() {
  current.value = 1
  fetchRooms()
}

/* -------------------------------- 初始化 -------------------------------- */
onMounted(async () => {
  fetchApartments()
  fetchRooms()

  try {
    const res = await regionApi.tree()
    regionOptions.value = (res.data as RegionNode[]).map(province => ({
      value: province.id,
      label: province.name,
      children: (province.children || []).map(city => ({
        value: city.id,
        label: city.name,
        children: (city.children || []).map(district => ({
          value: district.id,
          label: district.name
        }))
      }))
    }))
  } catch {
    regionOptions.value = []
  }

  try {
    const res = await paymentApi.list()
    paymentTypes.value = res.data || []
  } catch {
    paymentTypes.value = []
  }
})
</script>

<style scoped>
/* ---------------- 顶部 ---------------- */
.hero {
  background: linear-gradient(120deg, #7a6350 0%, #c3a181 55%, #f2cc8f 140%);
  color: #fff;
}

.hero-inner {
  max-width: var(--page-width);
  margin: 0 auto;
  padding: 52px 20px 56px;
  text-align: center;
}

.hero-title {
  margin: 0;
  font-size: 34px;
  font-weight: 700;
  letter-spacing: 3px;
}

.hero-sub {
  margin: 12px 0 26px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  letter-spacing: 2px;
}

.hero-search {
  max-width: 620px;
  margin: 0 auto;
}

/* ---------------- 区块 ---------------- */
.block {
  margin-bottom: 28px;
}

.total-text {
  font-size: 13px;
  font-weight: 400;
}

/* ---------------- 热门公寓 ---------------- */
.apartment-row {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.apartment-item {
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.apartment-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.1);
}

.apartment-cover {
  height: 100px;
  background: #f3efea;
}

.apartment-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.apartment-cover-ph {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--text-3);
}

.apartment-info {
  padding: 10px 12px 12px;
}

.apartment-name {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.apartment-addr {
  margin: 4px 0 8px;
  font-size: 12px;
  color: var(--text-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.apartment-price {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--price);
}

.apartment-price small {
  font-size: 12px;
  font-weight: 400;
}

/* ---------------- 筛选条 ---------------- */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px 20px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  color: var(--text-2);
  white-space: nowrap;
}

.filter-control {
  width: 210px;
}

.filter-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

/* ---------------- 房源网格 ---------------- */
.room-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  min-height: 80px;
}

.pagination-bar {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

@media (max-width: 1100px) {
  .apartment-row {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .room-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .apartment-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .room-grid {
    grid-template-columns: 1fr;
  }
  .filter-control {
    width: 100%;
  }
  .hero-title {
    font-size: 24px;
  }
}
</style>
