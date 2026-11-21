<template>
  <div class="login-container">
    <!-- 左侧品牌区（窄屏隐藏） -->
    <div class="login-left">
      <div class="brand">
        <img class="brand-logo" src="/logo.svg" alt="归心寓" />
        <h1 class="brand-title">归心寓</h1>
        <p class="brand-sub">GUIXINYU · 后台管理系统</p>
        <ul class="brand-points">
          <li>公寓 / 房间 / 预约 / 租约一体化管理</li>
          <li>省市区、标签、配套、杂费全维度配置</li>
          <li>实时掌握房间入住状态</li>
        </ul>
      </div>
    </div>

    <!-- 右侧登录卡 -->
    <div class="login-right">
      <el-form
        ref="ruleFormRef"
        :model="ruleForm"
        status-icon
        :rules="rules"
        class="login-form"
        @keyup.enter="submitForm"
      >
        <div class="form-header">
          <div class="title">hello !</div>
          <div class="title-tips">欢迎来到归心寓后台管理！</div>
        </div>

        <el-form-item prop="username">
          <el-input
            v-model.trim="ruleForm.username"
            :prefix-icon="User"
            autocomplete="off"
            placeholder="请输入用户名"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model.trim="ruleForm.password"
            type="password"
            show-password
            :prefix-icon="Lock"
            autocomplete="off"
            placeholder="请输入密码"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="captchaCode">
          <el-row :gutter="10" style="width: 100%">
            <el-col :span="15">
              <el-input
                v-model.trim="ruleForm.captchaCode"
                autocomplete="off"
                placeholder="请输入验证码"
                maxlength="6"
                size="large"
              />
            </el-col>
            <el-col :span="9">
              <div class="captcha-box" @click="getCaptcha" title="点击刷新">
                <img v-if="captcha.image" :src="captcha.image" alt="验证码" />
                <el-icon v-else class="is-loading"><Loading /></el-icon>
              </div>
            </el-col>
          </el-row>
        </el-form-item>

        <el-form-item>
          <el-button class="login-btn" type="primary" size="large" :loading="loading" @click="submitForm">
            登 录
          </el-button>
        </el-form-item>

        <div class="form-tip">默认账号 admin / 密码 123456（需先执行 sql/lease.sql）</div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Loading, Lock, User } from '@element-plus/icons-vue'
import { authApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { useTabsStore } from '@/stores/tabs'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const tabsStore = useTabsStore()

const ruleFormRef = ref<FormInstance>()
const loading = ref(false)
const captcha = ref({ image: '', key: '' })

const ruleForm = reactive({
  username: 'admin',
  password: '123456',
  captchaKey: '',
  captchaCode: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

onMounted(getCaptcha)

async function getCaptcha() {
  captcha.value = { image: '', key: '' }
  try {
    const res = await authApi.captcha()
    const data = res.data.data
    captcha.value = { image: data.image, key: data.key }
    ruleForm.captchaKey = data.key
    ruleForm.captchaCode = ''
  } catch {
    /* 拦截器已提示 */
  }
}

async function submitForm() {
  if (!ruleFormRef.value) return
  const valid = await ruleFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await authApi.login({
      username: ruleForm.username,
      password: ruleForm.password,
      captchaKey: ruleForm.captchaKey,
      captchaCode: ruleForm.captchaCode
    })
    const token = res.data.data
    if (!token) {
      ElMessage.error('登录失败')
      return
    }
    userStore.setToken(token)
    tabsStore.reset()

    try {
      const infoRes = await authApi.info()
      if (infoRes.data.data) userStore.setUserInfo(infoRes.data.data)
    } catch {
      /* 信息拿不到不影响进入系统 */
    }

    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.replace(redirect)
  } catch {
    // 验证码一次性，失败后刷新
    getCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at 15% 20%, rgb(224 122 95 / 30%), transparent 45%),
    radial-gradient(circle at 85% 75%, rgb(230 213 195 / 22%), transparent 45%),
    linear-gradient(135deg, #3e3229 0%, #5d4b3c 48%, #8a7057 100%);
}

/* ---------- 左侧品牌 ---------- */
.login-left {
  flex: 1.5;
  display: flex;
  align-items: center;
  padding: 0 8vw;
  color: #fff;
}
.brand-logo {
  width: 104px;
  height: 104px;
  display: block;
  margin-bottom: 12px;
}
.brand-title {
  margin: 0;
  font-size: 46px;
  font-weight: 700;
  letter-spacing: 6px;
}
.brand-sub {
  margin: 12px 0 0;
  font-size: 13px;
  letter-spacing: 2px;
  color: rgb(255 255 255 / 65%);
}
.brand-points {
  margin: 40px 0 0;
  padding: 0;
  list-style: none;
  font-size: 14px;
  line-height: 2.4;
  color: rgb(255 255 255 / 78%);
}
.brand-points li::before {
  content: '·';
  margin-right: 8px;
  color: #f2cc8f;
  font-weight: 700;
}

/* ---------- 右侧表单 ---------- */
.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-form {
  width: 100%;
  max-width: 400px;
  padding: 44px 38px 32px;
  border: 1px solid rgb(255 255 255 / 18%);
  border-radius: 10px;
  background: rgb(255 255 255 / 10%);
  backdrop-filter: blur(14px);
  box-shadow: 0 8px 32px rgb(0 0 0 / 28%);
}

.form-header {
  margin-bottom: 30px;
}
.form-header .title {
  font-size: 54px;
  font-weight: 500;
  line-height: 1.1;
  color: #fff;
}
.form-header .title-tips {
  margin-top: 14px;
  font-size: 18px;
  font-weight: 400;
  color: rgb(255 255 255 / 90%);
  white-space: nowrap;
}

.login-form :deep(.el-form-item) {
  margin: 20px 0;
}
.login-form :deep(.el-input__wrapper) {
  height: 48px;
  border-radius: 4px;
  background: rgb(255 255 255 / 92%);
  box-shadow: none;
}
.login-form :deep(.el-input__inner) {
  font-size: 14px;
  color: #454545;
}
.login-form :deep(.el-form-item__error) {
  padding-left: 4px;
  color: #ffd2c4;
}

.captcha-box {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #fffdfa;
  border-radius: 4px;
  cursor: pointer;
}
.captcha-box img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  letter-spacing: 4px;
  border: 0;
  border-radius: 4px;
}
.login-btn:hover {
  opacity: 0.9;
}

.form-tip {
  margin-top: 6px;
  font-size: 12px;
  text-align: center;
  color: rgb(255 255 255 / 55%);
  line-height: 1.6;
}

/* 窄屏：隐藏左侧 */
@media (max-width: 992px) {
  .login-left {
    display: none;
  }
  .login-right {
    flex: 1;
  }
}
</style>
