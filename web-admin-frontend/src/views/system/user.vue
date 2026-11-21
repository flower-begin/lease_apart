<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-toolbar">
        <el-input v-model="query.name" placeholder="姓名" clearable style="width: 160px" @keyup.enter="search" />
        <el-input v-model="query.phone" placeholder="手机号" clearable style="width: 160px" @keyup.enter="search" />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <div class="spacer"></div>
        <el-button type="primary" :icon="Plus" @click="openForm()">新增员工</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="头像" width="76" align="center">
          <template #default="{ row }">
            <el-avatar :size="32" :src="resolveUrl(row.avatarUrl)">
              {{ (row.name || 'U').slice(0, 1) }}
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="账号" width="130" />
        <el-table-column prop="name" label="姓名" width="110" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="dictType(SYSTEM_USER_TYPE, row.type)" size="small">
              {{ dictLabel(SYSTEM_USER_TYPE, row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="postName" label="岗位" width="110">
          <template #default="{ row }">{{ row.postName || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="dictType(BASE_STATUS, row.status)" size="small">
              {{ dictLabel(BASE_STATUS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button v-perm="'system:user:assign'" link type="primary" @click="openRole(row)">角色</el-button>
            <el-button link :type="Number(row.status) === 1 ? 'danger' : 'success'" @click="toggle(row)">
              {{ Number(row.status) === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <el-drawer v-model="formVisible" :title="form.id ? '编辑员工' : '新增员工'" size="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" maxlength="30" placeholder="登录账号" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="密码" :prop="form.id ? '' : 'password'">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            maxlength="30"
            :placeholder="form.id ? '留空则不修改密码' : '请输入初始密码'"
          />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" maxlength="30" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="员工类型">
          <el-radio-group v-model="form.type">
            <el-radio :value="0">管理员</el-radio>
            <el-radio :value="1">普通员工</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="form.postId" clearable placeholder="请选择岗位" style="width: 100%">
            <el-option v-for="p in posts" :key="p.id" :label="p.name" :value="p.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.additionalInfo" type="textarea" :rows="2" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-drawer>

    <!-- 分配角色 -->
    <el-dialog v-model="roleVisible" :title="`分配角色 · ${roleUser?.name || roleUser?.username || ''}`" width="460px">
      <el-checkbox-group v-model="selectedRoleIds" class="role-list">
        <el-checkbox v-for="r in roleOptions" :key="r.id" :value="r.id!" class="role-item">
          <span class="role-name">{{ r.name }}</span>
          <el-tag size="small" type="info" effect="plain">{{ r.code }}</el-tag>
        </el-checkbox>
      </el-checkbox-group>
      <div class="role-hint">角色决定该账号登录后能看到哪些菜单和按钮，保存后需重新登录生效。</div>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSaving" @click="submitRole">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { roleApi, systemPostApi, systemUserApi, userRoleApi } from '@/api'
import { BASE_STATUS, SYSTEM_USER_TYPE, dictLabel, dictType } from '@/utils/dict'
import type { SysRole, SystemPost, SystemUser } from '@/types/api'

/* ---------------------------------------------------- 分配角色（RBAC） */
const roleVisible = ref(false)
const roleSaving = ref(false)
const roleUser = ref<SystemUser | null>(null)
const roleOptions = ref<SysRole[]>([])
const selectedRoleIds = ref<number[]>([])

async function openRole(row: SystemUser) {
  roleUser.value = row
  roleVisible.value = true
  if (!roleOptions.value.length) {
    const res = await roleApi.list()
    roleOptions.value = res.data.data || []
  }
  const res = await userRoleApi.roleIds(row.id!)
  selectedRoleIds.value = res.data.data || []
}

async function submitRole() {
  if (!roleUser.value) return
  roleSaving.value = true
  try {
    await userRoleApi.assign(roleUser.value.id!, selectedRoleIds.value)
    ElMessage.success('角色已保存，该账号重新登录后生效')
    roleVisible.value = false
  } finally {
    roleSaving.value = false
  }
}

const loading = ref(false)
const saving = ref(false)
const total = ref(0)
const rows = ref<SystemUser[]>([])
const posts = ref<SystemPost[]>([])

const formVisible = ref(false)
const formRef = ref<FormInstance>()

const query = reactive({
  current: 1,
  size: 10,
  name: '',
  phone: ''
})

const form = reactive<SystemUser>({
  id: undefined,
  username: '',
  password: '',
  name: '',
  type: 1,
  phone: '',
  postId: undefined,
  status: 1,
  additionalInfo: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 30, message: '账号长度 3~30 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度 6~30 位', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

onMounted(async () => {
  try {
    const res = await systemPostApi.list()
    posts.value = res.data.data || []
  } catch {
    /* ignore */
  }
  await load()
})

async function load() {
  loading.value = true
  try {
    const res = await systemUserApi.page({ ...query })
    const data = res.data.data
    rows.value = data?.records || []
    total.value = data?.total || 0
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.current = 1
  load()
}

function reset() {
  query.current = 1
  query.name = ''
  query.phone = ''
  load()
}

function resolveUrl(url?: string) {
  if (!url) return ''
  if (/^(https?:)?\/\//i.test(url) || url.startsWith('data:')) return url
  return url.startsWith('/') ? url : `/${url}`
}

function openForm(row?: SystemUser) {
  formVisible.value = true
  Object.assign(form, {
    id: undefined,
    username: '',
    password: '',
    name: '',
    type: 1,
    phone: '',
    postId: undefined,
    status: 1,
    additionalInfo: ''
  })
  if (row) {
    Object.assign(form, {
      id: row.id,
      username: row.username,
      password: '',
      name: row.name,
      type: Number(row.type ?? 1),
      phone: row.phone,
      postId: row.postId,
      status: Number(row.status ?? 1),
      additionalInfo: row.additionalInfo
    })
  }
  formRef.value?.clearValidate()
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    // 编辑时不填密码就不提交该字段，避免把原密码覆盖成空
    const payload: SystemUser = { ...form }
    if (form.id && !form.password) {
      delete payload.password
    }
    await systemUserApi.save(payload)
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    saving.value = false
  }
}

async function toggle(row: SystemUser) {
  const next = Number(row.status) === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(`确定要${next === 0 ? '禁用' : '启用'}员工「${row.name}」吗？`, '提示', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await systemUserApi.updateStatus(row.id!, next)
    ElMessage.success('操作成功')
    load()
  } catch {
    /* 拦截器已提示 */
  }
}

async function remove(row: SystemUser) {
  try {
    await ElMessageBox.confirm(`确定删除员工「${row.name}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await systemUserApi.remove(row.id!)
    ElMessage.success('已删除')
    load()
  } catch {
    /* 拦截器已提示 */
  }
}
</script>

<style scoped>
.role-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.role-item {
  height: 32px;
  margin-right: 0;
}
.role-name {
  margin-right: 8px;
}
.role-hint {
  margin-top: 14px;
  font-size: 12px;
  color: #9c928a;
  line-height: 1.6;
}
</style>
