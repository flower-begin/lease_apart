<template>
  <div v-loading="loading" class="page-wrap">
    <template v-if="detail">
      <div class="detail-layout">
        <div class="main-col">
          <!-- 图片 -->
          <div class="card gallery">
            <el-carousel v-if="images.length" height="380px" :interval="4000">
              <el-carousel-item v-for="(img, i) in images" :key="i">
                <img :src="img.url" class="gallery-img" :alt="img.name || detail.roomNumber" />
              </el-carousel-item>
            </el-carousel>
            <div v-else class="gallery-empty">暂无图片</div>
          </div>

          <!-- 基本信息 -->
          <div class="card card-pad">
            <div class="title-row">
              <h1 class="detail-title">
                {{ detail.apartmentItemVo?.name }} {{ detail.roomNumber }}
              </h1>
              <el-tag v-if="detail.isCheckIn" type="info" effect="dark">已出租</el-tag>
              <el-tag v-else type="success" effect="dark">可租</el-tag>
            </div>

            <p class="detail-addr">
              <el-icon><Location /></el-icon>
              {{ address }}
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
            </div>

            <el-divider />

            <!-- 房间属性 -->
            <h2 class="section-title">房间属性</h2>
            <el-descriptions :column="3" border>
              <el-descriptions-item
                v-for="attr in detail.attrValueVoList || []"
                :key="attr.id"
                :label="attr.attrKeyName"
              >
                {{ attr.name }}
              </el-descriptions-item>
            </el-descriptions>
            <p v-if="!(detail.attrValueVoList || []).length" class="muted">暂无属性信息</p>

            <el-divider />

            <!-- 配套 -->
            <h2 class="section-title">房间配套</h2>
            <div class="facility-row">
              <span v-for="item in detail.facilityInfoList || []" :key="item.id" class="facility-item">
                <el-icon><CircleCheck /></el-icon>
                {{ item.name }}
              </span>
              <span v-if="!(detail.facilityInfoList || []).length" class="muted">暂无配套信息</span>
            </div>
          </div>

          <!-- 费用与支付 -->
          <div class="card card-pad">
            <h2 class="section-title">杂费明细</h2>
            <el-table :data="detail.feeValueVoList || []" border size="small">
              <el-table-column prop="feeKeyName" label="费用项目" width="160" />
              <el-table-column prop="name" label="说明" />
              <el-table-column label="金额" width="160">
                <template #default="{ row }">
                  {{ row.amount }} 元 / {{ row.unit || '次' }}
                </template>
              </el-table-column>
            </el-table>
            <p v-if="!(detail.feeValueVoList || []).length" class="muted">暂无杂费</p>

            <h2 class="section-title" style="margin-top: 22px">支付方式</h2>
            <div class="option-row">
              <el-tag
                v-for="item in detail.paymentTypeList || []"
                :key="item.id"
                type="primary"
                effect="plain"
                size="large"
              >
                {{ paymentText(item.name, item.payMonthCount) }}
              </el-tag>
              <span v-if="!(detail.paymentTypeList || []).length" class="muted">暂无</span>
            </div>

            <h2 class="section-title" style="margin-top: 22px">可租期限</h2>
            <div class="option-row">
              <el-tag
                v-for="item in detail.leaseTermList || []"
                :key="item.id"
                effect="plain"
                size="large"
              >
                {{ termText(item.monthCount, item.unit) }}
              </el-tag>
              <span v-if="!(detail.leaseTermList || []).length" class="muted">暂无</span>
            </div>
          </div>
        </div>

        <!-- 右侧操作 -->
        <aside class="side-col">
          <div class="card card-pad side-card">
            <p class="side-label">租金</p>
            <p class="side-price"><small>￥</small>{{ detail.rent }}<small> /月</small></p>

            <el-button
              type="primary"
              size="large"
              class="side-btn"
              :disabled="detail.isCheckIn"
              @click="goSign"
            >
              {{ detail.isCheckIn ? '该房间已出租' : '立即签约' }}
            </el-button>

            <el-button size="large" class="side-btn" @click="goAppointment">预约看房</el-button>

            <el-button
              v-if="detail.apartmentItemVo"
              link
              type="primary"
              @click="router.push(`/apartment/${detail.apartmentItemVo.id}`)"
            >
              查看所属公寓 →
            </el-button>
          </div>
        </aside>
      </div>
    </template>

    <el-empty v-else-if="!loading" description="房间不存在" class="empty-box" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CircleCheck, Location } from '@element-plus/icons-vue'
import { roomApi } from '@/api'
import type { RoomDetailVo } from '@/types/api'
import { paymentText, termText } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref<RoomDetailVo | null>(null)

const images = computed(() => detail.value?.graphVoList || [])

const address = computed(() => {
  const a = detail.value?.apartmentItemVo
  if (!a) return ''
  return [a.provinceName, a.cityName, a.districtName, a.addressDetail].filter(Boolean).join(' ')
})

function goAppointment() {
  router.push({
    path: '/appointment/create',
    query: {
      apartmentId: String(detail.value?.apartmentId || ''),
      roomId: String(detail.value?.id || '')
    }
  })
}

function goSign() {
  router.push({ path: '/agreement/sign', query: { roomId: String(detail.value?.id || '') } })
}

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    const res = await roomApi.detail(id)
    detail.value = res.data
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
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

.gallery {
  overflow: hidden;
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

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.detail-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
}

.detail-addr {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 14px;
  font-size: 13px;
  color: var(--text-2);
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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

.option-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

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
}
</style>
