import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface TabItem {
  path: string
  title: string
  /** 首页固定，不可关闭 */
  closable: boolean
}

const HOME: TabItem = { path: '/dashboard', title: '首页', closable: false }

export const useTabsStore = defineStore('tabs', () => {
  const tabs = ref<TabItem[]>([{ ...HOME }])
  const activePath = ref(HOME.path)

  function addTab(tab: TabItem) {
    activePath.value = tab.path
    const exists = tabs.value.some(t => t.path === tab.path)
    if (!exists) {
      tabs.value.push(tab)
    }
  }

  /**
   * 关闭标签页，返回关闭后应该跳转的路径（关闭的是当前页时才需要跳）。
   */
  function closeTab(path: string): string | null {
    const index = tabs.value.findIndex(t => t.path === path)
    if (index === -1 || !tabs.value[index].closable) return null

    tabs.value.splice(index, 1)

    if (activePath.value !== path) return null

    // 关闭当前页：优先激活右边一个，没有就取左边一个
    const next = tabs.value[index] ?? tabs.value[index - 1] ?? HOME
    activePath.value = next.path
    return next.path
  }

  function closeOthers(path: string) {
    tabs.value = tabs.value.filter(t => !t.closable || t.path === path)
    activePath.value = path
  }

  function closeAll(): string {
    tabs.value = [{ ...HOME }]
    activePath.value = HOME.path
    return HOME.path
  }

  function reset() {
    tabs.value = [{ ...HOME }]
    activePath.value = HOME.path
  }

  return { tabs, activePath, addTab, closeTab, closeOthers, closeAll, reset }
})
