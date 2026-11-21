<template>
  <div class="page-wrap">
    <div class="head-row">
      <h2 class="section-title">我的租约</h2>
      <el-radio-group v-model="filter" size="default">
        <el-radio-button :value="0">全部</el-radio-button>
        <el-radio-button :value="1">签约待确认</el-radio-button>
        <el-radio-button :value="2">已签约</el-radio-button>
        <el-radio-button :value="7">续约待确认</el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading" class="list">
      <div v-for="item in filtered" :key="item.id" class="card item">
        <div class="cover">
          <img v-if="item.roomGraphVoList?.length" :src="item.roomGraphVoList[0].url" :alt="item.roomNumber" />
          <div v-else class="cover-ph">暂无图片</div>
        </div>

        <div class="info">
          <div class="title-row">
            <h3 class="name">{{ item.apartmentName }} {{ item.roomNumber }}</h3>
            <el-tag :type="leaseTag(item.leaseStatus)" size="small">
              {{ LEASE_STATUS[item.leaseStatus] || '未知' }}
            </el-tag>
            <el-tag v-if="item.sourceType === 2" type="warning" effect="plain" size="small">
              {{ LEASE_SOURCE[item.sourceType] }}
            </el-tag>
          </div>

          <p class="meta">
            <el-icon><Calendar /></el-icon>
            租期：{{ dateOnly(item.leaseStartDate) }} ~ {{ dateOnly(item.leaseEndDate) }}
          </p>
          <p class="meta">
            <el-icon><Money /></el-icon>
            租金：<span class="price">{{ money(item.rent) }}</span> /月
          </p>
        </div>

        <div class="actions">
          <el-button size="small" @click="router.push(`/agreement/${item.id}`)">查看详情</el-button>

          <el-button
            v-if="item.leaseStatus === 1"
            size="small"
            type="danger"
            plain
            @click="changeStatus(item.id, 3, '确定取消签约吗？')"
          >
            取消签约
          </el-button>

          <el-button
            v-if="item.leaseStatus === 2"
            size="small"
            type="warning"
            plain
            @click="changeStatus(item.id, 5, '确定申请退租吗？提交后需管家确认')"
          >
            申请退租
          </el-button>

          <el-button
            v-if="item.leaseStatus === 2 && item.isRenewable"
            size="small"
            type="primary"
            plain
            @click="router.push({ path: '/agreement/sign', query: { renewId: String(item.id) } })"
          >
            续约
          </el-button>

          <el-button
            v-if="item.leaseStatus === 5"
            size="small"
            type="success"
            plain
            @click="changeStatus(item.id, 2, '确定撤销退租申请吗？')"
          >
            撤销退租
          </el-button>

          <el-button
            v-if="item.leaseStatus === 7"
            size="small"
            type="danger"
            plain
            @click="changeStatus(item.id, 2, '确定取消续约吗？')"
          >
            取消续约
          </el-button>
        </div>
      </div>
    </div>

    <el-empty
      v-if="!loading && !filtered.length"
      description="还没有租约，先去看看房子吧"
      class="empty-box"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Money } from '@element-plus/icons-vue'
import { agreementApi } from '@/api'
import type { AgreementItemVo } from '@/types/api'
import { LEASE_SOURCE, LEASE_STATUS, dateOnly, leaseTag, money } from '@/utils/format'

const router = useRouter()

const list = ref<AgreementItemVo[]>([])
const loading = ref(false)
const filter = ref(0)

const filtered = computed(() =>
  filter.value === 0 ? list.value : list.value.filter(item => item.leaseStatus === filter.value)
)

async function fetchList() {
  loading.value = true
  try {
    const res = await agreementApi.list()
    list.value = res.data || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function changeStatus(id: number, leaseStatus: number, tip: string) {
  try {
    await ElMessageBox.confirm(tip, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await agreementApi.updateStatus(id, leaseStatus)
    ElMessage.success('操作成功')
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
  gap: 16px;
  flex-wrap: wrap;
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
  flex-wrap: wrap;
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
  min-width: 100px;
}
</style>
