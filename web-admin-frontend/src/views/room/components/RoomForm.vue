<template>
  <el-drawer v-model="visible" :title="form.id ? '编辑房间' : '新增房间'" size="560px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" v-loading="loading">
      <el-form-item label="所属公寓" prop="apartmentId">
        <el-select v-model="form.apartmentId" placeholder="请选择公寓" filterable style="width: 100%">
          <el-option v-for="a in apartments" :key="a.id" :label="a.name" :value="a.id!" />
        </el-select>
      </el-form-item>

      <el-form-item label="房间号" prop="roomNumber">
        <el-input v-model="form.roomNumber" placeholder="如 101" maxlength="20" />
      </el-form-item>

      <el-form-item label="月租金" prop="rent">
        <el-input-number v-model="form.rent" :min="0" :step="100" :precision="2" style="width: 220px" />
        <span class="text-muted" style="margin-left: 8px">元 / 月</span>
      </el-form-item>

      <el-form-item label="房间属性">
        <el-select v-model="attrValueIds" multiple filterable placeholder="请选择属性" style="width: 100%">
          <el-option-group v-for="key in attrKeys" :key="key.id" :label="key.name">
            <el-option
              v-for="value in key.attrValueList || []"
              :key="value.id"
              :label="value.name"
              :value="value.id!"
            />
          </el-option-group>
        </el-select>
      </el-form-item>

      <el-form-item label="配套设施">
        <el-select v-model="facilityIds" multiple filterable placeholder="请选择配套" style="width: 100%">
          <el-option v-for="f in facilities" :key="f.id" :label="f.name" :value="f.id!" />
        </el-select>
      </el-form-item>

      <el-form-item label="标签">
        <el-select v-model="labelIds" multiple filterable placeholder="请选择标签" style="width: 100%">
          <el-option v-for="l in labels" :key="l.id" :label="l.name" :value="l.id!" />
        </el-select>
      </el-form-item>

      <el-form-item label="支付方式">
        <el-select v-model="paymentTypeIds" multiple filterable placeholder="请选择支付方式" style="width: 100%">
          <el-option v-for="p in paymentTypes" :key="p.id" :label="p.name" :value="p.id!" />
        </el-select>
      </el-form-item>

      <el-form-item label="可选租期">
        <el-select v-model="leaseTermIds" multiple filterable placeholder="请选择租期" style="width: 100%">
          <el-option
            v-for="t in leaseTerms"
            :key="t.id"
            :label="`${t.monthCount}${t.unit || '个月'}`"
            :value="t.id!"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="房间图片">
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
import { attrApi, facilityApi, labelApi, paymentApi, roomApi, termApi } from '@/api'
import ImageUploader from '@/components/ImageUploader.vue'
import type {
  ApartmentItemVo,
  AttrKey,
  FacilityInfo,
  GraphVo,
  LabelInfo,
  LeaseTerm,
  PaymentType,
  RoomSubmitVo
} from '@/types/api'

const props = defineProps<{
  apartments: ApartmentItemVo[]
}>()

const emit = defineEmits<{ saved: [] }>()

const visible = ref(false)
const loading = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const attrKeys = ref<AttrKey[]>([])
const facilities = ref<FacilityInfo[]>([])
const labels = ref<LabelInfo[]>([])
const paymentTypes = ref<PaymentType[]>([])
const leaseTerms = ref<LeaseTerm[]>([])

const graphVoList = ref<GraphVo[]>([])
const attrValueIds = ref<number[]>([])
const facilityIds = ref<number[]>([])
const labelIds = ref<number[]>([])
const paymentTypeIds = ref<number[]>([])
const leaseTermIds = ref<number[]>([])

const form = reactive<RoomSubmitVo>({
  id: undefined,
  apartmentId: undefined,
  roomNumber: '',
  rent: 0,
  isRelease: 0
})

const rules: FormRules = {
  apartmentId: [{ required: true, message: '请选择所属公寓', trigger: 'change' }],
  roomNumber: [{ required: true, message: '请输入房间号', trigger: 'blur' }],
  rent: [
    {
      required: true,
      trigger: 'blur',
      validator: (_r: any, value: any, callback: (error?: string | Error) => void) => {
        if (value === null || value === undefined || Number(value) <= 0) {
          callback(new Error('租金必须大于 0'))
          return
        }
        callback()
      }
    }
  ]
}

async function open(id?: number, defaultApartmentId?: number) {
  visible.value = true
  resetForm(defaultApartmentId)
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

function resetForm(defaultApartmentId?: number) {
  Object.assign(form, {
    id: undefined,
    apartmentId: defaultApartmentId,
    roomNumber: '',
    rent: 0,
    isRelease: 0
  })
  graphVoList.value = []
  attrValueIds.value = []
  facilityIds.value = []
  labelIds.value = []
  paymentTypeIds.value = []
  leaseTermIds.value = []
  formRef.value?.clearValidate()
}

async function loadOptions() {
  const [a, f, l, p, t] = await Promise.allSettled([
    attrApi.list(),
    facilityApi.list(2),
    labelApi.list(2),
    paymentApi.list(),
    termApi.list()
  ])
  if (a.status === 'fulfilled') attrKeys.value = a.value.data.data || []
  if (f.status === 'fulfilled') facilities.value = f.value.data.data || []
  if (l.status === 'fulfilled') labels.value = l.value.data.data || []
  if (p.status === 'fulfilled') paymentTypes.value = p.value.data.data || []
  if (t.status === 'fulfilled') leaseTerms.value = t.value.data.data || []
}

async function loadDetail(id: number) {
  const res = await roomApi.detail(id)
  const detail = res.data.data
  if (!detail) {
    ElMessage.error('房间不存在')
    visible.value = false
    return
  }

  Object.assign(form, {
    id: detail.id,
    apartmentId: detail.apartmentId,
    roomNumber: detail.roomNumber,
    rent: Number(detail.rent || 0),
    isRelease: detail.isRelease
  })

  graphVoList.value = (detail.graphVoList || []).map(g => ({ name: g.name, url: g.url }))
  attrValueIds.value = (detail.attrValueVoList || []).map(v => v.id!).filter(Boolean)
  facilityIds.value = (detail.facilityInfoList || []).map(v => v.id!).filter(Boolean)
  labelIds.value = (detail.labelInfoList || []).map(v => v.id!).filter(Boolean)
  paymentTypeIds.value = (detail.paymentTypeList || []).map(v => v.id!).filter(Boolean)
  leaseTermIds.value = (detail.leaseTermList || []).map(v => v.id!).filter(Boolean)
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await roomApi.save({
      ...form,
      graphVoList: graphVoList.value,
      attrValueIds: attrValueIds.value,
      facilityInfoIds: facilityIds.value,
      labelInfoIds: labelIds.value,
      paymentTypeIds: paymentTypeIds.value,
      leaseTermIds: leaseTermIds.value
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
