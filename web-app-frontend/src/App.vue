<template>
  <div id="app">
    <router-view />
    <van-tabbar v-if="showTabbar" route>
      <van-tabbar-item to="/home" icon="home-o" name="home">首页</van-tabbar-item>
      <van-tabbar-item to="/apartment" icon="search" name="apartment">找房</van-tabbar-item>
      <van-tabbar-item to="/ai" icon="chat-o" name="ai">AI助手</van-tabbar-item>
      <van-tabbar-item to="/profile" icon="user-o" name="profile">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const showTabbar = computed(() => {
  const hidePaths = ['/login']
  return !hidePaths.includes(route.path)
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
html, body, #app {
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: #f7f8fa;
}
#app {
  padding-bottom: 50px;
}
</style>
