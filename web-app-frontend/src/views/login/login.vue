<template>
  <div class="main-container h-[100vh]">
    <div class="h-[28vh] flex flex-col justify-center items-center">
      <van-image width="96px" height="96px" src="/logo.svg" />
      <div class="mt-[10px] text-[18px] font-bold text-[--van-primary-color] tracking-[3px]">
        归心寓
      </div>
    </div>

    <div>
      <van-form ref="formRef">
        <van-cell-group inset>
          <!--            账号-->
          <van-field
            v-model.trim="form.username"
            border
            name="username"
            required
            maxlength="20"
            autocomplete="off"
            placeholder="请输入账号"
            :rules="usernameRules"
          />

          <template v-if="isRegister">
            <!--            手机号-->
            <van-field
              v-model.trim="form.phone"
              border
              name="phone"
              required
              type="tel"
              maxlength="11"
              autocomplete="off"
              placeholder="请输入手机号"
              :rules="phoneRules"
            />

            <!--            昵称（选填）-->
            <van-field
              v-model.trim="form.nickname"
              border
              name="nickname"
              maxlength="20"
              autocomplete="off"
              placeholder="昵称（选填，默认同账号）"
            />
          </template>

          <!--            密码-->
          <van-field
            v-model.trim="form.password"
            name="password"
            required
            type="password"
            autocomplete="off"
            placeholder="请输入密码（6~20 位）"
            :rules="passwordRules"
            @keyup.enter="onSubmitHandle"
          />

          <!--            确认密码（仅注册）-->
          <van-field
            v-if="isRegister"
            v-model.trim="form.confirmPassword"
            name="confirmPassword"
            required
            type="password"
            autocomplete="off"
            placeholder="请再次输入密码"
            :rules="confirmRules"
            @keyup.enter="onSubmitHandle"
          />
        </van-cell-group>

        <div class="mt-[32px]">
          <loading-button
            round
            block
            type="primary"
            native-type="submit"
            :loadingClick="onSubmitHandle"
          >
            {{ isRegister ? "注 册" : "登 录" }}
          </loading-button>
        </div>

        <div class="mt-[16px] text-center text-[13px]">
          <span class="--van-gray-6">{{ isRegister ? "已有账号？" : "还没有账号？" }}</span>
          <span class="ml-[4px] text-[--van-primary-color]" @click="switchMode">
            {{ isRegister ? "去登录" : "立即注册" }}
          </span>
        </div>

        <div v-if="!isRegister" class="mt-[14px] text-center text-[12px] --van-gray-5">
          测试账号：zhangsan / lisi / wangwu，密码均为 123456
        </div>
      </van-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { FormInstance } from "vant";
import { useUserStore } from "@/store/modules/user";
import LoadingButton from "@/components/LoadingButton/LoadingButton.vue";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

/** 当前模式：登录 / 注册 */
const mode = ref<"login" | "register">("login");
const isRegister = computed(() => mode.value === "register");

// 表单数据
const form = ref({
  username: "zhangsan",
  password: "123456",
  phone: "",
  nickname: "",
  confirmPassword: ""
});

// 表单实例
const formRef = ref<FormInstance>();

const usernameRules = [
  { required: true, message: "请输入账号" },
  { pattern: /^[a-zA-Z0-9_]{3,20}$/, message: "账号为 3~20 位字母、数字或下划线" }
];

const phoneRules = [
  { required: true, message: "请输入手机号" },
  { pattern: /^1[3-9]\d{9}$/, message: "手机号格式不正确" }
];

const passwordRules = [
  { required: true, message: "请输入密码" },
  { pattern: /^.{6,20}$/, message: "密码长度 6~20 位" }
];

const confirmRules = [
  {
    // 返回 true 表示通过，返回字符串表示错误提示
    validator: (value: string) => {
      if (!isRegister.value) return true;
      if (!value) return "请再次输入密码";
      return value === form.value.password || "两次输入的密码不一致";
    }
  }
];

function switchMode() {
  mode.value = isRegister.value ? "login" : "register";
  if (mode.value === "register") {
    form.value.phone = "";
    form.value.nickname = "";
    form.value.confirmPassword = "";
  }
}

const onSubmitHandle = async () => {
  // 验证字段
  await formRef.value?.validate();

  if (isRegister.value) {
    await userStore.RegisterAction({
      username: form.value.username,
      password: form.value.password,
      phone: form.value.phone,
      nickname: form.value.nickname || undefined
    });
  } else {
    await userStore.LoginAction({
      username: form.value.username,
      password: form.value.password
    });
  }

  // 跳转首页
  await router.replace(
    route.query?.redirect ? decodeURIComponent(route.query.redirect as string) : "/"
  );
};
</script>

<style scoped lang="less"></style>
