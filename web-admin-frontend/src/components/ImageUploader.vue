<template>
  <div class="image-wall">
    <div class="image-item" v-for="(img, index) in modelValue" :key="index">
      <img :src="resolveUrl(img.url)" :alt="img.name || ''" />
      <div class="remove" @click="remove(index)">×</div>
    </div>

    <div class="image-add" v-if="modelValue.length < limit" @click="pick">
      <el-icon v-if="!uploading"><Plus /></el-icon>
      <el-icon v-else class="is-loading"><Loading /></el-icon>
    </div>

    <input ref="fileInput" type="file" accept="image/*" multiple hidden @change="onChange" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fileApi } from '@/api'
import type { GraphVo } from '@/types/api'

const props = withDefaults(
  defineProps<{
    modelValue: GraphVo[]
    limit?: number
  }>(),
  { limit: 8 }
)

const emit = defineEmits<{
  'update:modelValue': [value: GraphVo[]]
}>()

const fileInput = ref<HTMLInputElement>()
const uploading = ref(false)

function resolveUrl(url?: string) {
  if (!url) return ''
  if (/^(https?:)?\/\//i.test(url) || url.startsWith('data:')) return url
  return url.startsWith('/') ? url : `/${url}`
}

function pick() {
  fileInput.value?.click()
}

async function onChange(event: Event) {
  const target = event.target as HTMLInputElement
  const files = Array.from(target.files || [])
  if (!files.length) return

  uploading.value = true
  const next = [...props.modelValue]
  try {
    for (const file of files) {
      if (next.length >= props.limit) {
        ElMessage.warning(`最多只能上传 ${props.limit} 张图片`)
        break
      }
      if (!file.type.startsWith('image/')) {
        ElMessage.warning(`${file.name} 不是图片文件，已跳过`)
        continue
      }
      const res = await fileApi.upload(file)
      const url = res.data.data
      if (url) {
        next.push({ name: file.name, url })
      }
    }
    emit('update:modelValue', next)
  } catch {
    /* 拦截器已提示 */
  } finally {
    uploading.value = false
    target.value = ''
  }
}

function remove(index: number) {
  const next = [...props.modelValue]
  next.splice(index, 1)
  emit('update:modelValue', next)
}
</script>
