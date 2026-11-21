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
        <el-table-column prop="name" label="名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="dictType(ITEM_TYPE, row.type)" size="small">
              {{ dictLabel(ITEM_TYPE, row.type) }}
            </el-tag>
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
      :title="editingId === undefined ? '新增标签' : '编辑标签'"
      width="440px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="70px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入标签名称" maxlength="20" clearable />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" class="full" placeholder="请选择类型">
            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
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
import { labelApi } from '@/api'
import { ITEM_TYPE, dictLabel, dictOptions, dictType } from '@/utils/dict'
import type { LabelInfo } from '@/types/api'

const typeOptions = dictOptions(ITEM_TYPE)

const loading = ref(false)
const rows = ref<LabelInfo[]>([])
const typeFilter = ref<number>()

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number>()
const formRef = ref<FormInstance>()

const form = reactive<Partial<LabelInfo>>({ name: '', type: undefined })

const rules: FormRules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await labelApi.list(typeFilter.value)
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
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

async function openEdit(row: LabelInfo) {
  editingId.value = row.id
  form.name = row.name
  form.type = row.type
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

async function save() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const payload: LabelInfo = { name: (form.name || '').trim(), type: form.type }
  if (editingId.value !== undefined) payload.id = editingId.value

  saving.value = true
  try {
    await labelApi.save(payload)
    ElMessage.success(editingId.value === undefined ? '新增成功' : '修改成功')
    dialogVisible.value = false
    await load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    saving.value = false
  }
}

async function remove(row: LabelInfo) {
  if (row.id === undefined) return
  const confirmed = await ElMessageBox.confirm(`确定删除标签「${row.name}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) return

  loading.value = true
  try {
    await labelApi.remove(row.id)
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
</style>
