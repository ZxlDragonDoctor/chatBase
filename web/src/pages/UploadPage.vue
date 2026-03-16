<template>
  <section class="card">
    <div class="cardHeader">
      <div>
        <div class="h1">上传</div>
        <div class="muted">调用后端接口：<code>/api/chat/v1/files/upload</code></div>
      </div>
      <div class="right">
        <div class="pill">user: <code>{{ userId }}</code></div>
      </div>
    </div>

    <div class="upload">
      <input class="file" type="file" @change="onPick" />
      <button class="btn" :disabled="!file || loading" @click="doUpload">
        {{ loading ? '上传中…' : '上传到 Dify' }}
      </button>
    </div>

    <div v-if="result" class="result">
      <div class="h2">上传结果</div>
      <pre class="pre">{{ JSON.stringify(result, null, 2) }}</pre>
    </div>

    <div v-if="error" class="error">
      {{ error }}
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { getOrCreateUserId } from '../lib/user'
import { uploadFile } from '../api/upload'
import type { DifyFileUploadResponse } from '../types/dify'

const userId = getOrCreateUserId()

const file = ref<File | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const result = ref<DifyFileUploadResponse | null>(null)

function onPick(e: Event) {
  const input = e.target as HTMLInputElement
  file.value = input.files?.[0] || null
  result.value = null
  error.value = null
}

async function doUpload() {
  if (!file.value) return
  loading.value = true
  error.value = null
  result.value = null
  try {
    result.value = await uploadFile(file.value, userId)
  } catch (e: any) {
    error.value = e?.message || '上传失败'
  } finally {
    loading.value = false
  }
}
</script>

