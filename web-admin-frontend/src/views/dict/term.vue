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
        <el-table-column prop="monthCount" label="月数" width="120" />
        <el-table-column label="单位" width="140">
          <template #default="{ row }">{{ row.unit || '-' }}</template>
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
      :title="editingId === undefined ? '新增租期' : '编辑租期'"
      width="440px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="70px">
        <el-form-item label="月数" prop="monthCount">
          <el-input-number v-model="form.monthCount" :min="1" :max="60" />
          <span class="text-muted unit-tip">1 ~ 60</span>
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" placeholder="默认：个月" maxlength="10" clearable />
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
import { termApi } from '@/api'
import type { LeaseTerm } from '@/types/api'

const loading = ref(false)
const rows = ref<LeaseTerm[]>([])

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number>()
const formRef = ref<FormInstance>()

const form = reactive<Partial<LeaseTerm>>({ monthCount: 1, unit: '个月' })

const rules: FormRules = {
  monthCount: [{ required: true, message: '请输入月数', trigger: 'change' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }]
}

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await termApi.list()
    rows.value = res.data.data || []
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

async function openCreate() {
  editingId.value = undefined
  form.monthCount = 1
  form.unit = '个月'
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

async function openEdit(row: LeaseTerm) {
  editingId.value = row.id
  form.monthCount = row.monthCount
  form.unit = row.unit || '个月'
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

async function save() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const payload: LeaseTerm = {
    monthCount: form.monthCount ?? 1,
    unit: (form.unit || '').trim() || '个月'
  }
  if (editingId.value !== undefined) payload.id = editingId.value

  saving.value = true
  try {
    await termApi.save(payload)
    ElMessage.success(editingId.value === undefined ? '新增成功' : '修改成功')
    dialogVisible.value = false
    await load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    saving.value = false
  }
}

async function remove(row: LeaseTerm) {
  if (row.id === undefined) return
  const confirmed = await ElMessageBox.confirm(
    `确定删除租期「${row.monthCount} ${row.unit || ''}」吗？`,
    '删除确认',
    { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) return

  loading.value = true
  try {
    await termApi.remove(row.id)
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
