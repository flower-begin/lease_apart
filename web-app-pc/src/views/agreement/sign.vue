<template>
  <div class="page-wrap">
    <div class="card card-pad form-card">
      <h2 class="section-title">{{ isRenew ? '租约续约' : '在线签约' }}</h2>

      <el-alert
        v-if="roomTitle"
        :title="roomTitle"
        type="success"
        :closable="false"
        show-icon
        class="alert"
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" size="large">
        <template v-if="!isRenew">
          <el-form-item label="承租人" prop="name">
            <el-input v-model.trim="form.name" placeholder="请输入姓名" maxlength="20" />
          </el-form-item>

          <el-form-item label="身份证号" prop="identificationNumber">
            <el-input
              v-model.trim="form.identificationNumber"
              placeholder="请输入身份证号"
              maxlength="18"
            />
          </el-form-item>
        </template>

        <el-form-item label="开始日期" prop="leaseStartDate">
          <el-date-picker
            v-model="form.leaseStartDate"
            type="date"
            placeholder="选择起租日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            @change="recalcEndDate"
          />
        </el-form-item>

        <el-form-item label="租期" prop="leaseTermId">
          <el-select
            v-model="form.leaseTermId"
            placeholder="请选择租期"
            style="width: 100%"
            @change="recalcEndDate"
          >
            <el-option
              v-for="item in terms"
              :key="item.id"
              :label="termText(item.monthCount, item.unit)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="结束日期" prop="leaseEndDate">
          <el-date-picker
            v-model="form.leaseEndDate"
            type="date"
            placeholder="选择结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="支付方式" prop="paymentTypeId">
          <el-select v-model="form.paymentTypeId" placeholder="请选择支付方式" style="width: 100%">
            <el-option
              v-for="item in payments"
              :key="item.id"
              :label="paymentText(item.name, item.payMonthCount)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="月租金">
          <el-input v-model.number="form.rent" disabled>
            <template #append>元 / 月</template>
          </el-input>
        </el-form-item>

        <el-form-item label="押金">
          <el-input v-model.number="form.deposit">
            <template #append>元</template>
          </el-input>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model.trim="form.additionalInfo"
            type="textarea"
            :rows="2"
            maxlength="200"
            show-word-limit
            placeholder="有其他约定可写在这里"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit">
            {{ isRenew ? '提交续约' : '提交签约' }}
          </el-button>
          <el-button @click="router.back()">返回</el-button>
        </el-form-item>
      </el-form>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="提交后状态为「签约待确认」，等待公寓管家确认后正式生效。"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { agreementApi, paymentApi, roomApi, termApi } from '@/api'
import { useUserStore } from '@/stores/user'
import type { LeaseTerm, PaymentType } from '@/types/api'
import { paymentText, termText } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const renewId = Number(route.query.renewId || 0)
const isRenew = computed(() => Boolean(renewId))

const formRef = ref<FormInstance>()
const submitting = ref(false)
const roomTitle = ref('')
const terms = ref<LeaseTerm[]>([])
const payments = ref<PaymentType[]>([])

const today = new Date()
const defaultStart = formatDate(today)

const form = reactive({
  name: '',
  identificationNumber: '',
  apartmentId: 0,
  roomId: 0,
  leaseStartDate: defaultStart,
  leaseEndDate: '',
  leaseTermId: undefined as number | undefined,
  paymentTypeId: undefined as number | undefined,
  rent: 0,
  deposit: 0,
  additionalInfo: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入承租人姓名', trigger: 'blur' }],
  leaseStartDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  leaseTermId: [{ required: true, message: '请选择租期', trigger: 'change' }],
  leaseEndDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
  paymentTypeId: [{ required: true, message: '请选择支付方式', trigger: 'change' }]
}

function formatDate(d: Date) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** 按租期自动算结束日期 */
function recalcEndDate() {
  const term = terms.value.find(item => item.id === form.leaseTermId)
  if (!term || !form.leaseStartDate) return
  const start = new Date(form.leaseStartDate)
  if (Number.isNaN(start.getTime())) return
  const unit = (term.unit || '').toUpperCase()
  const end = new Date(start)
  if (unit.includes('DAY')) {
    end.setDate(end.getDate() + term.monthCount)
  } else if (unit.includes('YEAR')) {
    end.setFullYear(end.getFullYear() + term.monthCount)
  } else {
    end.setMonth(end.getMonth() + term.monthCount)
  }
  form.leaseEndDate = formatDate(end)
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isRenew.value) {
      await agreementApi.renew({
        id: renewId,
        leaseStartDate: form.leaseStartDate,
        leaseEndDate: form.leaseEndDate,
        leaseTermId: form.leaseTermId,
        rent: form.rent,
        paymentTypeId: form.paymentTypeId
      })
      ElMessage.success('续约申请已提交')
    } else {
      await agreementApi.sign({
        name: form.name,
        identificationNumber: form.identificationNumber,
        apartmentId: form.apartmentId,
        roomId: form.roomId,
        leaseStartDate: form.leaseStartDate,
        leaseEndDate: form.leaseEndDate,
        leaseTermId: form.leaseTermId,
        paymentTypeId: form.paymentTypeId,
        rent: form.rent,
        deposit: form.deposit,
        additionalInfo: form.additionalInfo
      })
      ElMessage.success('签约申请已提交')
    }
    router.replace('/agreement/mine')
  } catch {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  if (userStore.userInfo) {
    form.name = userStore.userInfo.nickname || ''
  }

  if (isRenew.value) {
    try {
      const res = await agreementApi.detail(renewId)
      const d = res.data
      form.name = d.name || form.name
      form.apartmentId = d.apartmentId
      form.roomId = d.roomId
      form.rent = Number(d.rent || 0)
      form.deposit = Number(d.deposit || 0)
      form.leaseStartDate = d.leaseEndDate || defaultStart
      roomTitle.value = `${d.apartmentName} ${d.roomNumber}（续约）`
      if (d.roomId) await loadRoomOptions(d.roomId)
    } catch {
      /* 忽略 */
    }
    return
  }

  const roomId = Number(route.query.roomId || 0)
  if (!roomId) {
    ElMessage.warning('缺少房间信息，请从房间详情页进入')
    return
  }
  try {
    const res = await roomApi.detail(roomId)
    const room = res.data
    form.apartmentId = room.apartmentId
    form.roomId = room.id
    form.rent = Number(room.rent || 0)
    form.deposit = Number(room.rent || 0)
    roomTitle.value = `${room.apartmentItemVo?.name || ''} ${room.roomNumber}`
    terms.value = room.leaseTermList || []
    payments.value = room.paymentTypeList || []
    if (terms.value.length) {
      form.leaseTermId = terms.value[0].id
    }
    if (payments.value.length) {
      form.paymentTypeId = payments.value[0].id
    }
    recalcEndDate()
  } catch {
    /* 忽略 */
  }
})

/** 续约模式下也要拿到该房间的可选租期/支付方式 */
async function loadRoomOptions(roomId: number) {
  try {
    const [termRes, payRes] = await Promise.all([termApi.listByRoom(roomId), paymentApi.listByRoom(roomId)])
    terms.value = termRes.data || []
    payments.value = payRes.data || []
    if (terms.value.length) form.leaseTermId = terms.value[0].id
    if (payments.value.length) form.paymentTypeId = payments.value[0].id
    recalcEndDate()
  } catch {
    /* 忽略 */
  }
}
</script>

<style scoped>
.form-card {
  max-width: 720px;
  margin: 0 auto;
}

.alert {
  margin-bottom: 18px;
}
</style>
