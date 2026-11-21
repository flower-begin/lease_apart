<template>
  <div class="page-container">
    <el-row :gutter="16">
      <!-- 左栏：属性名 -->
      <el-col :xs="24" :md="10">
        <div class="page-card">
          <div class="page-toolbar">
            <span class="page-title">属性名</span>
            <div class="spacer" />
            <el-button type="primary" size="small" @click="openKeyDialog()">
              <el-icon><Plus /></el-icon>
              <span>新增属性名</span>
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
            empty-text="暂无属性名"
            @row-click="selectKey"
          >
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="属性名" min-width="120">
              <template #default="{ row }">
                <span :class="{ 'key-active': row.id === selectedKey?.id }">{{ row.name }}</span>
                <span class="text-muted">（{{ row.attrValueList?.length || 0 }}）</span>
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

      <!-- 右栏：属性值 -->
      <el-col :xs="24" :md="14">
        <div class="page-card">
          <div class="page-toolbar">
            <span class="page-title">
              属性值
              <span v-if="selectedKey" class="text-muted">· {{ selectedKey.name }}</span>
            </span>
            <div class="spacer" />
            <el-button type="primary" size="small" :disabled="!selectedKey" @click="openValueDialog()">
              <el-icon><Plus /></el-icon>
              <span>新增属性值</span>
            </el-button>
          </div>

          <el-table
            :data="valueList"
            v-loading="loading"
            size="small"
            border
            empty-text="请先在左侧选择属性名"
          >
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="name" label="属性值" min-width="140" show-overflow-tooltip />
            <el-table-column label="所属属性" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="text-muted">{{ row.attrKeyName || selectedKey?.name || '-' }}</span>
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

    <!-- 属性名表单 -->
    <el-dialog
      v-model="keyDialogVisible"
      :title="keyEditingId === undefined ? '新增属性名' : '编辑属性名'"
      width="440px"
      :close-on-click-modal="false"
    >
      <el-form ref="keyFormRef" :model="keyForm" :rules="keyRules" label-width="80px">
        <el-form-item label="属性名" prop="name">
          <el-input v-model="keyForm.name" placeholder="如：朝向、楼层" maxlength="20" clearable />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="keyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="keySaving" @click="saveKey">确定</el-button>
      </template>
    </el-dialog>

    <!-- 属性值表单 -->
    <el-dialog
      v-model="valueDialogVisible"
      :title="valueEditingId === undefined ? '新增属性值' : '编辑属性值'"
      width="440px"
      :close-on-click-modal="false"
    >
      <el-form ref="valueFormRef" :model="valueForm" :rules="valueRules" label-width="80px">
        <el-form-item label="所属属性">
          <span class="text-muted">{{ selectedKey?.name || '-' }}</span>
        </el-form-item>
        <el-form-item label="属性值" prop="name">
          <el-input v-model="valueForm.name" placeholder="如：朝南、8层" maxlength="20" clearable />
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
import { attrApi } from '@/api'
import type { AttrKey, AttrValue } from '@/types/api'

const loading = ref(false)
const keyList = ref<AttrKey[]>([])
const selectedKey = ref<AttrKey | null>(null)

const valueList = computed<AttrValue[]>(() => selectedKey.value?.attrValueList ?? [])

/* 属性名弹窗 */
const keyDialogVisible = ref(false)
const keySaving = ref(false)
const keyEditingId = ref<number>()
const keyFormRef = ref<FormInstance>()
const keyForm = reactive<Partial<AttrKey>>({ name: '' })

/* 属性值弹窗 */
const valueDialogVisible = ref(false)
const valueSaving = ref(false)
const valueEditingId = ref<number>()
const valueFormRef = ref<FormInstance>()
const valueForm = reactive<Partial<AttrValue>>({ name: '', attrKeyId: undefined })

const keyRules: FormRules = {
  name: [{ required: true, message: '请输入属性名', trigger: 'blur' }]
}

const valueRules: FormRules = {
  name: [{ required: true, message: '请输入属性值', trigger: 'blur' }]
}

onMounted(() => load())

async function load(keepId?: number) {
  loading.value = true
  try {
    const res = await attrApi.list()
    keyList.value = res.data.data || []
    const targetId = keepId ?? selectedKey.value?.id
    selectedKey.value = keyList.value.find(item => item.id === targetId) ?? keyList.value[0] ?? null
    if (keyEditingId.value !== undefined) {
      keyForm.name = keyList.value.find(item => item.id === keyEditingId.value)?.name ?? keyForm.name
    }
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

function selectKey(row: AttrKey) {
  selectedKey.value = row
}

/* ---------------------------------------------------------------- 属性名 */
async function openKeyDialog(row?: AttrKey) {
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

  const payload: AttrKey = { name: (keyForm.name || '').trim() }
  if (keyEditingId.value !== undefined) payload.id = keyEditingId.value

  keySaving.value = true
  try {
    await attrApi.saveKey(payload)
    ElMessage.success(keyEditingId.value === undefined ? '新增成功' : '修改成功')
    keyDialogVisible.value = false
    await load(payload.id)
  } catch {
    /* 拦截器已提示 */
  } finally {
    keySaving.value = false
  }
}

async function removeKey(row: AttrKey) {
  if (row.id === undefined) return
  const confirmed = await ElMessageBox.confirm(
    `删除属性名「${row.name}」会一并影响其下的值，确定？`,
    '删除确认',
    { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) return

  loading.value = true
  try {
    await attrApi.removeKey(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

/* ---------------------------------------------------------------- 属性值 */
async function openValueDialog(row?: AttrValue) {
  valueEditingId.value = row?.id
  valueForm.name = row?.name ?? ''
  valueForm.attrKeyId = row?.attrKeyId ?? selectedKey.value?.id
  valueDialogVisible.value = true
  await nextTick()
  valueFormRef.value?.clearValidate()
}

async function saveValue() {
  if (!valueFormRef.value) return
  const valid = await valueFormRef.value.validate().catch(() => false)
  if (!valid) return

  const attrKeyId = valueForm.attrKeyId
  if (attrKeyId === undefined) {
    ElMessage.warning('请先在左侧选择属性名')
    return
  }

  const payload: AttrValue = { name: (valueForm.name || '').trim(), attrKeyId }
  if (valueEditingId.value !== undefined) payload.id = valueEditingId.value

  valueSaving.value = true
  try {
    await attrApi.saveValue(payload)
    ElMessage.success(valueEditingId.value === undefined ? '新增成功' : '修改成功')
    valueDialogVisible.value = false
    await load(attrKeyId)
  } catch {
    /* 拦截器已提示 */
  } finally {
    valueSaving.value = false
  }
}

async function removeValue(row: AttrValue) {
  if (row.id === undefined) return
  const confirmed = await ElMessageBox.confirm(`确定删除属性值「${row.name}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) return

  loading.value = true
  try {
    await attrApi.removeValue(row.id)
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
