<template>
  <div class="room-detail">
    <van-nav-bar title="房间详情" left-text="返回" left-arrow @click-left="router.back" />
    <div v-if="detail">
      <van-swipe :autoplay="3000" class="swipe" v-if="detail.graphVoList?.length">
        <van-swipe-item v-for="(img, i) in detail.graphVoList" :key="i">
          <van-image :src="img.url" width="100%" height="240" fit="cover" />
        </van-swipe-item>
      </van-swipe>
      <div v-else class="no-image">暂无图片</div>

      <div class="info-card">
        <h2>{{ detail.roomNumber }}</h2>
        <p class="rent">¥{{ detail.rent }} <span>/月</span></p>
        <p class="apt" v-if="detail.apartmentItemVo" @click="goApartment(detail.apartmentItemVo.id)">
          {{ detail.apartmentItemVo.name }}
          <van-icon name="arrow" />
        </p>
      </div>

      <div class="section" v-if="detail.attrValueVoList?.length">
        <div class="section-title">房间属性</div>
        <van-tag v-for="attr in detail.attrValueVoList" :key="attr.id" plain type="primary" style="margin: 3px;">
          {{ attr.attrKeyName }}: {{ attr.name }}
        </van-tag>
      </div>

      <div class="section" v-if="detail.facilityInfoList?.length">
        <div class="section-title">配套设施</div>
        <van-row gutter="10">
          <van-col span="6" v-for="f in detail.facilityInfoList" :key="f.id" style="text-align:center;margin-bottom:8px;">
            <van-icon :name="f.icon || 'checked'" size="20" />
            <p style="font-size:11px;">{{ f.name }}</p>
          </van-col>
        </van-row>
      </div>

      <div class="section" v-if="detail.labelInfoList?.length">
        <div class="section-title">标签</div>
        <van-tag v-for="tag in detail.labelInfoList" :key="tag.id" type="warning" style="margin: 3px;">
          {{ tag.name }}
        </van-tag>
      </div>

      <div class="section" v-if="detail.paymentTypeList?.length">
        <div class="section-title">支付方式</div>
        <p v-for="pt in detail.paymentTypeList" :key="pt.id">{{ pt.name }}</p>
      </div>

      <div class="section" v-if="detail.leaseTermList?.length">
        <div class="section-title">可租期限</div>
        <p v-for="lt in detail.leaseTermList" :key="lt.id">{{ lt.monthCount }}{{ lt.unit }}</p>
      </div>

      <div class="section" v-if="detail.feeValueVoList?.length">
        <div class="section-title">杂项费用</div>
        <p v-for="fee in detail.feeValueVoList" :key="fee.id">{{ fee.feeKeyName }}: {{ fee.name }} {{ fee.unit ? `(${fee.unit})` : '' }}</p>
      </div>

      <van-button type="primary" block round style="margin: 20px;" @click="bookAppointment">
        预约看房
      </van-button>
    </div>
    <van-loading v-else class="loading" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getRoomDetail } from '@/api/room'
import { saveOrUpdateAppointment } from '@/api/appointment'
import type { RoomDetailVo } from '@/types/api'

const route = useRoute()
const router = useRouter()
const detail = ref<RoomDetailVo | null>(null)

onMounted(async () => {
  try {
    const res = await getRoomDetail(Number(route.params.id))
    detail.value = res.data.data
  } catch {
    showToast('加载失败')
  }
})

function goApartment(id: number) {
  router.push(`/apartment/${id}`)
}

async function bookAppointment() {
  if (!detail.value) return
  try {
    await saveOrUpdateAppointment({
      apartmentId: detail.value.apartmentId || detail.value.apartmentItemVo?.id,
      name: '',
      phone: '',
      appointmentTime: new Date().toISOString()
    })
    showToast('预约成功')
  } catch {
    showToast('请先登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.room-detail { padding-bottom: 80px; }
.no-image { height: 200px; display: flex; align-items: center; justify-content: center; background: #eee; color: #999; }
.info-card { padding: 15px; background: #fff; margin-bottom: 10px; }
.info-card h2 { font-size: 18px; }
.rent { font-size: 22px; color: #ee0a24; font-weight: bold; margin: 8px 0; }
.rent span { font-size: 14px; font-weight: normal; }
.apt { color: #1989fa; font-size: 14px; display: flex; align-items: center; }
.section { padding: 12px 15px; background: #fff; margin-bottom: 10px; }
.section-title { font-size: 15px; font-weight: bold; margin-bottom: 8px; }
.loading { display: flex; justify-content: center; margin-top: 100px; }
</style>
