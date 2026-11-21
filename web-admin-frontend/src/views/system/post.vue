<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-toolbar">
        <el-input
          v-model="query.name"
          class="search-input"
          placeholder="请输入岗位名称"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          <span>搜索</span>
        </el-button>
        <el-button @click="handleReset">
          <el-icon><Refresh /></el-icon>
          <span>重置</span>
        </el-button>
        <div class="spacer" />
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          <span>新增</span>
        </el-button>
      </div>

      <el-table :data="rows" v-loading="loading" size="small" border row-key="id" empty-text="暂无数据">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="岗位编码" width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.postCode || '-' }}</template>
        </el-table-column>
        <el-table-column prop="name" label="岗位名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="描述" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status ?? 0"
              :active-value="1"
              :inactive-value="0"
              :loading="statusLoadingId === row.id"
              @change="value => changeStatus(row, value)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="load"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId === undefined ? '新增岗位' : '编辑岗位'"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="岗位编码" prop="postCode">
          <el-input v-model="form.postCode" placeholder="建议英文大写，如 ADMIN" maxlength="20" clearable />
        </el-form-item>
        <el-form-item label="岗位名称" prop="name">
          <el-input v-model="form.name" placeholder="如：公寓管家" maxlength="20" clearable />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请输入岗位描述"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="item in BASE_STATUS" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-radio>
          </el-radio-group>
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
import { systemPostApi } from '@/api'
import { BASE_STATUS } from '@/utils/dict'
import type { SystemPost } from '@/types/api'

const loading = ref(false)
const rows = ref<SystemPost[]>([])
const total = ref(0)
const statusLoadingId = ref<number>()

const query = reactive({ current: 1, size: 10, name: '' })

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number>()
const formRef = ref<FormInstance>()

const form = reactive<Partial<SystemPost>>({
  postCode: '',
  name: '',
  description: '',
  status: 1
})

const rules: FormRules = {
  postCode: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }]
}

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await systemPostApi.page({
      current: query.current,
      size: query.size,
      name: query.name.trim() || undefined
    })
    const data = res.data.data
    rows.value = data?.records || []
    total.value = data?.total || 0
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.current = 1
  load()
}

function handleReset() {
  query.name = ''
  query.current = 1
  load()
}

function handleSizeChange() {
  query.current = 1
  load()
}

/* ------------------------------------------------------------------ 新增/编辑 */
async function openCreate() {
  editingId.value = undefined
  form.postCode = ''
  form.name = ''
  form.description = ''
  form.status = 1
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

async function openEdit(row: SystemPost) {
  if (row.id === undefined) return
  let detail: SystemPost = row
  try {
    const res = await systemPostApi.detail(row.id)
    if (res.data.data) detail = res.data.data
  } catch {
    /* 拦截器已提示，退回使用列表数据 */
  }
  editingId.value = detail.id ?? row.id
  form.postCode = detail.postCode ?? ''
  form.name = detail.name ?? ''
  form.description = detail.description ?? ''
  form.status = detail.status ?? 1
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

async function save() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const payload: SystemPost = {
    postCode: (form.postCode || '').trim(),
    name: (form.name || '').trim(),
    description: (form.description || '').trim(),
    status: form.status ?? 1
  }
  if (editingId.value !== undefined) payload.id = editingId.value

  saving.value = true
  try {
    await systemPostApi.save(payload)
    ElMessage.success(editingId.value === undefined ? '新增成功' : '修改成功')
    dialogVisible.value = false
    await load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    saving.value = false
  }
}

async function changeStatus(row: SystemPost, value: string | number | boolean) {
  if (row.id === undefined) return
  const status = value ? 1 : 0
  statusLoadingId.value = row.id
  try {
    await systemPostApi.updateStatus(row.id, status)
    row.status = status
    ElMessage.success(status === 1 ? '已启用' : '已禁用')
  } catch {
    /* 拦截器已提示 */
  } finally {
    statusLoadingId.value = undefined
  }
}

async function remove(row: SystemPost) {
  if (row.id === undefined) return
  const confirmed = await ElMessageBox.confirm(`确定删除岗位「${row.name}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) return

  loading.value = true
  try {
    await systemPostApi.remove(row.id)
    ElMessage.success('删除成功')
    if (rows.value.length === 1 && query.current > 1) query.current -= 1
    await load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.search-input {
  width: 220px;
}
</style>
