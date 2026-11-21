<template>
  <div class="page-wrap">
    <div class="card card-pad form-card">
      <h2 class="section-title">预约看房</h2>

      <el-alert
        v-if="apartmentName"
        :title="`预约公寓：${apartmentName}`"
        type="success"
        :closable="false"
        show-icon
        class="alert"
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" size="large">
        <el-form-item label="联系人" prop="name">
          <el-input v-model.trim="form.name" placeholder="请输入您的姓名" maxlength="20" />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model.trim="form.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>

        <el-form-item label="看房时间" prop="appointmentTime">
          <el-date-picker
            v-model="form.appointmentTime"
            type="datetime"
            placeholder="选择看房时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            :disabled-date="disabledDate"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model.trim="form.additionalInfo"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="例如：希望晚上 7 点后看房"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit">提交预约</el-button>
          <el-button @click="router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { apartmentApi, appointmentApi, roomApi } from '@/api'
import { useUserStore } from '@/stores/user'
import type { ViewAppointment } from '@/types/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const apartmentName = ref('')

const form = reactive<ViewAppointment>({
  name: '',
  phone: '',
  apartmentId: Number(route.query.apartmentId || 0),
  appointmentTime: '',
  additionalInfo: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  appointmentTime: [{ required: true, message: '请选择看房时间', trigger: 'change' }]
}

function disabledDate(date: Date) {
  // 不能选今天之前
  return date.getTime() < Date.now() - 24 * 60 * 60 * 1000
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (!form.apartmentId) {
    ElMessage.error('缺少公寓信息，请从房间/公寓详情页进入')
    return
  }

  submitting.value = true
  try {
    await appointmentApi.save({ ...form })
    ElMessage.success('预约成功，等待公寓管家联系您')
    router.replace('/appointment/mine')
  } catch {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  // 预填当前用户信息
  if (userStore.userInfo) {
    form.name = userStore.userInfo.nickname || ''
    form.phone = userStore.userInfo.phone || ''
  }
  const roomId = Number(route.query.roomId || 0)
  if (!form.apartmentId && roomId) {
    // 只传了 roomId 时反查公寓
    try {
      const res = await roomApi.detail(roomId)
      form.apartmentId = res.data.apartmentId
    } catch {
      /* 忽略 */
    }
  }
  if (form.apartmentId) {
    try {
      const res = await apartmentApi.detail(form.apartmentId)
      apartmentName.value = res.data?.name || ''
    } catch {
      /* 忽略 */
    }
  }
})
</script>

<style scoped>
.form-card {
  max-width: 680px;
  margin: 0 auto;
}

.alert {
  margin-bottom: 18px;
}
</style>
