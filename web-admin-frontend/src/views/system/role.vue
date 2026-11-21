<template>
  <div class="role-page">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="keyword" placeholder="搜索角色名称 / 编码" clearable style="width: 240px" />
        </div>
        <div class="toolbar-right">
          <el-button v-perm="'system:role:save'" type="primary" @click="openForm()">
            <el-icon><Plus /></el-icon> 新增角色
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="filteredRoles" border stripe>
        <el-table-column prop="name" label="角色名称" min-width="140" />
        <el-table-column prop="code" label="角色编码" min-width="160">
          <template #default="{ row }">
            <el-tag type="info" effect="plain">{{ row.code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="数据范围" width="120">
          <template #default="{ row }">
            {{ scopeLabel(row.dataScope) }}
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :disabled="!userStore.hasPerm('system:role:save')"
              @change="(v: boolean) => toggleStatus(row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-perm="'system:role:save'" link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button v-perm="'system:role:assign'" link type="primary" @click="openAssign(row)">
              分配权限
            </el-button>
            <el-button
              v-perm="'system:role:delete'"
              link
              type="danger"
              :disabled="row.code === 'super_admin'"
              @click="remove(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增 / 编辑角色 -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑角色' : '新增角色'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="如：门店店长" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" placeholder="如：manager（英文、下划线）" :disabled="form.code === 'super_admin'" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option label="全部数据" value="ALL" />
            <el-option label="本门店" value="STORE" />
            <el-option label="仅本人" value="SELF" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单权限 -->
    <el-dialog v-model="assignVisible" :title="`分配权限 · ${assignRole?.name || ''}`" width="560px">
      <el-alert
        v-if="assignRole?.code === 'super_admin'"
        type="warning"
        :closable="false"
        title="超级管理员固定拥有全部权限，保存时会自动还原为全量"
        style="margin-bottom: 12px"
      />
      <div class="tree-wrap">
        <el-tree
          ref="treeRef"
          :data="menuTree"
          :props="{ label: 'name', children: 'children' }"
          node-key="id"
          show-checkbox
          default-expand-all
          :check-strictly="false"
        >
          <template #default="{ data }">
            <span class="tree-node">
              <span>{{ data.name }}</span>
              <el-tag v-if="data.perms" size="small" type="info" effect="plain">{{ data.perms }}</el-tag>
            </span>
          </template>
        </el-tree>
      </div>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="submitAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, ElTree } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { menuApi, roleApi } from '@/api'
import { useUserStore } from '@/stores/user'
import type { MenuVo, SysRole } from '@/types/api'

const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const assigning = ref(false)
const keyword = ref('')
const roles = ref<SysRole[]>([])

const formVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<SysRole>({
  name: '',
  code: '',
  description: '',
  dataScope: 'ALL',
  sort: 0,
  status: 1
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_]{1,29}$/, message: '小写字母开头，只能包含小写字母、数字、下划线', trigger: 'blur' }
  ]
}

const assignVisible = ref(false)
const assignRole = ref<SysRole | null>(null)
const menuTree = ref<MenuVo[]>([])
const treeRef = ref<InstanceType<typeof ElTree>>()

const filteredRoles = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return roles.value
  return roles.value.filter(
    r => (r.name || '').toLowerCase().includes(kw) || (r.code || '').toLowerCase().includes(kw)
  )
})

function scopeLabel(scope?: string) {
  return { ALL: '全部数据', STORE: '本门店', SELF: '仅本人' }[scope || 'ALL'] || scope
}

async function loadRoles() {
  loading.value = true
  try {
    const res = await roleApi.list()
    roles.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

function openForm(row?: SysRole) {
  Object.assign(form, {
    id: row?.id,
    name: row?.name || '',
    code: row?.code || '',
    description: row?.description || '',
    dataScope: row?.dataScope || 'ALL',
    sort: row?.sort ?? 0,
    status: row?.status ?? 1
  })
  formVisible.value = true
  formRef.value?.clearValidate()
}

async function submitForm() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    await roleApi.save({ ...form })
    ElMessage.success('保存成功')
    formVisible.value = false
    await loadRoles()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: SysRole, enabled: boolean) {
  await roleApi.updateStatus(row.id!, enabled ? 1 : 0)
  row.status = enabled ? 1 : 0
  ElMessage.success(enabled ? '已启用' : '已禁用')
}

async function remove(row: SysRole) {
  await ElMessageBox.confirm(`确定删除角色「${row.name}」吗？`, '提示', { type: 'warning' })
  await roleApi.remove(row.id!)
  ElMessage.success('已删除')
  await loadRoles()
}

async function openAssign(row: SysRole) {
  assignRole.value = row
  assignVisible.value = true
  if (!menuTree.value.length) {
    const res = await menuApi.tree()
    menuTree.value = res.data.data || []
  }
  const res = await roleApi.menuIds(row.id!)
  const ids = res.data.data || []
  // 等 tree 渲染完再勾选
  await Promise.resolve()
  treeRef.value?.setCheckedKeys(ids, false)
}

async function submitAssign() {
  if (!assignRole.value || !treeRef.value) return
  assigning.value = true
  try {
    // 父子联动模式下，半选的父节点也要一并提交，否则父目录不会被授权
    const checked = treeRef.value.getCheckedKeys() as number[]
    const half = treeRef.value.getHalfCheckedKeys() as number[]
    await roleApi.assignMenu(assignRole.value.id!, [...checked, ...half])
    ElMessage.success('权限已保存，相关账号重新登录后生效')
    assignVisible.value = false
  } finally {
    assigning.value = false
  }
}

onMounted(loadRoles)
</script>

<style scoped>
.role-page {
  padding: 16px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.tree-wrap {
  max-height: 460px;
  overflow: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 8px;
}
.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
</style>
