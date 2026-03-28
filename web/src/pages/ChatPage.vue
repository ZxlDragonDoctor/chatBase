<template>
  <section class="card">
    <div class="cardHeader">
      <div>
        <div class="h1">聊天</div>
        <div class="muted">调用后端接口：<code>/api/chat/web</code></div>
      </div>
      <div class="right">
        <div class="pill">userId: <code>{{ userId }}</code></div>
        <button class="btn btnGhost" @click="reset">清空</button>
      </div>
    </div>

    <div class="chat">
      <div v-if="messages.length === 0" class="empty">
        请输入问题开始对话。
      </div>
      <div v-for="(m, idx) in messages" :key="idx" class="msgRow" :class="m.role">
        <div class="avatar">{{ m.role === 'user' ? '我' : 'AI' }}</div>
        <div class="bubble">
          <div class="content">{{ m.text }}</div>
          <div v-if="m.sources?.length" class="sources">
            <div class="sourcesTitle">引用</div>
            <div v-for="(s, i) in m.sources" :key="i" class="sourceItem">
              <div class="sourceMeta">
                <span class="badge">{{ s.datasetName || s.datasetId || 'dataset' }}</span>
                <span class="badge">{{ s.documentName || s.documentId || 'doc' }}</span>
                <span v-if="typeof s.score === 'number'" class="badge">score: {{ s.score.toFixed(3) }}</span>
              </div>
              <div class="sourceText">{{ s.content }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <form class="composer" @submit.prevent="send">
      <div v-if="menuOpen" class="menuOverlay" aria-hidden="true" @click="closePlusMenu" />
      <div v-if="pendingFiles.length" class="fileTags">
        <span v-for="(f, i) in pendingFiles" :key="i" class="badge">
          {{ f.transferMethod === 'local_file' ? `本地:${f.uploadFileId?.slice(0, 8)}…` : `链接:${truncate(f.url, 24)}` }}
        </span>
        <button class="btn btnGhost" type="button" @click="pendingFiles = []">清空附件</button>
      </div>
      <div v-if="urlPanelOpen" class="urlPanel">
        <input
          v-model="urlInput"
          class="input urlPanelInput"
          placeholder="粘贴文件或图片的直链 URL（https://…）"
          :disabled="loading"
          @keyup.enter.prevent="confirmUrlAndClose"
        />
        <button class="btn btnGhost" type="button" :disabled="!urlInput.trim() || loading" @click="confirmUrlAndClose">
          添加链接
        </button>
        <button class="btn btnGhost" type="button" @click="urlPanelOpen = false">取消</button>
      </div>
      <div class="composerRow">
        <div class="plusWrap">
          <button
            type="button"
            class="plusBtn"
            :aria-expanded="menuOpen"
            title="添加附件"
            @click.stop="togglePlusMenu"
          >
            +
          </button>
          <div v-show="menuOpen" class="plusMenu" role="menu">
            <button type="button" class="plusMenuItem" role="menuitem" @click="pickLocalFile">
              <span class="plusMenuIco">📎</span>
              <span>文件和图片</span>
            </button>
            <button type="button" class="plusMenuItem" role="menuitem" @click="openUrlPanelFromMenu">
              <span class="plusMenuIco">🔗</span>
              <span>链接地址</span>
            </button>
          </div>
        </div>
        <input
          ref="fileInputRef"
          type="file"
          class="fileHidden"
          :disabled="loading"
          @change="uploadLocalFile"
        />
        <input
          v-model="input"
          class="input inputMain"
          placeholder="尽管问，带图也行"
          :disabled="loading"
        />
        <button class="btn" type="submit" :disabled="loading || !input.trim()">
          {{ loading ? '发送中…' : '发送' }}
        </button>
      </div>
      <p class="attachHint">支持本地上传与 URL；通过「+」选择添加方式。</p>
    </form>

    <div v-if="error" class="error">
      {{ error }}
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { webChat } from '../api/chat'
import { getOrCreateUserId } from '../lib/user'
import { uploadFile } from '../api/upload'
import type { ChatFileInfo, RetrieverResource } from '../types/dify'

type ChatMsg = { role: 'user' | 'assistant'; text: string; sources?: RetrieverResource[] }

const userId = getOrCreateUserId()

const input = ref('')
const urlInput = ref('')
const loading = ref(false)
const error = ref<string | null>(null)
const messages = ref<ChatMsg[]>([])
const pendingFiles = ref<ChatFileInfo[]>([])
const menuOpen = ref(false)
const urlPanelOpen = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)

function truncate(s: string | undefined, max: number) {
  if (!s) return ''
  return s.length > max ? `${s.slice(0, max)}…` : s
}

function togglePlusMenu() {
  menuOpen.value = !menuOpen.value
}

function closePlusMenu() {
  menuOpen.value = false
}

function pickLocalFile() {
  closePlusMenu()
  fileInputRef.value?.click()
}

function openUrlPanelFromMenu() {
  closePlusMenu()
  urlPanelOpen.value = true
}

function confirmUrlAndClose() {
  addUrlFile()
  urlPanelOpen.value = false
}

function reset() {
  messages.value = []
  error.value = null
  input.value = ''
  urlInput.value = ''
  pendingFiles.value = []
  menuOpen.value = false
  urlPanelOpen.value = false
}

async function send() {
  const text = input.value.trim()
  if (!text) return

  error.value = null
  messages.value.push({ role: 'user', text })
  input.value = ''
  loading.value = true

  try {
    const files = [...pendingFiles.value]
    const resp = await webChat(text, userId, files)
    messages.value.push({
      role: 'assistant',
      text: resp.answer || '（无返回）',
      sources: resp.retrieverResources || [],
    })
    pendingFiles.value = []
  } catch (e: any) {
    error.value = e?.message || '请求失败'
    messages.value.push({ role: 'assistant', text: '【系统错误】请求失败，请稍后再试' })
  } finally {
    loading.value = false
  }
}

function addUrlFile() {
  const u = urlInput.value.trim()
  if (!u) return
  pendingFiles.value.push({
    type: guessFileTypeFromUrl(u),
    transferMethod: 'remote_url',
    url: u,
  })
  urlInput.value = ''
}

async function uploadLocalFile(e: Event) {
  const el = e.target as HTMLInputElement
  const file = el.files?.[0]
  if (!file) return
  loading.value = true
  error.value = null
  try {
    const resp = await uploadFile(file, userId)
    if (resp.id) {
      pendingFiles.value.push({
        type: guessFileTypeFromName(file.name),
        transferMethod: 'local_file',
        uploadFileId: resp.id,
      })
    }
  } catch (err: any) {
    error.value = err?.message || '上传失败'
  } finally {
    loading.value = false
    el.value = ''
  }
}

function guessFileTypeFromName(name: string): ChatFileInfo['type'] {
  const n = name.toLowerCase()
  if (/\.(png|jpg|jpeg|gif|webp|bmp)$/.test(n)) return 'image'
  if (/\.(mp3|wav|m4a|aac|ogg)$/.test(n)) return 'audio'
  if (/\.(mp4|mov|mkv|webm)$/.test(n)) return 'video'
  return 'document'
}

function guessFileTypeFromUrl(url: string): ChatFileInfo['type'] {
  try {
    const path = new URL(url).pathname
    return guessFileTypeFromName(path)
  } catch {
    return 'document'
  }
}
</script>

