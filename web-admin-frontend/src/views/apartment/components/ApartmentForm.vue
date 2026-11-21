<template>
  <el-drawer v-model="visible" :title="form.id ? '编辑公寓' : '新增公寓'" size="560px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" v-loading="loading">
      <el-form-item label="公寓名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入公寓名称" maxlength="50" show-word-limit />
      </el-form-item>

      <el-form-item label="公寓介绍">
        <el-input
          v-model="form.introduction"
          type="textarea"
          :rows="3"
          maxlength="1000"
          show-word-limit
          placeholder="介绍一下公寓的亮点"
        />
      </el-form-item>

      <el-form-item label="所在地区" prop="districtId">
        <div class="region-row">
          <el-select v-model="form.provinceId" placeholder="省" clearable @change="onProvinceChange">
            <el-option v-for="p in provinces" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
          <el-select v-model="form.cityId" placeholder="市" clearable :disabled="!form.provinceId" @change="onCityChange">
            <el-option v-for="c in cities" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <el-select v-model="form.districtId" placeholder="区" clearable :disabled="!form.cityId">
            <el-option v-for="d in districts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </div>
      </el-form-item>

      <el-form-item label="详细地址">
        <el-input v-model="form.addressDetail" placeholder="街道、门牌号" maxlength="200" />
      </el-form-item>

      <el-form-item label="经纬度">
        <div class="region-row">
          <el-input v-model="form.latitude" placeholder="经度，如 116.4757" />
          <el-input v-model="form.longitude" placeholder="纬度，如 39.9968" />
        </div>
      </el-form-item>

      <el-form-item label="前台电话">
        <el-input v-model="form.phone" placeholder="如 010-64701234" maxlength="20" />
      </el-form-item>

      <el-form-item label="标签">
        <el-select v-model="labelIds" multiple filterable placeholder="请选择标签" style="width: 100%">
          <el-option v-for="l in labels" :key="l.id" :label="l.name" :value="l.id!" />
        </el-select>
      </el-form-item>

      <el-form-item label="配套设施">
        <el-select v-model="facilityIds" multiple filterable placeholder="请选择配套" style="width: 100%">
          <el-option v-for="f in facilities" :key="f.id" :label="f.name" :value="f.id!" />
        </el-select>
      </el-form-item>

      <el-form-item label="杂费项">
        <el-select v-model="feeValueIds" multiple filterable placeholder="请选择该公寓收取的杂费" style="width: 100%">
          <el-option-group v-for="key in feeKeys" :key="key.id" :label="key.name">
            <el-option
              v-for="value in key.feeValueList || []"
              :key="value.id"
              :label="`${value.name}${value.unit ? '（' + value.unit + '）' : ''}`"
              :value="value.id!"
            />
          </el-option-group>
        </el-select>
      </el-form-item>

      <el-form-item label="公寓图片">
        <image-uploader v-model="graphVoList" :limit="8" />
      </el-form-item>

      <el-form-item label="发布状态">
        <el-radio-group v-model="form.isRelease">
          <el-radio :value="1">已发布</el-radio>
          <el-radio :value="0">未发布</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { apartmentApi, facilityApi, feeApi, labelApi, regionApi } from '@/api'
import ImageUploader from '@/components/ImageUploader.vue'
import type {
  ApartmentSubmitVo,
  CityInfo,
  DistrictInfo,
  FacilityInfo,
  FeeKey,
  GraphVo,
  LabelInfo,
  ProvinceInfo
} from '@/types/api'

const emit = defineEmits<{ saved: [] }>()

const visible = ref(false)
const loading = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const provinces = ref<ProvinceInfo[]>([])
const cities = ref<CityInfo[]>([])
const districts = ref<DistrictInfo[]>([])
const labels = ref<LabelInfo[]>([])
const facilities = ref<FacilityInfo[]>([])
const feeKeys = ref<FeeKey[]>([])

const graphVoList = ref<GraphVo[]>([])
const labelIds = ref<number[]>([])
const facilityIds = ref<number[]>([])
const feeValueIds = ref<number[]>([])

const form = reactive<ApartmentSubmitVo>({
  id: undefined,
  name: '',
  introduction: '',
  provinceId: undefined,
  provinceName: '',
  cityId: undefined,
  cityName: '',
  districtId: undefined,
  districtName: '',
  addressDetail: '',
  latitude: '',
  longitude: '',
  phone: '',
  isRelease: 0
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入公寓名称', trigger: 'blur' }],
  districtId: [
    {
      required: true,
      message: '请选择所在地区',
      trigger: 'change',
      validator: (_rule: any, _value: any, callback: (error?: string | Error) => void) => {
        if (!form.provinceId || !form.cityId || !form.districtId) {
          callback(new Error('请选择完整的省 / 市 / 区'))
          return
        }
        callback()
      }
    }
  ]
}

/** 打开抽屉：传 id 为编辑，不传为新增 */
async function open(id?: number) {
  visible.value = true
  resetForm()
  loading.value = true
  try {
    await loadOptions()
    if (id) {
      await loadDetail(id)
    }
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    name: '',
    introduction: '',
    provinceId: undefined,
    provinceName: '',
    cityId: undefined,
    cityName: '',
    districtId: undefined,
    districtName: '',
    addressDetail: '',
    latitude: '',
    longitude: '',
    phone: '',
    isRelease: 0
  })
  graphVoList.value = []
  labelIds.value = []
  facilityIds.value = []
  feeValueIds.value = []
  cities.value = []
  districts.value = []
  formRef.value?.clearValidate()
}

async function loadOptions() {
  const [p, l, f, fee] = await Promise.allSettled([
    regionApi.provinces(),
    labelApi.list(1),
    facilityApi.list(1),
    feeApi.list()
  ])
  if (p.status === 'fulfilled') provinces.value = p.value.data.data || []
  if (l.status === 'fulfilled') labels.value = l.value.data.data || []
  if (f.status === 'fulfilled') facilities.value = f.value.data.data || []
  if (fee.status === 'fulfilled') feeKeys.value = fee.value.data.data || []
}

async function loadDetail(id: number) {
  const res = await apartmentApi.detail(id)
  const detail = res.data.data
  if (!detail) {
    ElMessage.error('公寓不存在')
    visible.value = false
    return
  }

  Object.assign(form, {
    id: detail.id,
    name: detail.name,
    introduction: detail.introduction || '',
    provinceId: detail.provinceId,
    provinceName: detail.provinceName,
    cityId: detail.cityId,
    cityName: detail.cityName,
    districtId: detail.districtId,
    districtName: detail.districtName,
    addressDetail: detail.addressDetail || '',
    latitude: detail.latitude || '',
    longitude: detail.longitude || '',
    phone: detail.phone || '',
    isRelease: detail.isRelease
  })

  graphVoList.value = (detail.graphVoList || []).map(g => ({ name: g.name, url: g.url }))
  labelIds.value = (detail.labelInfoList || []).map(l => l.id!).filter(Boolean)
  facilityIds.value = (detail.facilityInfoList || []).map(f => f.id!).filter(Boolean)
  feeValueIds.value = (detail.feeValueVoList || []).map(f => f.id!).filter(Boolean)

  // 级联回填
  if (form.provinceId) {
    const cityRes = await regionApi.cities(form.provinceId)
    cities.value = cityRes.data.data || []
  }
  if (form.cityId) {
    const districtRes = await regionApi.districts(form.cityId)
    districts.value = districtRes.data.data || []
  }
}

async function onProvinceChange() {
  form.cityId = undefined
  form.districtId = undefined
  cities.value = []
  districts.value = []
  if (!form.provinceId) return
  const res = await regionApi.cities(form.provinceId)
  cities.value = res.data.data || []
}

async function onCityChange() {
  form.districtId = undefined
  districts.value = []
  if (!form.cityId) return
  const res = await regionApi.districts(form.cityId)
  districts.value = res.data.data || []
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 冗余名称一起提交，后端直接落库
  form.provinceName = provinces.value.find(p => p.id === form.provinceId)?.name
  form.cityName = cities.value.find(c => c.id === form.cityId)?.name
  form.districtName = districts.value.find(d => d.id === form.districtId)?.name

  saving.value = true
  try {
    await apartmentApi.save({
      ...form,
      labelIds: labelIds.value,
      facilityInfoIds: facilityIds.value,
      feeValueIds: feeValueIds.value,
      graphVoList: graphVoList.value
    })
    ElMessage.success('保存成功')
    visible.value = false
    emit('saved')
  } catch {
    /* 拦截器已提示 */
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.region-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.region-row > * {
  flex: 1;
}
</style>
