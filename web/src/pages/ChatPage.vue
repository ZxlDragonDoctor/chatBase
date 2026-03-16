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
      <input
        v-model="input"
        class="input"
        placeholder="输入你的问题…"
        :disabled="loading"
      />
      <button class="btn" type="submit" :disabled="loading || !input.trim()">
        {{ loading ? '发送中…' : '发送' }}
      </button>
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
import type { RetrieverResource } from '../types/dify'

type ChatMsg = { role: 'user' | 'assistant'; text: string; sources?: RetrieverResource[] }

const userId = getOrCreateUserId()

const input = ref('')
const loading = ref(false)
const error = ref<string | null>(null)
const messages = ref<ChatMsg[]>([])

function reset() {
  messages.value = []
  error.value = null
  input.value = ''
}

async function send() {
  const text = input.value.trim()
  if (!text) return

  error.value = null
  messages.value.push({ role: 'user', text })
  input.value = ''
  loading.value = true

  try {
    const resp = await webChat(text, userId)
    messages.value.push({
      role: 'assistant',
      text: resp.answer || '（无返回）',
      sources: resp.retrieverResources || [],
    })
  } catch (e: any) {
    error.value = e?.message || '请求失败'
    messages.value.push({ role: 'assistant', text: '【系统错误】请求失败，请稍后再试' })
  } finally {
    loading.value = false
  }
}
</script>

