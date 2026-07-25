<template>
  <div class="login">
    <van-nav-bar title="登录" />
    <div class="login-form">
      <van-field v-model="phone" type="tel" label="手机号" placeholder="请输入手机号" maxlength="11" />
      <van-field v-model="code" type="digit" label="验证码" placeholder="请输入验证码">
        <template #button>
          <van-button size="small" type="primary" :disabled="countdown > 0" @click="sendCode">
            {{ countdown > 0 ? `${countdown}秒后重发` : '获取验证码' }}
          </van-button>
        </template>
      </van-field>
      <van-button type="primary" block round :loading="loading" @click="handleLogin" style="margin-top: 30px;">
        登录
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getCode, login } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const phone = ref('')
const code = ref('')
const countdown = ref(0)
const loading = ref(false)

function sendCode() {
  if (!/^1[3-9]\d{9}$/.test(phone.value)) {
    showToast('请输入正确的手机号')
    return
  }
  getCode(phone.value).then(() => {
    showToast('验证码已发送')
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  }).catch(() => showToast('发送失败'))
}

async function handleLogin() {
  if (!phone.value || !code.value) {
    showToast('请填写完整信息')
    return
  }
  loading.value = true
  try {
    const res = await login(phone.value, code.value)
    const token = res.data.data
    if (token) {
      userStore.setToken(token)
      userStore.setUserInfo({ nickname: '用户' + phone.value.slice(-4), avatarUrl: '' })
      showToast('登录成功')
      router.replace('/home')
    } else {
      showToast('登录失败')
    }
  } catch {
    showToast('登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login { min-height: 100vh; background: #f7f8fa; }
.login-form { padding: 30px 20px; }
</style>
