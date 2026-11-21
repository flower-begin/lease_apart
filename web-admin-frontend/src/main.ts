import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import { setupPermissionDirective } from './directives/perm'
import './styles/index.css'

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
// v-perm 按钮级权限指令，必须在 pinia 之后（指令内部会读 userStore）
setupPermissionDirective(app)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
