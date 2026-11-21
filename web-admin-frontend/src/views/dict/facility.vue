<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-toolbar">
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          <span>新增</span>
        </el-button>

        <el-select v-model="typeFilter" class="type-filter" placeholder="全部类型" clearable @change="load">
          <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>

        <div class="spacer" />
        <span class="text-muted">共 {{ rows.length }} 条</span>
      </div>

      <el-table :data="rows" v-loading="loading" size="small" border empty-text="暂无数据">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="dictType(ITEM_TYPE, row.type)" size="small">
              {{ dictLabel(ITEM_TYPE, row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="图标" min-width="160">
          <template #default="{ row }">
            <span class="icon-cell">
              <el-icon><component :is="row.icon || 'CircleCheck'" /></el-icon>
              <span class="text-muted">{{ row.icon || '-' }}</span>
            </span>
          </template>
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
      :title="editingId === undefined ? '新增配套' : '编辑配套'"
      width="460px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="70px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入配套名称" maxlength="20" clearable />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" class="full" placeholder="请选择类型">
            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <div class="icon-row">
            <el-input v-model="form.icon" placeholder="Element Plus 图标名，如 WiFi、Coffee" clearable />
            <el-icon v-if="form.icon" :size="20" class="icon-preview">
              <component :is="form.icon" />
            </el-icon>
          </div>
          <div class="form-tip">Element Plus 图标名，如 WiFi、Coffee</div>
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
import { facilityApi } from '@/api'
import { ITEM_TYPE, dictLabel, dictOptions, dictType } from '@/utils/dict'
import type { FacilityInfo } from '@/types/api'

const typeOptions = dictOptions(ITEM_TYPE)

const loading = ref(false)
const rows = ref<FacilityInfo[]>([])
const typeFilter = ref<number>()

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number>()
const formRef = ref<FormInstance>()

const form = reactive<Partial<FacilityInfo>>({ name: '', type: undefined, icon: '' })

const rules: FormRules = {
  name: [{ required: true, message: '请输入配套名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await facilityApi.list(typeFilter.value)
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
  form.type = typeFilter.value
  form.icon = ''
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

async function openEdit(row: FacilityInfo) {
  editingId.value = row.id
  form.name = row.name
  form.type = row.type
  form.icon = row.icon || ''
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

async function save() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const payload: FacilityInfo = {
    name: (form.name || '').trim(),
    type: form.type,
    icon: (form.icon || '').trim()
  }
  if (editingId.value !== undefined) payload.id = editingId.value

  saving.value = true
  try {
    await facilityApi.save(payload)
    ElMessage.success(editingId.value === undefined ? '新增成功' : '修改成功')
    dialogVisible.value = false
    await load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    saving.value = false
  }
}

async function remove(row: FacilityInfo) {
  if (row.id === undefined) return
  const confirmed = await ElMessageBox.confirm(`确定删除配套「${row.name}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) return

  loading.value = true
  try {
    await facilityApi.remove(row.id)
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
.type-filter {
  width: 150px;
}
.full {
  width: 100%;
}
.icon-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.icon-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}
.icon-preview {
  color: #409eff;
  flex-shrink: 0;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: 2px;
}
</style>
