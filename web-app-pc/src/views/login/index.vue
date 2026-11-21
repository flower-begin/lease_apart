<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <img class="brand-badge" src="/logo.svg" alt="归心寓" />
        <h1 class="brand-title">归心寓</h1>
        <p class="brand-sub">找房 · 预约看房 · 在线签约</p>
        <p class="brand-tagline">WARMTH · HEALING · BELONGING</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent>
        <el-form-item prop="username">
          <el-input
            v-model.trim="form.username"
            placeholder="请输入账号"
            :prefix-icon="User"
            maxlength="20"
            autocomplete="off"
          />
        </el-form-item>

        <template v-if="isRegister">
          <el-form-item prop="phone">
            <el-input
              v-model.trim="form.phone"
              placeholder="请输入手机号"
              :prefix-icon="Iphone"
              maxlength="11"
              autocomplete="off"
            />
          </el-form-item>

          <el-form-item prop="nickname">
            <el-input
              v-model.trim="form.nickname"
              placeholder="昵称（选填，不填就用账号）"
              :prefix-icon="Postcard"
              maxlength="20"
              autocomplete="off"
            />
          </el-form-item>
        </template>

        <el-form-item prop="password">
          <el-input
            v-model.trim="form.password"
            type="password"
            show-password
            placeholder="请输入密码（6~20 位）"
            :prefix-icon="Lock"
            autocomplete="off"
            @keyup.enter="onSubmit"
          />
        </el-form-item>

        <el-form-item v-if="isRegister" prop="confirmPassword">
          <el-input
            v-model.trim="form.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入密码"
            :prefix-icon="Lock"
            autocomplete="off"
            @keyup.enter="onSubmit"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="submit-btn"
            :loading="loading"
            round
            @click="onSubmit"
          >
            {{ isRegister ? '注 册' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="switch">
        <template v-if="isRegister">
          已有账号？
          <el-link type="primary" :underline="false" @click="switchMode('login')">去登录</el-link>
        </template>
        <template v-else>
          还没有账号？
          <el-link type="primary" :underline="false" @click="switchMode('register')">
            立即注册
          </el-link>
        </template>
      </div>

      <div v-if="!isRegister" class="tips">
        <p>测试账号：zhangsan / lisi / wangwu</p>
        <p>密码均为 123456</p>
      </div>

      <div class="back">
        <el-link type="primary" :underline="false" @click="router.push('/search')">
          先随便逛逛 →
        </el-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Iphone, Lock, Postcard, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

/** 当前模式：登录 / 注册 */
const mode = ref<'login' | 'register'>('login')
const isRegister = computed(() => mode.value === 'register')

const form = reactive({
  username: 'zhangsan',
  password: '123456',
  phone: '',
  nickname: '',
  confirmPassword: ''
})

/** 确认密码只在注册时校验 */
function validateConfirm(_rule: unknown, value: string, callback: (error?: Error) => void) {
  if (!isRegister.value) {
    callback()
    return
  }
  if (!value) {
    callback(new Error('请再次输入密码'))
    return
  }
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_]{3,20}$/,
      message: '账号为 3~20 位字母、数字或下划线',
      trigger: 'blur'
    }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: /^.{6,20}$/, message: '密码长度 6~20 位', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
}

function switchMode(next: 'login' | 'register') {
  mode.value = next
  loading.value = false
  // 切模式时清掉注册表单项的校验残留
  formRef.value?.clearValidate()
  if (next === 'register') {
    form.phone = ''
    form.nickname = ''
    form.confirmPassword = ''
  }
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    if (isRegister.value) {
      await userStore.register({
        username: form.username,
        password: form.password,
        phone: form.phone,
        nickname: form.nickname || undefined
      })
      ElMessage.success('注册成功，已自动登录')
    } else {
      await userStore.login({ username: form.username, password: form.password })
      ElMessage.success('登录成功')
    }

    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/search'
    router.replace(redirect)
  } catch {
    // 拦截器已提示具体原因（账号已存在 / 手机号已注册 / 账号或密码错误…）
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background:
    radial-gradient(circle at 12% 18%, rgba(224, 122, 95, 0.16), transparent 42%),
    radial-gradient(circle at 88% 82%, rgba(230, 213, 195, 0.75), transparent 45%),
    linear-gradient(140deg, #fbf8f2 0%, #f4ede3 100%);
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 40px 36px 26px;
  background: var(--bg-card);
  border-radius: 16px;
  border: 1px solid rgba(234, 223, 210, 0.8);
  box-shadow: 0 12px 40px rgba(120, 100, 80, 0.12);
}

.brand {
  text-align: center;
  margin-bottom: 26px;
}

.brand-badge {
  width: 88px;
  height: 88px;
  display: block;
  margin: 0 auto;
}

.brand-title {
  margin: 12px 0 6px;
  font-size: 22px;
  font-weight: 700;
  color: var(--brand);
  letter-spacing: 3px;
}

.brand-sub {
  margin: 0;
  font-size: 13px;
  color: var(--text-3);
  letter-spacing: 1px;
}

.brand-tagline {
  margin: 10px 0 0;
  font-size: 10px;
  letter-spacing: 2.4px;
  color: #bfae97;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 4px;
}

.switch {
  margin-top: 2px;
  text-align: center;
  font-size: 13px;
  color: var(--text-2);
}

.tips {
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #fbf6ef;
  border: 1px solid #eadfd2;
  font-size: 12px;
  color: var(--text-2);
  line-height: 1.8;
  text-align: center;
}

.tips p {
  margin: 0;
}

.back {
  margin-top: 12px;
  text-align: center;
  font-size: 13px;
}
</style>
