<template>
  <div class="page-wrap">
    <div class="detail-layout">
      <!-- 左侧资料卡 -->
      <div class="card card-pad profile">
        <el-upload
          class="avatar-upload"
          :show-file-list="false"
          :before-upload="beforeUpload"
          :http-request="onUpload"
          accept="image/*"
        >
          <el-avatar :size="90" :src="form.avatarUrl">
            {{ (form.nickname || 'U').slice(0, 1) }}
          </el-avatar>
          <div class="avatar-tip">点击更换头像</div>
        </el-upload>

        <el-form :model="form" label-width="70px" size="large" class="profile-form">
          <el-form-item label="昵称">
            <el-input v-model.trim="form.nickname" maxlength="20" placeholder="请输入昵称" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input :model-value="userStore.userInfo?.phone || '-'" disabled />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="onSave">保存资料</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 右侧快捷入口 -->
      <div class="main-col">
        <div class="card card-pad">
          <h2 class="section-title">我的</h2>
          <div class="link-grid">
            <div class="link-item" @click="router.push('/appointment/mine')">
              <el-icon><Clock /></el-icon>
              <span>我的预约</span>
            </div>
            <div class="link-item" @click="router.push('/agreement/mine')">
              <el-icon><Document /></el-icon>
              <span>我的租约</span>
            </div>
            <div class="link-item" @click="router.push('/history')">
              <el-icon><Timer /></el-icon>
              <span>浏览历史</span>
            </div>
            <div class="link-item" @click="router.push('/search')">
              <el-icon><Search /></el-icon>
              <span>去找房</span>
            </div>
          </div>
        </div>

        <div class="card card-pad">
          <h2 class="section-title">账号</h2>
          <el-button type="danger" plain @click="onLogout">退出登录</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import { Clock, Document, Search, Timer } from '@element-plus/icons-vue'
import { userApi } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const saving = ref(false)
const form = reactive({
  nickname: '',
  avatarUrl: ''
})

function fillForm() {
  if (userStore.userInfo) {
    form.nickname = userStore.userInfo.nickname || ''
    form.avatarUrl = userStore.userInfo.avatarUrl || ''
  }
}

function beforeUpload(file: File) {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片不能超过 5MB')
    return false
  }
  return true
}

async function onUpload(options: UploadRequestOptions) {
  try {
    const res = await userApi.upload(options.file as File)
    form.avatarUrl = res.data
    ElMessage.success('头像已上传，记得点保存')
  } catch {
    /* 拦截器已提示 */
  }
}

async function onSave() {
  if (!form.nickname) {
    ElMessage.warning('昵称不能为空')
    return
  }
  saving.value = true
  try {
    const res = await userApi.update({ nickname: form.nickname, avatarUrl: form.avatarUrl })
    userStore.userInfo = res.data
    ElMessage.success('保存成功')
  } catch {
    /* 拦截器已提示 */
  } finally {
    saving.value = false
  }
}

async function onLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/search')
}

onMounted(async () => {
  if (!userStore.userInfo) {
    await userStore.fetchInfo()
  }
  fillForm()
})
</script>

<style scoped>
.detail-layout {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.profile {
  text-align: center;
}

.avatar-upload {
  display: inline-block;
  cursor: pointer;
}

.avatar-tip {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-3);
}

.profile-form {
  margin-top: 20px;
  text-align: left;
}

.main-col {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.link-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.link-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 92px;
  border-radius: var(--radius);
  background: #fbf6ee;
  border: 1px solid #eadfd2;
  color: #b3624c;
  cursor: pointer;
  transition: all 0.2s;
}

.link-item:hover {
  background: #f7eee2;
  transform: translateY(-2px);
}

.link-item .el-icon {
  font-size: 22px;
}

.link-item span {
  font-size: 13px;
}

@media (max-width: 900px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
  .link-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
