<template>
  <div class="page-wrap">
    <div class="head-row">
      <h2 class="section-title">我的预约</h2>
      <el-button type="primary" round @click="router.push('/search')">去预约看房</el-button>
    </div>

    <div v-loading="loading" class="list">
      <div v-for="item in list" :key="item.id" class="card item">
        <div class="cover">
          <img v-if="item.graphVoList?.length" :src="item.graphVoList[0].url" :alt="item.apartmentName" />
          <div v-else class="cover-ph">暂无图片</div>
        </div>

        <div class="info">
          <div class="title-row">
            <h3 class="name">{{ item.apartmentName || '公寓' }}</h3>
            <el-tag :type="appointmentTag(item.appointmentStatus)" size="small">
              {{ APPOINTMENT_STATUS[item.appointmentStatus] || '未知' }}
            </el-tag>
          </div>
          <p class="meta">
            <el-icon><Clock /></el-icon>
            看房时间：{{ dateTime(item.appointmentTime) }}
          </p>
          <p class="meta muted">预约单号：#{{ item.id }}</p>
        </div>

        <div class="actions">
          <el-button size="small" @click="openDetail(item.id)">查看详情</el-button>
          <el-button
            v-if="item.appointmentStatus === 1"
            size="small"
            type="danger"
            plain
            @click="onCancel(item.id)"
          >
            取消预约
          </el-button>
        </div>
      </div>
    </div>

    <el-empty
      v-if="!loading && !list.length"
      description="还没有预约记录，去找找心仪的房子吧"
      class="empty-box"
    />

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="预约详情" width="560px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="预约单号">#{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="公寓">{{ detail.apartmentName }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ detail.apartmentAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone }}</el-descriptions-item>
        <el-descriptions-item label="看房时间">
          {{ dateTime(detail.appointmentTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="appointmentTag(detail.appointmentStatus)" size="small">
            {{ APPOINTMENT_STATUS[detail.appointmentStatus] || '未知' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注">
          {{ detail.additionalInfo || '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="detail && detail.appointmentStatus === 1"
          type="danger"
          plain
          @click="onCancel(detail.id)"
        >
          取消预约
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock } from '@element-plus/icons-vue'
import { appointmentApi } from '@/api'
import type { AppointmentDetailVo, AppointmentItemVo } from '@/types/api'
import { APPOINTMENT_STATUS, appointmentTag, dateTime } from '@/utils/format'

const router = useRouter()

const list = ref<AppointmentItemVo[]>([])
const loading = ref(false)
const detail = ref<AppointmentDetailVo | null>(null)
const detailVisible = ref(false)

async function fetchList() {
  loading.value = true
  try {
    const res = await appointmentApi.list()
    list.value = res.data || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function openDetail(id: number) {
  try {
    const res = await appointmentApi.detail(id)
    detail.value = res.data
    detailVisible.value = true
  } catch {
    /* 拦截器已提示 */
  }
}

async function onCancel(id: number) {
  try {
    await ElMessageBox.confirm('确定要取消这次预约吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await appointmentApi.cancel(id)
    ElMessage.success('已取消预约')
    detailVisible.value = false
    fetchList()
  } catch {
    /* 拦截器已提示 */
  }
}

onMounted(fetchList)
</script>

<style scoped>
.head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.head-row .section-title {
  margin: 0;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 60px;
}

.item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
}

.cover {
  width: 140px;
  height: 96px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  background: #f3efea;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-ph {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--text-3);
}

.info {
  flex: 1;
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 6px;
  font-size: 13px;
  color: var(--text-2);
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
}
</style>
