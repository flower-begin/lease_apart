<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-toolbar">
        <el-input v-model="query.name" placeholder="公寓名称" clearable style="width: 180px" @keyup.enter="search" />
        <el-select v-model="query.provinceId" placeholder="省份" clearable style="width: 140px" @change="onProvinceChange">
          <el-option v-for="p in provinces" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-select
          v-model="query.cityId"
          placeholder="城市"
          clearable
          style="width: 140px"
          :disabled="!query.provinceId"
          @change="onCityChange"
        >
          <el-option v-for="c in cities" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="query.districtId" placeholder="区县" clearable style="width: 140px" :disabled="!query.cityId">
          <el-option v-for="d in districts" :key="d.id" :label="d.name" :value="d.id" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <div class="spacer"></div>
        <el-button type="primary" :icon="Plus" @click="openForm()">新增公寓</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column label="封面" width="86" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.cover"
              :src="resolveUrl(row.cover)"
              :preview-src-list="[resolveUrl(row.cover)]"
              preview-teleported
              class="thumb"
              fit="cover"
            />
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="公寓名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="位置" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.provinceName || '' }}{{ row.cityName || '' }}{{ row.districtName || '' }}{{ row.addressDetail || '' }}
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column label="房间" width="120" align="center">
          <template #default="{ row }">
            <span class="text-success">{{ row.freeRoomCount ?? 0 }}</span>
            <span class="text-muted"> / {{ row.totalRoomCount ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="dictType(RELEASE_STATUS, row.isRelease)" size="small">
              {{ dictLabel(RELEASE_STATUS, row.isRelease) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row.id)">编辑</el-button>
            <el-button link type="primary" @click="toggleRelease(row)">
              {{ Number(row.isRelease) === 1 ? '下架' : '发布' }}
            </el-button>
            <el-button link type="primary" @click="goRooms(row.id)">房间</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <apartment-form ref="formRef" @saved="load" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { apartmentApi, regionApi } from '@/api'
import { RELEASE_STATUS, dictLabel, dictType } from '@/utils/dict'
import ApartmentForm from './components/ApartmentForm.vue'
import type { ApartmentItemVo, CityInfo, DistrictInfo, ProvinceInfo } from '@/types/api'

const router = useRouter()

/** 后端返回的是 /upload/xxx.jpg 这种相对路径，绝对地址原样用 */
function resolveUrl(url?: string) {
  if (!url) return ''
  if (/^(https?:)?\/\//i.test(url) || url.startsWith('data:')) return url
  return url.startsWith('/') ? url : `/${url}`
}

const loading = ref(false)
const total = ref(0)
const rows = ref<ApartmentItemVo[]>([])
const formRef = ref<InstanceType<typeof ApartmentForm>>()

const provinces = ref<ProvinceInfo[]>([])
const cities = ref<CityInfo[]>([])
const districts = ref<DistrictInfo[]>([])

const query = reactive({
  current: 1,
  size: 10,
  name: '',
  provinceId: undefined as number | undefined,
  cityId: undefined as number | undefined,
  districtId: undefined as number | undefined
})

onMounted(async () => {
  await loadRegions()
  await load()
})

async function loadRegions() {
  try {
    const res = await regionApi.provinces()
    provinces.value = res.data.data || []
  } catch {
    /* 拦截器已提示 */
  }
}

async function onProvinceChange() {
  query.cityId = undefined
  query.districtId = undefined
  cities.value = []
  districts.value = []
  if (!query.provinceId) return
  try {
    const res = await regionApi.cities(query.provinceId)
    cities.value = res.data.data || []
  } catch {
    /* ignore */
  }
}

async function onCityChange() {
  query.districtId = undefined
  districts.value = []
  if (!query.cityId) return
  try {
    const res = await regionApi.districts(query.cityId)
    districts.value = res.data.data || []
  } catch {
    /* ignore */
  }
}

async function load() {
  loading.value = true
  try {
    const res = await apartmentApi.page({ ...query })
    const data = res.data.data
    rows.value = data?.records || []
    total.value = data?.total || 0
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.current = 1
  load()
}

function reset() {
  query.current = 1
  query.name = ''
  query.provinceId = undefined
  query.cityId = undefined
  query.districtId = undefined
  cities.value = []
  districts.value = []
  load()
}

function openForm(id?: number) {
  formRef.value?.open(id)
}

function goRooms(apartmentId?: number) {
  if (apartmentId === undefined) return
  router.push({ path: '/room', query: { apartmentId } })
}

async function toggleRelease(row: ApartmentItemVo) {
  const next = Number(row.isRelease) === 1 ? 0 : 1
  try {
    await apartmentApi.updateRelease(row.id!, next)
    ElMessage.success(next === 1 ? '已发布' : '已下架')
    load()
  } catch {
    /* 拦截器已提示 */
  }
}

async function remove(row: ApartmentItemVo) {
  try {
    await ElMessageBox.confirm(
      `确定删除公寓「${row.name}」吗？若该公寓下还有房间，需要先删除房间。`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await apartmentApi.remove(row.id!)
    ElMessage.success('已删除')
    load()
  } catch {
    /* 拦截器已提示 */
  }
}

defineExpose({ load })
</script>
