<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-toolbar">
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          <span>新增</span>
        </el-button>
        <div class="spacer" />
        <span class="text-muted">共 {{ rows.length }} 条</span>
      </div>

      <el-table :data="rows" v-loading="loading" size="small" border empty-text="暂无数据">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="payMonthCount" label="付几个月" width="120" />
        <el-table-column label="附加说明" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.additionalInfo || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId === undefined ? '新增支付方式' : '编辑支付方式'"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如：押一付三" maxlength="20" clearable />
        </el-form-item>
        <el-form-item label="付几个月" prop="payMonthCount">
          <el-input-number v-model="form.payMonthCount" :min="1" :max="36" />
          <span class="text-muted unit-tip">1 ~ 36 个月</span>
        </el-form-item>
        <el-form-item label="附加说明" prop="additionalInfo">
          <el-input
            v-model="form.additionalInfo"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请输入附加说明"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { paymentApi } from '@/api'
import type { PaymentType } from '@/types/api'

const loading = ref(false)
const rows = ref<PaymentType[]>([])

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number>()
const formRef = ref<FormInstance>()

const form = reactive<Partial<PaymentType>>({ name: '', payMonthCount: 1, additionalInfo: '' })

const rules: FormRules = {
  name: [{ required: true, message: '请输入支付方式名称', trigger: 'blur' }],
  payMonthCount: [{ required: true, message: '请输入付几个月', trigger: 'change' }]
}

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await paymentApi.list()
    rows.value = res.data.data || []
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

async function openCreate() {
  editingId.value = undefined
  form.name = ''
  form.payMonthCount = 1
  form.additionalInfo = ''
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

async function openEdit(row: PaymentType) {
  editingId.value = row.id
  form.name = row.name
  form.payMonthCount = row.payMonthCount ?? 1
  form.additionalInfo = row.additionalInfo ?? ''
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

async function save() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const payload: PaymentType = {
    name: (form.name || '').trim(),
    payMonthCount: form.payMonthCount ?? 1,
    additionalInfo: (form.additionalInfo || '').trim()
  }
  if (editingId.value !== undefined) payload.id = editingId.value

  saving.value = true
  try {
    await paymentApi.save(payload)
    ElMessage.success(editingId.value === undefined ? '新增成功' : '修改成功')
    dialogVisible.value = false
    await load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    saving.value = false
  }
}

async function remove(row: PaymentType) {
  if (row.id === undefined) return
  const confirmed = await ElMessageBox.confirm(`确定删除支付方式「${row.name}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) return

  loading.value = true
  try {
    await paymentApi.remove(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.unit-tip {
  margin-left: 10px;
  font-size: 12px;
}
</style>
