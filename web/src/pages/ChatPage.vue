<template>
  <div class="anime-page-shell">
    <section class="anime-card anime-chat-card">
      <div class="anime-card-header">
        <div>
          <h1 class="anime-card-title">Web问答</h1>
          <div class="anime-card-desc">
            AI智能问答 · 知识库检索 · 对话管理
          </div>
        </div>
        <div class="anime-card-actions">
          <select v-model="selectedAppId" class="anime-app-select" aria-label="选择问答应用" :disabled="loading">
            <option v-for="app in appList" :key="app.id" :value="app.id">{{ app.name }}</option>
          </select>
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
              >
                <button type="button" class="anime-session-select" :aria-current="currentSession?.sessionId === s.sessionId ? 'true' : undefined" @click="switchSession(s)">
                  <span class="anime-session-title">{{ s.title || '新对话' }}</span>
                  <span class="anime-session-meta">
                    <span>{{ s.messageCount || 0 }} 条消息</span>
                    <span>{{ formatTime(s.lastMessageTime) }}</span>
                  </span>
                </button>
                <button class="anime-session-delete" :aria-label="`删除对话：${s.title || '新对话'}`" @click.stop="deleteSession(s)">
                  <Trash2 :size="14" />
                </button>
              </div>
            </template>
          </div>
        </aside>

        <div class="anime-chat-main">
          <div ref="messagesEl" class="anime-chat-messages">
            <div v-if="messages.length === 0" class="anime-empty">
              <div class="anime-empty-icon">💬</div>
              <div class="anime-empty-text">输入问题开始对话 ✿ 支持文件附件 ✿</div>
            </div>
            <div v-for="(m, idx) in messages" :key="idx" class="anime-chat-message anime-fade-in" :class="m.role">
              <div class="anime-chat-avatar">{{ m.role === 'user' ? 'U' : 'AI' }}</div>
              <div class="anime-chat-bubble">
                <template v-if="m.role === 'assistant' && (m.thinkingHtml || getThinkingHtml(m.text))">
                  <div class="thinking-section">
                    <button class="thinking-toggle" :aria-expanded="Boolean(m.showThinking)" @click="toggleThinking(idx)">
                      <Brain :size="14" />
                      <span>思考过程</span>
                      <ChevronDown v-if="!m.showThinking" :size="14" />
                      <ChevronUp v-else :size="14" />
                    </button>
                    <div v-if="m.showThinking" class="thinking-content" v-html="m.thinkingHtml || getThinkingHtml(m.text)"></div>
                  </div>
                </template>
                <!-- 用户消息：纯文本插值，杜绝 XSS -->
                <div v-if="m.role === 'user'" class="message-content" style="white-space: pre-wrap; line-height: 1.7;">{{ m.text }}</div>
                <div v-else class="message-content" v-html="m.contentHtml || getContentHtml(m.text, m.role)"></div>
                <div v-if="m.sources?.length" class="chat-sources">
                  <div class="chat-sources-heading">
                    <span class="anime-badge blue">引用来源</span>
                  </div>
                  <div v-for="(s, i) in m.sources" :key="i" class="chat-source-item">
                    <div class="chat-source-meta">
                      <span class="anime-badge muted">{{ s.datasetName || s.datasetId || 'KB' }}</span>
                      <span class="anime-badge pink">{{ s.documentName || s.documentId || 'DOC' }}</span>
                      <span v-if="typeof s.score === 'number'" class="anime-code">score: {{ s.score.toFixed(3) }}</span>
                    </div>
                    <div class="chat-source-content">{{ s.content }}</div>
                  </div>
                </div>
                <div v-if="m.role === 'assistant' && m.feedback === undefined" class="chat-message-feedback">
                  <button class="anime-btn ghost" aria-label="回答有帮助" style="padding: 6px 12px;" @click="submitFeedback(idx, 1)">
                    <ThumbsUp :size="16" />
                  </button>
                  <button class="anime-btn ghost" aria-label="回答没有帮助" style="padding: 6px 12px;" @click="submitFeedback(idx, 0)">
                    <ThumbsDown :size="16" />
                  </button>
                  <RouterLink to="/feedback" class="anime-btn ghost" style="padding: 6px 12px;">
                    <MessageSquare :size="16" />
                    <span style="margin-left: 4px;">反馈问题</span>
                  </RouterLink>
                </div>
                <div v-if="m.role === 'assistant' && m.feedback !== undefined" class="chat-message-feedback">
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

          <div v-if="pendingFiles.length" class="chat-pending-files">
            <span class="anime-badge pink">附件 {{ pendingFiles.length }} 个</span>
            <div class="chat-pending-list">
              <span v-for="(f, i) in pendingFiles" :key="i" class="anime-pill">
                {{ f.transfer_method === 'local_file' ? `本地:${f.upload_file_id?.slice(0, 8)}...` : `链接:${truncate(f.url || '', 20)}` }}
              </span>
            </div>
            <button class="anime-btn ghost" style="padding: 6px 12px;" @click="pendingFiles = []">清空附件</button>
          </div>

          <div class="anime-chat-composer">
            <button class="anime-btn ghost" type="button" aria-label="添加附件" :aria-expanded="showAttachMenu" @click="toggleAttachMenu">
              <Plus :size="18" />
            </button>
            <div v-if="showAttachMenu" class="chat-attach-menu">
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
            <div v-if="showUrlInput" class="chat-url-input-row">
              <input v-model="urlInput" class="anime-input anime-chat-input" aria-label="附件文件 URL" placeholder="输入文件URL (https://...)" :disabled="loading" @keyup.enter="addUrlFile" />
              <button class="anime-btn blue" @click="addUrlFile">添加</button>
              <button class="anime-btn ghost" @click="showUrlInput = false; urlInput = ''">取消</button>
            </div>
            <div v-else class="anime-chat-input">
              <input
                v-model="input"
                class="anime-input"
                aria-label="输入问题"
                placeholder="输入问题，按Enter发送..."
                :disabled="loading"
                @keydown.enter.exact="onEnterSend"
              />
            </div>
            <button class="anime-btn primary" :disabled="loading || (!input.trim() && pendingFiles.length === 0)" @click="send">
              <Send :size="18" />
              <span>{{ loading ? '发送中...' : '发送' }}</span>
            </button>
          </div>
        </div>
      </div>

      <div v-if="error" class="anime-error chat-error" role="alert">{{ error }}</div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { Plus, Trash2, Paperclip, Link, Send, ThumbsUp, ThumbsDown, MessageSquare, ChevronDown, ChevronUp, Brain } from 'lucide-vue-next'
import { webChatWithSession } from '../api/chat'
import { submitFeedback as submitFeedbackApi, getFeedbackStatus } from '../api/feedback'
import { getOrCreateUserId, getCurrentUser } from '../lib/user'
import { uploadFile } from '../api/upload'
import { createSession, listSessions, getSessionMessages, deleteSession as deleteSessionApi } from '../api/session'
import { renderMessage, escapeHtml } from '../lib/markdown'
import { api } from '../api/client'
import type { ChatFileInfo, RetrieverResource } from '../types/dify'
import type { ChatSession, ChatMessage } from '../api/session'

interface KbApp {
  id: number
  name: string
  icon: string
  isDefault: boolean
}

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
const messagesEl = ref<HTMLElement | null>(null)
const appList = ref<KbApp[]>([])
const selectedAppId = ref<number | null>(null)

/** 统一提取后端/axios 错误文案 */
function errMsg(e: any, fallback: string): string {
  return e?.response?.data?.message || e?.message || fallback
}

function truncate(s: string, max: number): string { return s && s.length > max ? `${s.slice(0, max)}...` : s }

function getThinkingHtml(text: string): string | null {
  const { thinkingHtml } = renderMessage(text)
  return thinkingHtml
}

function getContentHtml(text: string, role: 'user' | 'assistant'): string {
  if (role === 'user') {
    return `<p style="white-space: pre-wrap; line-height: 1.7;">${escapeHtml(text)}</p>`
  }
  const { contentHtml } = renderMessage(text)
  return contentHtml
}

/** 新消息后自动滚到底部 */
function scrollToBottom() {
  nextTick(() => {
    const el = messagesEl.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

watch(() => messages.value.length, () => scrollToBottom())
watch(loading, () => scrollToBottom())

/** 中文输入法组合期间按 Enter 不发送 */
function onEnterSend(e: KeyboardEvent) {
  if (e.isComposing || e.keyCode === 229) return
  e.preventDefault()
  send()
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
    error.value = errMsg(e, '加载会话列表失败')
  }
}

async function loadApps() {
  try {
    const resp = await api.get('/kb/app/list')
    appList.value = resp.data || []
    const defaultApp = appList.value.find(a => a.isDefault)
    selectedAppId.value = defaultApp?.id || (appList.value.length > 0 ? appList.value[0].id : null)
  } catch (e) {
    console.error('加载应用列表失败', e)
  }
}

async function createNewSession() {
  try {
    const session = await createSession(userId, 'web')
    sessions.value.unshift(session)
    await switchSession(session)
  } catch (e: any) {
    error.value = errMsg(e, '创建会话失败')
  }
}

async function switchSession(session: ChatSession) {
  currentSession.value = session
  messages.value = []
  error.value = null
  pendingFiles.value = []
  
  try {
    const msgs = await getSessionMessages(session.sessionId, userId)
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
    error.value = errMsg(e, '加载消息失败')
  }
}

async function deleteSession(session: ChatSession) {
  if (!confirm('确定删除此对话吗？')) return
  try {
    await deleteSessionApi(session.sessionId, userId)
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
    error.value = errMsg(e, '删除失败')
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
  } catch (err: any) { error.value = errMsg(err, '上传失败') }
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
  if (loading.value) return
  if (!text && pendingFiles.value.length === 0) return

  if (!currentSession.value) {
    await createNewSession()
    if (!currentSession.value) return
  }

  error.value = null
  messages.value.push({ role: 'user', text: text || '（附件）' })
  input.value = ''
  loading.value = true

  try {
    const files = [...pendingFiles.value]
    const resp = await webChatWithSession(currentSession.value.sessionId, text, userId, files, selectedAppId.value ?? undefined)
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
    error.value = errMsg(e, '请求失败，请稍后再试')
    // 错误提示走 error 横幅，不写入 messages，避免 feedback 索引错位
  } finally {
    loading.value = false
    scrollToBottom()
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

onMounted(() => {
  loadSessions()
  loadApps()
})
</script>

<style scoped>
.anime-chat-card { min-height: calc(100vh - 120px); }
.anime-chat-card .anime-card-body { padding: 0; }
.anime-chat-layout { display: flex; height: calc(100vh - 180px); }
.anime-chat-sidebar { width: 260px; background: var(--anime-bg-card); overflow-y: auto; position: relative; }
.anime-chat-sidebar::after { content: ''; position: absolute; right: 0; top: 0; bottom: 0; width: 1px; background: linear-gradient(180deg, transparent, var(--anime-border), transparent); }
.anime-session-list { padding: 8px; }
.anime-session-date {
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 700;
  color: var(--ui-primary);
  margin-top: 8px;
  border-bottom: 1px solid var(--anime-border-light);
}
.anime-session-date:first-child { margin-top: 0; }
.anime-session-item {
  padding: 12px;
  border-radius: var(--anime-radius-lg);
  margin-bottom: 8px;
  cursor: pointer;
  background: var(--anime-bg);
  box-shadow: var(--anime-shadow-sm);
  transition: all 0.2s;
  position: relative;
}
.anime-session-item:hover { box-shadow: var(--ui-shadow-card); }
.anime-session-item.active { box-shadow: var(--ui-shadow-card); background: var(--ui-primary-soft); }
.anime-session-title { font-weight: 600; color: var(--anime-text-primary); margin-bottom: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.anime-session-meta { font-size: 12px; color: var(--anime-text-muted); display: flex; gap: 8px; }
.anime-session-delete {
  position: absolute;
  right: 8px;
  top: 8px;
  background: transparent;
  border: none;
  color: var(--ui-danger);
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
  box-shadow: var(--anime-shadow-sm);
  border-radius: var(--anime-radius-lg);
  background: var(--ui-surface-muted);
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
  color: var(--ui-text-secondary);
  transition: all 0.2s;
  border-radius: var(--anime-radius-lg);
}

.thinking-toggle:hover {
  background: var(--ui-primary-soft);
}

.thinking-content {
  padding: 12px 14px;
  border-top: 1px solid var(--anime-border-light);
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
  border-left: 3px solid var(--ui-primary);
  padding-left: 12px;
  margin: 12px 0;
  color: var(--anime-text-secondary);
  background: var(--ui-primary-soft);
  border-radius: 0 var(--anime-radius-sm) var(--anime-radius-sm) 0;
}

.message-content table {
  border-collapse: collapse;
  margin: 12px 0;
  width: 100%;
}

.message-content th, .message-content td {
  padding: 8px 12px;
  border-bottom: 1px solid var(--anime-border-light);
}

.message-content th {
  background: var(--ui-surface-muted);
  font-weight: 600;
}

.anime-app-select {
  background: var(--anime-bg);
  box-shadow: var(--anime-shadow-sm);
  border: none;
  border-radius: var(--anime-radius-lg);
  padding: 6px 12px;
  font-size: 14px;
  color: var(--anime-text-primary);
  cursor: pointer;
  outline: none;
}

.anime-app-select:focus {
  box-shadow: 0 0 0 2px var(--ui-primary-soft);
}

.anime-chat-card { height: calc(100dvh - 72px); min-height: 560px; display: flex; flex-direction: column; }
.anime-chat-card > .anime-card-header { flex: 0 0 auto; }
.anime-chat-card > .anime-chat-layout { flex: 1 1 auto; min-height: 0; height: auto; border-top: 1px solid var(--ui-border); }
.anime-chat-sidebar { flex: 0 0 260px; background: var(--ui-surface-muted); border-right: 1px solid var(--ui-border); }
.anime-chat-sidebar::after { display: none; }
.anime-session-list { padding: 10px; }
.anime-session-date { color: var(--ui-text-muted); border-color: var(--ui-border); }
.anime-session-item { border: 1px solid transparent; background: transparent; box-shadow: none; transition: background-color .16s ease, border-color .16s ease; }
.anime-session-select { display: block; width: 100%; padding: 0 28px 0 0; border: 0; background: transparent; color: inherit; text-align: left; font: inherit; cursor: pointer; }
.anime-session-select:focus-visible { outline: 2px solid var(--ui-focus); outline-offset: 2px; border-radius: var(--ui-radius-sm); }
.anime-session-item:hover, .anime-session-item:focus-visible { border-color: var(--ui-border); background: var(--ui-surface); box-shadow: none; }
.anime-session-item.active { border-color: #a6d2c8; background: var(--ui-primary-soft); box-shadow: inset 3px 0 var(--ui-primary); }
.anime-session-title { color: var(--ui-text); padding-right: 28px; }
.anime-session-meta { flex-wrap: wrap; color: var(--ui-text-muted); }
.anime-session-delete { color: var(--ui-danger); opacity: .7; }
.anime-session-delete:hover, .anime-session-delete:focus-visible { opacity: 1; }
.anime-chat-main { min-width: 0; background: var(--ui-surface); }
.anime-chat-messages { padding: 22px clamp(16px, 3vw, 40px); }
.anime-chat-bubble { max-width: min(820px, 92%); border: 1px solid var(--ui-border); }
.anime-chat-message.user .anime-chat-bubble { background: var(--ui-primary-soft); border-color: #c8e7de; }
.anime-chat-message.assistant .anime-chat-bubble { background: var(--ui-surface); }
.thinking-section { background: #f4f5f2; border: 1px solid var(--ui-border); box-shadow: none; }
.thinking-toggle { color: var(--ui-text-secondary); }
.thinking-toggle:hover { background: var(--ui-surface-muted); }
.thinking-content { color: var(--ui-text-secondary); border-color: var(--ui-border); }
.message-content { color: var(--ui-text); overflow-wrap: anywhere; }
.message-content pre { max-width: 100%; background: #f2f5f3; overflow-x: auto; }
.message-content code { background: #edf1ef; }
.message-content a { color: var(--ui-primary); }
.message-content blockquote { border-color: var(--ui-primary); background: var(--ui-primary-soft); color: var(--ui-text-secondary); }
.message-content th { background: var(--ui-surface-muted); }
.message-content th, .message-content td { border-color: var(--ui-border); }
.chat-sources { margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--ui-border); }
.chat-sources-heading { margin-bottom: 8px; }
.chat-source-item { min-width: 0; margin-bottom: 8px; padding: 12px; border: 1px solid var(--ui-border); border-radius: var(--ui-radius-md); background: var(--ui-surface-muted); }
.chat-source-meta { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 8px; }
.chat-source-content { color: var(--ui-text-secondary); font-size: 13px; line-height: 1.55; white-space: pre-wrap; overflow-wrap: anywhere; }
.chat-message-feedback { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-top: 12px; }
.chat-pending-files { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; padding: 12px 18px; background: var(--ui-primary-soft); border-top: 1px solid var(--ui-border); }
.chat-pending-list { display: flex; min-width: 0; flex-wrap: wrap; gap: 8px; }
.chat-attach-menu { position: absolute; z-index: 10; bottom: calc(100% + 8px); left: 0; display: flex; flex-direction: column; gap: 8px; padding: 10px; border: 1px solid var(--ui-border); border-radius: var(--ui-radius-md); background: var(--ui-surface); box-shadow: var(--ui-shadow-card); }
.chat-url-input-row { display: flex; flex: 1 1 300px; min-width: 0; flex-wrap: wrap; gap: 8px; }
.chat-url-input-row .anime-chat-input { flex: 1 1 180px; min-width: 0; }
.anime-chat-composer { flex: 0 0 auto; border-top: 1px solid var(--ui-border); background: var(--ui-surface); }
.anime-chat-composer .anime-input { min-width: 0; background: var(--ui-surface-muted); }
.anime-app-select { border: 1px solid var(--ui-border); border-radius: var(--ui-radius-sm); background: var(--ui-surface); box-shadow: none; color: var(--ui-text); }
.anime-app-select:focus { border-color: var(--ui-focus); outline: 2px solid rgb(8 127 112 / 14%); box-shadow: none; }
.chat-error { margin: 12px 20px 16px; }
@media (max-width: 900px) { .anime-chat-card { height: calc(100dvh - 112px); } }
@media (max-width: 760px) {
  .anime-chat-card { height: calc(100dvh - 112px); min-height: 480px; }
  .anime-chat-card > .anime-card-header { align-items: stretch; gap: 12px; }
  .anime-card-actions { justify-content: flex-start; }
  .anime-chat-layout { flex-direction: column; }
  .anime-chat-sidebar { flex: 0 0 auto; width: 100%; max-height: 160px; border-right: 0; border-bottom: 1px solid var(--ui-border); }
  .anime-session-list { display: flex; gap: 8px; overflow-x: auto; }
  .anime-session-date { display: none; }
  .anime-session-item { flex: 0 0 220px; margin-bottom: 0; }
  .anime-chat-main { flex: 1 1 0; min-height: 0; }
  .anime-chat-bubble { max-width: 96%; }
  .anime-chat-composer { flex-wrap: wrap; }
  .anime-chat-composer > .anime-btn.primary { margin-left: auto; }
}
@media (max-width: 480px) {
  .anime-chat-card { min-height: 400px; height: calc(100dvh - 110px); }
  .anime-chat-card > .anime-card-header { padding: 14px; }
  .anime-card-actions { width: 100%; }
  .anime-card-actions .anime-app-select { flex: 1 1 160px; min-width: 0; }
  .anime-chat-messages { padding: 14px 10px; }
  .anime-chat-bubble { max-width: calc(100% - 38px); padding: 12px; }
  .anime-chat-composer { gap: 8px; padding: 10px; }
  .chat-url-input-row > .anime-btn { flex: 1 1 90px; }
  .chat-error { margin: 10px 12px 14px; }
}
</style>
