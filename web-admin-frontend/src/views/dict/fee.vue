<template>
  <div class="page-container">
    <el-row :gutter="16">
      <!-- 左栏：费用名 -->
      <el-col :xs="24" :md="10">
        <div class="page-card">
          <div class="page-toolbar">
            <span class="page-title">费用名</span>
            <div class="spacer" />
            <el-button type="primary" size="small" @click="openKeyDialog()">
              <el-icon><Plus /></el-icon>
              <span>新增费用名</span>
            </el-button>
          </div>

          <el-table
            :data="keyList"
            v-loading="loading"
            size="small"
            border
            row-key="id"
            highlight-current-row
            :current-row-key="selectedKey?.id"
            empty-text="暂无费用名"
            @row-click="selectKey"
          >
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="费用名" min-width="120">
              <template #default="{ row }">
                <span :class="{ 'key-active': row.id === selectedKey?.id }">{{ row.name }}</span>
                <span class="text-muted">（{{ row.feeValueList?.length || 0 }}）</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button link type="primary" @click.stop="selectKey(row)">查看子项</el-button>
                <el-button link type="primary" @click.stop="openKeyDialog(row)">编辑</el-button>
                <el-button link type="danger" @click.stop="removeKey(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>

      <!-- 右栏：费用值 -->
      <el-col :xs="24" :md="14">
        <div class="page-card">
          <div class="page-toolbar">
            <span class="page-title">
              费用值
              <span v-if="selectedKey" class="text-muted">· {{ selectedKey.name }}</span>
            </span>
            <div class="spacer" />
            <el-button type="primary" size="small" :disabled="!selectedKey" @click="openValueDialog()">
              <el-icon><Plus /></el-icon>
              <span>新增费用值</span>
            </el-button>
          </div>

          <el-table
            :data="valueList"
            v-loading="loading"
            size="small"
            border
            empty-text="请先在左侧选择费用名"
          >
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="name" label="费用值" min-width="140" show-overflow-tooltip />
            <el-table-column label="单位" width="110">
              <template #default="{ row }">{{ row.unit || '-' }}</template>
            </el-table-column>
            <el-table-column label="所属费用" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="text-muted">{{ row.feeKeyName || selectedKey?.name || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openValueDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="removeValue(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>

    <!-- 费用名表单 -->
    <el-dialog
      v-model="keyDialogVisible"
      :title="keyEditingId === undefined ? '新增费用名' : '编辑费用名'"
      width="440px"
      :close-on-click-modal="false"
    >
      <el-form ref="keyFormRef" :model="keyForm" :rules="keyRules" label-width="80px">
        <el-form-item label="费用名" prop="name">
          <el-input v-model="keyForm.name" placeholder="如：水费、网费" maxlength="20" clearable />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="keyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="keySaving" @click="saveKey">确定</el-button>
      </template>
    </el-dialog>

    <!-- 费用值表单 -->
    <el-dialog
      v-model="valueDialogVisible"
      :title="valueEditingId === undefined ? '新增费用值' : '编辑费用值'"
      width="440px"
      :close-on-click-modal="false"
    >
      <el-form ref="valueFormRef" :model="valueForm" :rules="valueRules" label-width="80px">
        <el-form-item label="所属费用">
          <span class="text-muted">{{ selectedKey?.name || '-' }}</span>
        </el-form-item>
        <el-form-item label="费用值" prop="name">
          <el-input v-model="valueForm.name" placeholder="如：50、300" maxlength="20" clearable />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="valueForm.unit" placeholder="如：元/月、元" maxlength="10" clearable />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="valueDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="valueSaving" @click="saveValue">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { feeApi } from '@/api'
import type { FeeKey, FeeValue } from '@/types/api'

const loading = ref(false)
const keyList = ref<FeeKey[]>([])
const selectedKey = ref<FeeKey | null>(null)

const valueList = computed<FeeValue[]>(() => selectedKey.value?.feeValueList ?? [])

/* 费用名弹窗 */
const keyDialogVisible = ref(false)
const keySaving = ref(false)
const keyEditingId = ref<number>()
const keyFormRef = ref<FormInstance>()
const keyForm = reactive<Partial<FeeKey>>({ name: '' })

/* 费用值弹窗 */
const valueDialogVisible = ref(false)
const valueSaving = ref(false)
const valueEditingId = ref<number>()
const valueFormRef = ref<FormInstance>()
const valueForm = reactive<Partial<FeeValue>>({ name: '', unit: '', feeKeyId: undefined })

const keyRules: FormRules = {
  name: [{ required: true, message: '请输入费用名', trigger: 'blur' }]
}

const valueRules: FormRules = {
  name: [{ required: true, message: '请输入费用值', trigger: 'blur' }]
}

onMounted(() => load())

async function load(keepId?: number) {
  loading.value = true
  try {
    const res = await feeApi.list()
    keyList.value = res.data.data || []
    const targetId = keepId ?? selectedKey.value?.id
    selectedKey.value = keyList.value.find(item => item.id === targetId) ?? keyList.value[0] ?? null
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

function selectKey(row: FeeKey) {
  selectedKey.value = row
}

/* ---------------------------------------------------------------- 费用名 */
async function openKeyDialog(row?: FeeKey) {
  keyEditingId.value = row?.id
  keyForm.name = row?.name ?? ''
  keyDialogVisible.value = true
  await nextTick()
  keyFormRef.value?.clearValidate()
}

async function saveKey() {
  if (!keyFormRef.value) return
  const valid = await keyFormRef.value.validate().catch(() => false)
  if (!valid) return

  const payload: FeeKey = { name: (keyForm.name || '').trim() }
  if (keyEditingId.value !== undefined) payload.id = keyEditingId.value

  keySaving.value = true
  try {
    await feeApi.saveKey(payload)
    ElMessage.success(keyEditingId.value === undefined ? '新增成功' : '修改成功')
    keyDialogVisible.value = false
    await load(payload.id)
  } catch {
    /* 拦截器已提示 */
  } finally {
    keySaving.value = false
  }
}

async function removeKey(row: FeeKey) {
  if (row.id === undefined) return
  const confirmed = await ElMessageBox.confirm(
    `删除费用名「${row.name}」会一并影响其下的值，确定？`,
    '删除确认',
    { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) return

  loading.value = true
  try {
    await feeApi.removeKey(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

/* ---------------------------------------------------------------- 费用值 */
async function openValueDialog(row?: FeeValue) {
  valueEditingId.value = row?.id
  valueForm.name = row?.name ?? ''
  valueForm.unit = row?.unit ?? ''
  valueForm.feeKeyId = row?.feeKeyId ?? selectedKey.value?.id
  valueDialogVisible.value = true
  await nextTick()
  valueFormRef.value?.clearValidate()
}

async function saveValue() {
  if (!valueFormRef.value) return
  const valid = await valueFormRef.value.validate().catch(() => false)
  if (!valid) return

  const feeKeyId = valueForm.feeKeyId
  if (feeKeyId === undefined) {
    ElMessage.warning('请先在左侧选择费用名')
    return
  }

  const payload: FeeValue = {
    name: (valueForm.name || '').trim(),
    unit: (valueForm.unit || '').trim(),
    feeKeyId
  }
  if (valueEditingId.value !== undefined) payload.id = valueEditingId.value

  valueSaving.value = true
  try {
    await feeApi.saveValue(payload)
    ElMessage.success(valueEditingId.value === undefined ? '新增成功' : '修改成功')
    valueDialogVisible.value = false
    await load(feeKeyId)
  } catch {
    /* 拦截器已提示 */
  } finally {
    valueSaving.value = false
  }
}

async function removeValue(row: FeeValue) {
  if (row.id === undefined) return
  const confirmed = await ElMessageBox.confirm(`确定删除费用值「${row.name}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) return

  loading.value = true
  try {
    await feeApi.removeValue(row.id)
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
.key-active {
  color: #e07a5f;
  font-weight: 600;
}
.page-title {
  margin-bottom: 0;
}
</style>
