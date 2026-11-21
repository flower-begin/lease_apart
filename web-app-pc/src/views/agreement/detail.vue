<template>
  <div v-loading="loading" class="page-wrap">
    <template v-if="detail">
      <div class="head-row">
        <h2 class="section-title">租约详情</h2>
        <el-tag :type="leaseTag(detail.status)" size="large">
          {{ LEASE_STATUS[detail.status as number] || '未知' }}
        </el-tag>
      </div>

      <div class="detail-layout">
        <div class="main-col">
          <div class="card card-pad">
            <h3 class="house-title">{{ detail.apartmentName }} {{ detail.roomNumber }}</h3>

            <div class="image-row">
              <img
                v-for="(img, i) in roomImages"
                :key="i"
                :src="img.url"
                class="thumb"
                :alt="detail.roomNumber"
              />
              <span v-if="!roomImages.length" class="muted">暂无房间图片</span>
            </div>

            <el-divider />

            <el-descriptions :column="2" border>
              <el-descriptions-item label="承租人">{{ detail.name }}</el-descriptions-item>
              <el-descriptions-item label="手机号">{{ detail.phone || '-' }}</el-descriptions-item>
              <el-descriptions-item label="身份证号">
                {{ detail.identificationNumber || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="租约来源">
                {{ LEASE_SOURCE[detail.sourceType as number] || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="起租日期">
                {{ dateOnly(detail.leaseStartDate) }}
              </el-descriptions-item>
              <el-descriptions-item label="到期日期">
                {{ dateOnly(detail.leaseEndDate) }}
              </el-descriptions-item>
              <el-descriptions-item label="租期">
                {{ termText(detail.leaseTermMonthCount, detail.leaseTermUnit) }}
              </el-descriptions-item>
              <el-descriptions-item label="支付方式">
                {{ detail.paymentTypeName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="月租金">
                <span class="price">{{ money(detail.rent) }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="押金">
                <span class="price">{{ money(detail.deposit) }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">
                {{ detail.additionalInfo || '-' }}
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div v-if="apartmentImages.length" class="card card-pad">
            <h2 class="section-title">公寓图片</h2>
            <div class="image-row">
              <img
                v-for="(img, i) in apartmentImages"
                :key="i"
                :src="img.url"
                class="thumb"
                :alt="detail.apartmentName"
              />
            </div>
          </div>
        </div>

        <aside class="side-col">
          <div class="card card-pad side-card">
            <p class="side-label">当前状态</p>
            <p class="side-status">
              <el-tag :type="leaseTag(detail.status)" size="large">
                {{ LEASE_STATUS[detail.status as number] || '未知' }}
              </el-tag>
            </p>

            <el-button
              v-if="detail.status === 1"
              type="danger"
              plain
              class="side-btn"
              @click="changeStatus(3, '确定取消签约吗？')"
            >
              取消签约
            </el-button>

            <el-button
              v-if="detail.status === 2"
              type="warning"
              plain
              class="side-btn"
              @click="changeStatus(5, '确定申请退租吗？')"
            >
              申请退租
            </el-button>

            <el-button
              v-if="detail.status === 5"
              type="success"
              plain
              class="side-btn"
              @click="changeStatus(2, '确定撤销退租申请吗？')"
            >
              撤销退租
            </el-button>

            <el-button
              v-if="detail.status === 2"
              type="primary"
              class="side-btn"
              @click="router.push({ path: '/agreement/sign', query: { renewId: String(detail.id) } })"
            >
              续约
            </el-button>

            <el-button class="side-btn" @click="router.push('/agreement/mine')">返回列表</el-button>
          </div>
        </aside>
      </div>
    </template>

    <el-empty v-else-if="!loading" description="租约不存在或无权查看" class="empty-box" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { agreementApi } from '@/api'
import type { AgreementDetailVo } from '@/types/api'
import { LEASE_SOURCE, LEASE_STATUS, dateOnly, leaseTag, money, termText } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref<AgreementDetailVo | null>(null)

const roomImages = computed(() => detail.value?.roomGraphVoList || [])
const apartmentImages = computed(() => detail.value?.apartmentGraphVoList || [])

async function fetchDetail() {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    const res = await agreementApi.detail(id)
    detail.value = res.data
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function changeStatus(leaseStatus: number, tip: string) {
  if (!detail.value) return
  try {
    await ElMessageBox.confirm(tip, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await agreementApi.updateStatus(detail.value.id!, leaseStatus)
    ElMessage.success('操作成功')
    fetchDetail()
  } catch {
    /* 拦截器已提示 */
  }
}

onMounted(fetchDetail)
</script>

<style scoped>
.head-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}

.head-row .section-title {
  margin: 0;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 18px;
  align-items: start;
}

.main-col {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.house-title {
  margin: 0 0 16px;
  font-size: 20px;
  font-weight: 700;
}

.image-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.thumb {
  width: 150px;
  height: 105px;
  object-fit: cover;
  border-radius: 8px;
  background: #f3efea;
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

.side-status {
  margin: 10px 0 18px;
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
