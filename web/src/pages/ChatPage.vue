<template>
  <div class="anime-page-shell">
    <section class="anime-card anime-chat-card">
      <div class="anime-card-header">
        <div>
          <div class="anime-card-title">Web问答</div>
          <div class="anime-card-desc">
            <span class="anime-pill" style="margin-left: 12px;">
              当前用户: <span class="anime-code">{{ displayUser }}</span>
            </span>
          </div>
        </div>
        <div class="anime-card-actions">
          <button class="anime-btn primary" @click="createNewSession">
            <Plus :size="18" />
            <span>新建对话</span>
          </button>
        </div>
      </div>

      <div class="anime-chat-layout">
        <aside class="anime-chat-sidebar">
          <div class="anime-session-list">
            <div v-if="sessions.length === 0" class="anime-empty" style="padding: 20px;">
              <div class="anime-empty-icon">💬</div>
              <div class="anime-empty-text">暂无对话记录</div>
            </div>
            <template v-for="group in groupedSessions" :key="group.dateKey">
              <div class="anime-session-date">{{ group.dateTitle }}</div>
              <div 
                v-for="s in group.sessions" 
                :key="s.sessionId" 
                class="anime-session-item"
                :class="{ active: currentSession?.sessionId === s.sessionId }"
                @click="switchSession(s)"
              >
                <div class="anime-session-title">{{ s.title || '新对话' }}</div>
                <div class="anime-session-meta">
                  <span>{{ s.messageCount || 0 }} 条消息</span>
                  <span>{{ formatTime(s.lastMessageTime) }}</span>
                </div>
                <button class="anime-session-delete" @click.stop="deleteSession(s)">
                  <Trash2 :size="14" />
                </button>
              </div>
            </template>
          </div>
        </aside>

        <div class="anime-chat-main">
          <div class="anime-chat-messages">
            <div v-if="messages.length === 0" class="anime-empty">
              <div class="anime-empty-icon">💬</div>
              <div class="anime-empty-text">输入问题开始对话 ✿ 支持文件附件 ✿</div>
            </div>
            <div v-for="(m, idx) in messages" :key="idx" class="anime-chat-message anime-fade-in" :class="m.role">
              <div class="anime-chat-avatar">{{ m.role === 'user' ? 'U' : 'AI' }}</div>
              <div class="anime-chat-bubble">
                <template v-if="m.role === 'assistant' && (m.thinkingHtml || getThinkingHtml(m.text))">
                  <div class="thinking-section">
                    <button class="thinking-toggle" @click="toggleThinking(idx)">
                      <Brain :size="14" />
                      <span>思考过程</span>
                      <ChevronDown v-if="!m.showThinking" :size="14" />
                      <ChevronUp v-else :size="14" />
                    </button>
                    <div v-if="m.showThinking" class="thinking-content" v-html="m.thinkingHtml || getThinkingHtml(m.text)"></div>
                  </div>
                </template>
                <div class="message-content" v-html="m.contentHtml || getContentHtml(m.text, m.role)"></div>
                <div v-if="m.sources?.length" style="margin-top: 12px; padding-top: 12px; border-top: 2px solid var(--anime-border-light);">
                  <div style="margin-bottom: 8px;">
                    <span class="anime-badge blue">引用来源</span>
                  </div>
                  <div v-for="(s, i) in m.sources" :key="i" style="padding: 12px; background: rgba(168, 216, 234, 0.1); border: 2px solid var(--anime-border); border-radius: var(--anime-radius-lg); margin-bottom: 8px;">
                    <div style="display: flex; gap: 10px; margin-bottom: 8px;">
                      <span class="anime-badge muted">{{ s.datasetName || s.datasetId || 'KB' }}</span>
                      <span class="anime-badge pink">{{ s.documentName || s.documentId || 'DOC' }}</span>
                      <span v-if="typeof s.score === 'number'" class="anime-code">score: {{ s.score.toFixed(3) }}</span>
                    </div>
                    <div style="font-size: 13px; color: var(--anime-text-secondary); white-space: pre-wrap;">{{ s.content }}</div>
                  </div>
                </div>
                <div v-if="m.role === 'assistant' && m.feedback === undefined" style="margin-top: 12px; display: flex; gap: 10px; align-items: center;">
                  <button class="anime-btn ghost" style="padding: 6px 12px;" @click="submitFeedback(idx, 1)">
                    <ThumbsUp :size="16" />
                  </button>
                  <button class="anime-btn ghost" style="padding: 6px 12px;" @click="submitFeedback(idx, 0)">
                    <ThumbsDown :size="16" />
                  </button>
                  <RouterLink to="/feedback" class="anime-btn ghost" style="padding: 6px 12px;">
                    <MessageSquare :size="16" />
                    <span style="margin-left: 4px;">反馈问题</span>
                  </RouterLink>
                </div>
                <div v-if="m.role === 'assistant' && m.feedback !== undefined" style="margin-top: 12px; display: flex; gap: 10px; align-items: center;">
                  <span v-if="m.feedback === 1" class="anime-badge green" style="padding: 6px 12px;">
                    <ThumbsUp :size="14" /> 已点赞
                  </span>
                  <span v-if="m.feedback === 0" class="anime-badge pink" style="padding: 6px 12px;">
                    <ThumbsDown :size="14" /> 已踩
                  </span>
                </div>
              </div>
            </div>
            <div v-if="loading" class="anime-chat-message assistant anime-fade-in">
              <div class="anime-chat-avatar">AI</div>
              <div class="anime-chat-bubble" style="display: flex; align-items: center; gap: 10px;">
                <span class="anime-loader-spinner"></span>
                <span style="color: var(--anime-text-muted);">思考中...</span>
              </div>
            </div>
          </div>

          <div v-if="pendingFiles.length" style="padding: 14px 20px; background: rgba(255, 183, 197, 0.1); border-top: 2px solid var(--anime-border); display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
            <span class="anime-badge pink">附件 {{ pendingFiles.length }} 个</span>
            <div style="display: flex; gap: 8px; flex-wrap: wrap;">
              <span v-for="(f, i) in pendingFiles" :key="i" class="anime-pill">
                {{ f.transfer_method === 'local_file' ? `本地:${f.upload_file_id?.slice(0, 8)}...` : `链接:${truncate(f.url || '', 20)}` }}
              </span>
            </div>
            <button class="anime-btn ghost" style="padding: 6px 12px;" @click="pendingFiles = []">清空附件</button>
          </div>

          <div class="anime-chat-composer">
            <button class="anime-btn ghost" type="button" @click="toggleAttachMenu">
              <Plus :size="18" />
            </button>
            <div v-if="showAttachMenu" style="position: absolute; bottom: 100%; left: 0; background: var(--anime-bg-card); border: 3px solid var(--anime-pink); border-radius: var(--anime-radius-lg); padding: 10px; display: flex; flex-direction: column; gap: 8px; z-index: 10; box-shadow: var(--anime-shadow-card);">
              <button class="anime-btn ghost" type="button" @click="pickLocalFile">
                <Paperclip :size="16" />
                <span>本地文件</span>
              </button>
              <button class="anime-btn ghost" type="button" @click="showUrlInput = true; showAttachMenu = false">
                <Link :size="16" />
                <span>URL链接</span>
              </button>
            </div>
            <input ref="fileInputRef" type="file" style="display: none;" :disabled="loading" @change="handleFileUpload" />
            <div v-if="showUrlInput" style="flex: 1; display: flex; gap: 10px;">
              <input v-model="urlInput" class="anime-input anime-chat-input" placeholder="输入文件URL (https://...)" :disabled="loading" @keyup.enter="addUrlFile" />
              <button class="anime-btn blue" @click="addUrlFile">添加</button>
              <button class="anime-btn ghost" @click="showUrlInput = false; urlInput = ''">取消</button>
            </div>
            <div v-else class="anime-chat-input">
              <input v-model="input" class="anime-input" placeholder="输入问题，按Enter发送..." :disabled="loading" @keyup.enter="send" />
            </div>
            <button class="anime-btn primary" :disabled="loading || !input.trim()" @click="send">
              <Send :size="18" />
              <span>{{ loading ? '发送中...' : '发送' }}</span>
            </button>
          </div>
        </div>
      </div>

      <div v-if="error" class="anime-error" style="margin: 16px 28px;">{{ error }}</div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { Plus, Trash2, Paperclip, Link, Send, ThumbsUp, ThumbsDown, MessageSquare, ChevronDown, ChevronUp, Brain } from 'lucide-vue-next'
import { webChatWithSession } from '../api/chat'
import { submitFeedback as submitFeedbackApi, getFeedbackStatus } from '../api/feedback'
import { getOrCreateUserId, getCurrentUser } from '../lib/user'
import { uploadFile } from '../api/upload'
import { createSession, listSessions, getSessionMessages, deleteSession as deleteSessionApi } from '../api/session'
import { renderMessage } from '../lib/markdown'
import type { ChatFileInfo, RetrieverResource } from '../types/dify'
import type { ChatSession, ChatMessage } from '../api/session'

type DisplayMessage = { 
  role: 'user' | 'assistant'
  text: string
  sources?: RetrieverResource[]
  feedback?: number
  thinkingHtml?: string | null
  contentHtml?: string
  showThinking?: boolean
}

const userId = getOrCreateUserId()
const displayUser = computed(() => getCurrentUser() || '访客用户')

const input = ref('')
const urlInput = ref('')
const loading = ref(false)
const error = ref<string | null>(null)
const sessions = ref<ChatSession[]>([])
const currentSession = ref<ChatSession | null>(null)
const messages = ref<DisplayMessage[]>([])
const pendingFiles = ref<ChatFileInfo[]>([])
const showAttachMenu = ref(false)
const showUrlInput = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)

function truncate(s: string, max: number): string { return s && s.length > max ? `${s.slice(0, max)}...` : s }

function getThinkingHtml(text: string): string | null {
  const { thinkingHtml } = renderMessage(text)
  return thinkingHtml
}

function getContentHtml(text: string, role: 'user' | 'assistant'): string {
  if (role === 'user') {
    return `<p style="white-space: pre-wrap; line-height: 1.7;">${text}</p>`
  }
  const { contentHtml } = renderMessage(text)
  return contentHtml
}

function toggleThinking(idx: number) {
  messages.value[idx].showThinking = !messages.value[idx].showThinking
}

function getDateKey(t: string | undefined): string {
  if (!t) return 'unknown'
  const d = new Date(t)
  return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`
}

function formatDateTitle(t: string | undefined): string {
  if (!t) return '未知日期'
  const d = new Date(t)
  const today = new Date()
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)
  
  if (d.getFullYear() === today.getFullYear() && d.getMonth() === today.getMonth() && d.getDate() === today.getDate()) {
    return '今天'
  }
  if (d.getFullYear() === yesterday.getFullYear() && d.getMonth() === yesterday.getMonth() && d.getDate() === yesterday.getDate()) {
    return '昨天'
  }
  return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()}`
}

function formatTime(t: string | undefined): string {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

const groupedSessions = computed(() => {
  const groups: { dateKey: string; dateTitle: string; sessions: ChatSession[] }[] = []
  const groupMap = new Map<string, ChatSession[]>()
  
  for (const s of sessions.value) {
    const key = getDateKey(s.lastMessageTime || s.createTime)
    if (!groupMap.has(key)) {
      groupMap.set(key, [])
    }
    groupMap.get(key)!.push(s)
  }
  
  const sortedKeys = Array.from(groupMap.keys()).sort((a, b) => {
    const [ay, am, ad] = a.split('-').map(Number)
    const [by, bm, bd] = b.split('-').map(Number)
    return (by * 10000 + bm * 100 + bd) - (ay * 10000 + am * 100 + ad)
  })
  
  for (const key of sortedKeys) {
    const sessionList = groupMap.get(key)!
    const sampleTime = sessionList[0]?.lastMessageTime || sessionList[0]?.createTime
    groups.push({
      dateKey: key,
      dateTitle: formatDateTitle(sampleTime),
      sessions: sessionList
    })
  }
  
  return groups
})

async function loadSessions() {
  try {
    const resp = await listSessions(userId, 'web', 1, 50)
    sessions.value = resp.records || []
    if (sessions.value.length > 0 && !currentSession.value) {
      await switchSession(sessions.value[0])
    }
  } catch (e: any) {
    error.value = e?.message || '加载会话列表失败'
  }
}

async function createNewSession() {
  try {
    const session = await createSession(userId, 'web')
    sessions.value.unshift(session)
    await switchSession(session)
  } catch (e: any) {
    error.value = e?.message || '创建会话失败'
  }
}

async function switchSession(session: ChatSession) {
  currentSession.value = session
  messages.value = []
  error.value = null
  pendingFiles.value = []
  
  try {
    const msgs = await getSessionMessages(session.sessionId)
    const feedbackMap = await getFeedbackStatus(session.sessionId)
    
    for (const m of msgs) {
      if (m.query) {
        messages.value.push({ role: 'user', text: m.query })
      }
      if (m.answer) {
        const idx = messages.value.length
        const feedback = feedbackMap[idx] !== undefined ? feedbackMap[idx] : undefined
        const { thinkingHtml, contentHtml } = renderMessage(m.answer)
        messages.value.push({ 
          role: 'assistant', 
          text: m.answer, 
          feedback,
          thinkingHtml,
          contentHtml,
          showThinking: false
        })
      }
    }
  } catch (e: any) {
    error.value = e?.message || '加载消息失败'
  }
}

async function deleteSession(session: ChatSession) {
  if (!confirm('确定删除此对话吗？')) return
  try {
    await deleteSessionApi(session.sessionId)
    sessions.value = sessions.value.filter(s => s.sessionId !== session.sessionId)
    if (currentSession.value?.sessionId === session.sessionId) {
      if (sessions.value.length > 0) {
        await switchSession(sessions.value[0])
      } else {
        currentSession.value = null
        messages.value = []
      }
    }
  } catch (e: any) {
    error.value = e?.message || '删除失败'
  }
}

function toggleAttachMenu() {
  showAttachMenu.value = !showAttachMenu.value
  if (showUrlInput.value) showUrlInput.value = false
}

function pickLocalFile() {
  showAttachMenu.value = false
  fileInputRef.value?.click()
}

async function handleFileUpload(e: Event) {
  const el = e.target as HTMLInputElement
  const file = el.files?.[0]
  if (!file) return
  loading.value = true
  error.value = null
  try {
    const resp = await uploadFile(file, userId)
    if (resp.id) {
      pendingFiles.value.push({ type: guessFileType(file.name), transfer_method: 'local_file', upload_file_id: resp.id })
    }
  } catch (err: any) { error.value = err?.message || '上传失败' }
  finally { loading.value = false; el.value = '' }
}

function addUrlFile() {
  const u = urlInput.value.trim()
  if (!u) return
  pendingFiles.value.push({ type: guessFileType(u), transfer_method: 'remote_url', url: u })
  urlInput.value = ''
  showUrlInput.value = false
}

function guessFileType(name: string): ChatFileInfo['type'] {
  const n = name.toLowerCase()
  if (/\.(png|jpg|jpeg|gif|webp|bmp)$/.test(n)) return 'image'
  if (/\.(mp3|wav|m4a|aac|ogg)$/.test(n)) return 'audio'
  if (/\.(mp4|mov|mkv|webm)$/.test(n)) return 'video'
  return 'document'
}

async function send() {
  const text = input.value.trim()
  if (!text || loading.value) return

  if (!currentSession.value) {
    await createNewSession()
    if (!currentSession.value) return
  }

  error.value = null
  messages.value.push({ role: 'user', text })
  input.value = ''
  loading.value = true

  try {
    const files = [...pendingFiles.value]
    const resp = await webChatWithSession(currentSession.value.sessionId, text, userId, files)
    const { thinkingHtml, contentHtml } = renderMessage(resp.answer || '（无返回）')
    messages.value.push({ 
      role: 'assistant', 
      text: resp.answer || '（无返回）', 
      sources: resp.retrieverResources || [],
      thinkingHtml,
      contentHtml,
      showThinking: false
    })
    pendingFiles.value = []
    
    currentSession.value.messageCount = (currentSession.value.messageCount || 0) + 2
    currentSession.value.lastMessageTime = new Date().toISOString()
    if (!currentSession.value.title) {
      currentSession.value.title = text.length > 50 ? text.substring(0, 50) + '...' : text
    }
  } catch (e: any) {
    error.value = e?.message || '请求失败'
    messages.value.push({ role: 'assistant', text: '【系统错误】请求失败，请稍后再试' })
  } finally {
    loading.value = false
  }
}

async function submitFeedback(msgIdx: number, rating: number) {
  if (!currentSession.value) return
  try {
    const result = await submitFeedbackApi(currentSession.value.sessionId, msgIdx, rating)
    if (result.success) {
      messages.value[msgIdx].feedback = rating
    } else {
      error.value = result.message
    }
  } catch {
    error.value = '反馈提交失败'
  }
}

onMounted(loadSessions)
</script>

<style scoped>
.anime-chat-card { min-height: calc(100vh - 120px); }
.anime-chat-card .anime-card-body { padding: 0; }
.anime-chat-layout { display: flex; height: calc(100vh - 180px); }
.anime-chat-sidebar { width: 260px; border-right: 3px solid var(--anime-border); background: var(--anime-bg-card); overflow-y: auto; }
.anime-session-list { padding: 8px; }
.anime-session-date { 
  padding: 8px 12px; 
  font-size: 13px; 
  font-weight: 700; 
  color: var(--anime-pink); 
  margin-top: 8px;
  border-bottom: 2px solid var(--anime-border);
}
.anime-session-date:first-child { margin-top: 0; }
.anime-session-item { 
  padding: 12px; 
  border-radius: var(--anime-radius-lg); 
  margin-bottom: 8px; 
  cursor: pointer;
  background: var(--anime-bg);
  border: 2px solid transparent;
  transition: all 0.2s;
  position: relative;
}
.anime-session-item:hover { border-color: var(--anime-pink); }
.anime-session-item.active { border-color: var(--anime-pink); background: rgba(255, 183, 197, 0.1); }
.anime-session-title { font-weight: 600; color: var(--anime-text-primary); margin-bottom: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.anime-session-meta { font-size: 12px; color: var(--anime-text-muted); display: flex; gap: 8px; }
.anime-session-delete { 
  position: absolute; 
  right: 8px; 
  top: 8px; 
  background: transparent; 
  border: none; 
  color: var(--anime-pink); 
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
}
.anime-session-item:hover .anime-session-delete { opacity: 1; }
.anime-chat-main { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.anime-chat-messages { flex: 1; overflow-y: auto; padding: 16px; }
.anime-chat-composer { position: relative; }

.thinking-section {
  margin-bottom: 12px;
  border: 2px solid var(--anime-border);
  border-radius: var(--anime-radius-lg);
  background: rgba(255, 183, 197, 0.05);
}

.thinking-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: transparent;
  border: none;
  width: 100%;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  color: var(--anime-purple);
  transition: all 0.2s;
}

.thinking-toggle:hover {
  background: rgba(255, 183, 197, 0.1);
}

.thinking-content {
  padding: 12px 14px;
  border-top: 2px solid var(--anime-border);
  font-size: 14px;
  color: var(--anime-text-secondary);
  line-height: 1.6;
  max-height: 200px;
  overflow-y: auto;
}

.message-content {
  line-height: 1.7;
}

.message-content p {
  margin: 0 0 12px 0;
}

.message-content p:last-child {
  margin-bottom: 0;
}

.message-content pre {
  background: rgba(0, 0, 0, 0.05);
  padding: 12px;
  border-radius: var(--anime-radius-lg);
  overflow-x: auto;
  margin: 12px 0;
}

.message-content code {
  background: rgba(0, 0, 0, 0.08);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

.message-content pre code {
  background: transparent;
  padding: 0;
}

.message-content ul, .message-content ol {
  margin: 12px 0;
  padding-left: 24px;
}

.message-content li {
  margin-bottom: 6px;
}

.message-content h1, .message-content h2, .message-content h3 {
  margin: 16px 0 8px 0;
  font-weight: 700;
}

.message-content h1 { font-size: 20px; }
.message-content h2 { font-size: 18px; }
.message-content h3 { font-size: 16px; }

.message-content a {
  color: var(--anime-blue);
  text-decoration: underline;
}

.message-content blockquote {
  border-left: 4px solid var(--anime-pink);
  padding-left: 12px;
  margin: 12px 0;
  color: var(--anime-text-secondary);
}

.message-content table {
  border-collapse: collapse;
  margin: 12px 0;
}

.message-content th, .message-content td {
  border: 2px solid var(--anime-border);
  padding: 8px 12px;
}

.message-content th {
  background: rgba(255, 183, 197, 0.1);
  font-weight: 600;
}
</style>