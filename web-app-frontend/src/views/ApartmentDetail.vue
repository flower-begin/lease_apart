<template>
  <div class="apartment-detail">
    <van-nav-bar title="公寓详情" left-text="返回" left-arrow @click-left="router.back" />
    <div v-if="detail">
      <van-swipe :autoplay="3000" class="swipe" v-if="detail.graphVoList?.length">
        <van-swipe-item v-for="(img, i) in detail.graphVoList" :key="i">
          <van-image :src="img.url" width="100%" height="240" fit="cover" />
        </van-swipe-item>
      </van-swipe>
      <div v-else class="no-image">暂无图片</div>
      <div class="info">
        <h2>{{ detail.name }}</h2>
        <p class="intro">{{ detail.introduction }}</p>
        <p class="addr">{{ detail.provinceName }} {{ detail.cityName }} {{ detail.districtName }} {{ detail.addressDetail }}</p>
      </div>
      <div class="tags" v-if="detail.labelInfoList?.length">
        <van-tag v-for="tag in detail.labelInfoList" :key="tag.id" type="primary" style="margin-right: 5px;">{{ tag.name }}</van-tag>
      </div>
      <div class="facilities" v-if="detail.facilityInfoList?.length">
        <div class="section-title">配套设施</div>
        <van-row gutter="10">
          <van-col span="6" v-for="f in detail.facilityInfoList" :key="f.id" style="text-align:center;margin-bottom:10px;">
            <van-icon :name="f.icon || 'checked'" size="24" />
            <p style="font-size:12px;">{{ f.name }}</p>
          </van-col>
        </van-row>
      </div>
      <van-button type="primary" block round style="margin: 20px;" @click="showPhone">{{ detail.phone }}</van-button>
    </div>
    <van-loading v-else class="loading" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getApartmentDetail } from '@/api/apartment'
import type { ApartmentDetailVo } from '@/types/api'

const route = useRoute()
const router = useRouter()
const detail = ref<ApartmentDetailVo | null>(null)

onMounted(async () => {
  try {
    const res = await getApartmentDetail(Number(route.params.id))
    detail.value = res.data.data
  } catch {
    showToast('加载失败')
  }
})

function showPhone() {
  if (detail.value?.phone) {
    showToast(detail.value.phone)
  }
}
</script>

<style scoped>
.apartment-detail { padding-bottom: 50px; }
.no-image { height: 200px; display: flex; align-items: center; justify-content: center; background: #eee; color: #999; }
.info { padding: 15px; }
.info h2 { font-size: 18px; margin-bottom: 8px; }
.intro { color: #666; font-size: 14px; margin-bottom: 5px; }
.addr { color: #999; font-size: 13px; }
.tags { padding: 0 15px 10px; }
.facilities { padding: 0 15px; }
.section-title { font-size: 15px; font-weight: bold; margin-bottom: 10px; }
.loading { display: flex; justify-content: center; margin-top: 100px; }
</style>
