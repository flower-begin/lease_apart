import type { App, Directive } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 按钮级权限指令。
 *
 * 用法：
 *   <el-button v-perm="'apartment:save'">新增</el-button>
 *   <el-button v-perm="['system:role:save', 'system:role:assign']">保存</el-button>
 *
 * 没有权限时直接把元素从 DOM 移除（不是隐藏），避免被审查元素改出来。
 */
const perm: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    const store = useUserStore()
    if (!store.hasPerm(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  }
}

export function setupPermissionDirective(app: App) {
  app.directive('perm', perm)
}

export default perm
