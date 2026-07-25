<template>
  <div class="apartment-list">
    <van-nav-bar title="找房" fixed />
    <div class="content">
      <van-search v-model="searchKeyword" placeholder="搜索公寓" shape="round" />

      <!-- 省市区三级联动选择 -->
      <div class="region-bar">
        <van-field
          v-model="provinceText"
          readonly
          is-link
          placeholder="全部省份"
          clearable
          @click="showProvincePicker = true"
          @clear="clearProvince"
        />
        <van-field
          v-model="cityText"
          readonly
          is-link
          placeholder="全部城市"
          :disabled="!selectedProvinceId"
          clearable
          @click="showCityPicker = true"
          @clear="clearCity"
        />
        <van-field
          v-model="districtText"
          readonly
          is-link
          placeholder="全部区县"
          :disabled="!selectedCityId"
          clearable
          @click="showDistrictPicker = true"
          @clear="clearDistrict"
        />
      </div>

      <!-- 省份选择器 -->
      <van-popup v-model:show="showProvincePicker" position="bottom" round>
        <van-picker
          :columns="provinceColumns"
          value-key="name"
          title="选择省份"
          @confirm="onProvinceConfirm"
          @cancel="showProvincePicker = false"
        />
      </van-popup>

      <!-- 城市选择器 -->
      <van-popup v-model:show="showCityPicker" position="bottom" round>
        <van-picker
          :columns="cityColumns"
          value-key="name"
          title="选择城市"
          @confirm="onCityConfirm"
          @cancel="showCityPicker = false"
        />
      </van-popup>

      <!-- 区县选择器 -->
      <van-popup v-model:show="showDistrictPicker" position="bottom" round>
        <van-picker
          :columns="districtColumns"
          value-key="name"
          title="选择区县"
          @confirm="onDistrictConfirm"
          @cancel="showDistrictPicker = false"
        />
      </van-popup>

      <!-- 排序 -->
      <van-dropdown-menu>
        <van-dropdown-item v-model="filters.sortType" :options="sortOptions" @change="onSortChange" />
      </van-dropdown-menu>

      <!-- 房间列表 -->
      <van-list v-model:loading="loading" :finished="finished" @load="onLoad">
        <room-card v-for="item in rooms" :key="item.id" :room="item" @click="goRoom(item.id)" />
      </van-list>
      <van-empty v-if="!loading && rooms.length === 0" description="暂无房源" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getRoomPage } from '@/api/room'
import { listProvince, listCityByProvinceId, listDistrictByCityId } from '@/api/region'
import type { RoomItemVo, ProvinceInfo, CityInfo, DistrictInfo } from '@/types/api'
import RoomCard from '@/components/RoomCard.vue'

const router = useRouter()
const searchKeyword = ref('')
const rooms = ref<RoomItemVo[]>([])
const loading = ref(false)
const finished = ref(false)
const current = ref(1)

// 省市区原始数据
const provinceList = ref<ProvinceInfo[]>([])
const cityList = ref<CityInfo[]>([])
const districtList = ref<DistrictInfo[]>([])

// 选择器列（包含"全部"选项）
const provinceColumns = ref<(ProvinceInfo | { name: string })[]>([])
const cityColumns = ref<(CityInfo | { name: string })[]>([])
const districtColumns = ref<(DistrictInfo | { name: string })[]>([])

// 当前选中值
const selectedProvinceId = ref<number | null>(null)
const selectedCityId = ref<number | null>(null)
const selectedDistrictId = ref<number | null>(null)

// 显示文本
const provinceText = ref('')
const cityText = ref('')
const districtText = ref('')

// 选择器显示状态
const showProvincePicker = ref(false)
const showCityPicker = ref(false)
const showDistrictPicker = ref(false)

const sortOptions = [
  { text: '默认排序', value: '' },
  { text: '价格从低到高', value: 'asc' },
  { text: '价格从高到低', value: 'desc' }
]

const filters = ref({
  sortType: ''
})

// 加载省份列表
onMounted(async () => {
  try {
    const res = await listProvince()
    provinceList.value = res.data.data || []
    provinceColumns.value = [{ name: '全部省份' }, ...provinceList.value]
  } catch {}
})

// ---- 省份 ----
function clearProvince() {
  provinceText.value = ''
  selectedProvinceId.value = null
  selectedCityId.value = null
  selectedDistrictId.value = null
  cityText.value = ''
  districtText.value = ''
  cityList.value = []
  districtList.value = []
  cityColumns.value = [{ name: '全部城市' }]
  districtColumns.value = [{ name: '全部区县' }]
  resetAndReload()
}

async function onProvinceConfirm({ selectedOptions }: any) {
  const selected = selectedOptions[0]
  showProvincePicker.value = false
  if (!selected.id) {
    // 选了"全部省份"
    clearProvince()
    return
  }
  const p = selected as ProvinceInfo
  provinceText.value = p.name
  selectedProvinceId.value = p.id
  selectedCityId.value = null
  selectedDistrictId.value = null
  cityText.value = ''
  districtText.value = ''
  cityList.value = []
  districtList.value = []

  try {
    const res = await listCityByProvinceId(p.id)
    cityList.value = res.data.data || []
    cityColumns.value = [{ name: '全部城市' }, ...cityList.value]
    districtColumns.value = [{ name: '全部区县' }]
  } catch {}

  resetAndReload()
}

// ---- 城市 ----
function clearCity() {
  cityText.value = ''
  selectedCityId.value = null
  selectedDistrictId.value = null
  districtText.value = ''
  districtList.value = []
  districtColumns.value = [{ name: '全部区县' }]
  resetAndReload()
}

async function onCityConfirm({ selectedOptions }: any) {
  const selected = selectedOptions[0]
  showCityPicker.value = false
  if (!selected.id) {
    clearCity()
    return
  }
  const c = selected as CityInfo
  cityText.value = c.name
  selectedCityId.value = c.id
  selectedDistrictId.value = null
  districtText.value = ''
  districtList.value = []

  try {
    const res = await listDistrictByCityId(c.id)
    districtList.value = res.data.data || []
    districtColumns.value = [{ name: '全部区县' }, ...districtList.value]
  } catch {}

  resetAndReload()
}

// ---- 区县 ----
function clearDistrict() {
  districtText.value = ''
  selectedDistrictId.value = null
  resetAndReload()
}

async function onDistrictConfirm({ selectedOptions }: any) {
  const selected = selectedOptions[0]
  showDistrictPicker.value = false
  if (!selected.id) {
    clearDistrict()
    return
  }
  const d = selected as DistrictInfo
  districtText.value = d.name
  selectedDistrictId.value = d.id
  resetAndReload()
}

function resetAndReload() {
  current.value = 1
  rooms.value = []
  finished.value = false
}

function onSortChange() {
  resetAndReload()
}

async function onLoad() {
  loading.value = true
  try {
    const res = await getRoomPage(current.value, 10, {
      provinceId: selectedProvinceId.value || undefined,
      cityId: selectedCityId.value || undefined,
      districtId: selectedDistrictId.value || undefined,
      orderType: filters.value.sortType || undefined
    })
    const data = res.data.data
    if (data?.records) {
      rooms.value.push(...data.records)
      if (rooms.value.length >= (data.total || 0)) {
        finished.value = true
      }
      current.value++
    } else {
      finished.value = true
    }
  } catch {
    finished.value = true
  } finally {
    loading.value = false
  }
}

function goRoom(id: number) {
  router.push(`/room/${id}`)
}
</script>

<style scoped>
.apartment-list { padding-bottom: 50px; }
.content { padding-top: 46px; }
.region-bar { padding: 4px 0; background: #fff; }
</style>
