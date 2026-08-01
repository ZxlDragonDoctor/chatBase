<template>
  <div class="anime-page-shell">
    <section class="anime-card">
      <div class="anime-card-header">
        <div>
          <div class="anime-card-title">私聊采集</div>
          <div class="anime-card-desc">
            QQ/企微/微信单聊消息采集记录 · 应用绑定
          </div>
        </div>
        <div class="anime-card-actions">
          <button class="anime-btn ghost" :disabled="loading" @click="reload">
            <RefreshCw :size="18" :class="{ 'animate-spin': loading }" />
            <span>刷新数据</span>
          </button>
        </div>
      </div>

      <div v-if="err" class="anime-error" style="margin: 16px 28px;">{{ err }}</div>

      <div class="anime-card-body">
        <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 16px; flex-wrap: wrap;">
          <select v-model="scope" class="anime-scope-select" style="min-width: 100px;">
            <option value="all">全部</option>
            <option value="unassigned">未分配</option>
            <option value="bound">已绑定</option>
          </select>
          <div class="anime-tabs">
            <button v-for="t in platformTabs" :key="t.key" class="anime-tab" :class="{ active: platform === t.key }" @click="togglePlatform(t.key)">{{ t.label }}</button>
          </div>
          <input v-model="convSearch" type="text" placeholder="搜索昵称或会话ID..." class="anime-search-input" style="min-width: 200px; flex: 1;" />
        </div>

        <div class="im-split-view">
          <div class="im-list-panel">
            <div v-if="loading && conversations.length === 0" class="anime-empty">
              <span class="anime-loader-spinner"></span>
              <span class="anime-empty-text">加载中...</span>
            </div>
            <div v-else-if="conversations.length === 0" class="anime-empty">
              <div class="anime-empty-icon">📭</div>
              <div class="anime-empty-text">暂无私聊记录</div>
            </div>
            <button v-for="c in filteredConversations" :key="c.platform + ':' + c.conversationId" class="im-group-item" :class="{ 'active': selected?.conversationId === c.conversationId && selected?.platform === c.platform }" @click="selectConversation(c)">
              <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 8px;">
                <span class="anime-badge" :class="getPlatformColor(c.platform)">{{ getPlatformLabel(c.platform) }}</span>
                <span style="font-weight: 600; color: var(--anime-text-primary);">{{ getConvName(c) }}</span>
                <span v-if="c.appName" class="anime-badge blue" style="margin-left: 4px;">{{ c.appName }}</span>
              </div>
              <div style="font-size: 13px; color: var(--anime-text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 260px;">
                {{ c.lastMessage || '暂无消息' }}
              </div>
              <div style="display: flex; justify-content: space-between; font-size: 13px; color: var(--anime-text-muted); margin-top: 6px;">
                <span style="color: var(--anime-blue);">{{ c.messageCount }} 条消息</span>
                <span v-if="c.lastMessageTime">{{ formatTime(c.lastMessageTime) }}</span>
              </div>
              <div style="font-size: 12px; color: var(--anime-text-muted); margin-top: 4px;">
                <span v-if="c.createdBy">归属: {{ c.createdBy }}</span>
              </div>
            </button>
          </div>

          <div class="im-detail-panel">
            <template v-if="!selected">
              <div class="anime-empty">
                <div class="anime-empty-icon">👈</div>
                <div class="anime-empty-text">请选择左侧私聊查看消息 ✿</div>
              </div>
            </template>
            <template v-else>
              <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 16px;">
                <span class="anime-badge" :class="getPlatformColor(selected.platform)">{{ getPlatformLabel(selected.platform) }}</span>
                <span style="font-weight: 600; color: var(--anime-text-primary);">{{ getConvName(selected) }}</span>
                <span style="font-size: 12px; color: var(--anime-text-muted);">会话ID: {{ selected.conversationId }}</span>
              </div>

              <div v-if="!selected.createdBy || selected.createdBy === currentUserName" style="padding: 16px; background: var(--anime-blue-bg); box-shadow: var(--anime-shadow-sm); border-radius: var(--anime-radius-lg); margin-bottom: 16px;">
                <div style="font-weight: 600; color: var(--anime-text-primary); margin-bottom: 12px;">
                  <span style="color: var(--anime-pink);">应用绑定</span>
                </div>
                <div style="display: flex; align-items: center; gap: 12px;">
                  <select v-model="bindAppId" class="anime-app-select" style="min-width: 200px;">
                    <option value="">不绑定应用</option>
                    <option v-for="app in appList" :key="app.id" :value="app.id">{{ app.icon || '🤖' }} {{ app.name }}</option>
                  </select>
                  <button class="anime-btn primary" @click="saveBindApp" :disabled="bindSaving">
                    <span v-if="bindSaving">保存中...</span>
                    <span v-else>保存绑定</span>
                  </button>
                </div>
                <div v-if="selected.appName" style="margin-top: 10px; font-size: 13px; color: var(--anime-text-muted);">
                  当前绑定: <span style="color: var(--anime-blue);">{{ selected.appName }}</span>
                </div>
              </div>

              <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 16px;">
                <input v-model="searchKeyword" type="text" placeholder="搜索消息内容或用户ID..." class="anime-search-input" @keyup.enter="searchMessages" style="flex: 1;" />
                <button class="anime-btn ghost" @click="searchMessages" :disabled="msgLoading">
                  <span>搜索</span>
                </button>
                <button v-if="searchKeyword" class="anime-btn ghost" @click="clearSearch">
                  <span>清除</span>
                </button>
              </div>

              <div v-if="msgLoading" class="anime-empty">
                <span class="anime-loader-spinner"></span>
                <span class="anime-empty-text">消息加载中...</span>
              </div>
              <div v-else-if="messages.length === 0" class="anime-empty">
                <div class="anime-empty-icon">📭</div>
                <div class="anime-empty-text">{{ searchKeyword ? '未找到匹配消息' : '暂无消息记录' }}</div>
              </div>
              <div v-else class="message-list">
                <div v-for="m in messages" :key="m.id" style="padding: 14px; background: var(--anime-bg-card); box-shadow: var(--anime-shadow-sm); border-radius: var(--anime-radius-lg); margin-bottom: 12px;">
                  <div style="display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 8px;">
                    <span class="anime-badge blue">用户: {{ m.userId || '未知' }}</span>
                    <span class="anime-badge muted">{{ m.messageType || '文本' }}</span>
                    <span class="anime-badge" :class="m.synced ? 'green' : 'pink'">{{ m.synced ? '已同步' : '未同步' }}</span>
                    <span style="font-size: 13px; color: var(--anime-text-muted);">{{ formatTime(m.messageTime) }}</span>
                  </div>
                  <div style="font-size: 14px; color: var(--anime-text-primary); white-space: pre-wrap; line-height: 1.5;">{{ m.rawMessage }}</div>
                </div>
              </div>
              <div v-if="msgTotal > messages.length" style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap; padding-top: 12px; border-top: 1px solid var(--anime-border-light);">
                <button class="anime-btn ghost" :disabled="msgLoading || page <= 0" @click="page--; loadMessages()">上一页</button>
                <span class="anime-code">第 {{ page + 1 }} 页 · 共 {{ msgTotal }} 条</span>
                <button class="anime-btn ghost" :disabled="msgLoading || (page + 1) * pageSize >= msgTotal" @click="page++; loadMessages()">下一页</button>
              </div>
            </template>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import { fetchConversations, fetchPrivateMessages } from '../api/console'
import { api } from '../api/client'
import type { ConversationSummary, GroupMessageItem } from '../types/console'

interface KbApp {
  id: number
  name: string
  icon: string
  isDefault: boolean
}

const currentUserName = computed(() => localStorage.getItem('chatbase_original_username') || localStorage.getItem('chatbase_user') || '')

const filteredConversations = computed(() => {
  const q = convSearch.value.trim().toLowerCase()
  if (!q) return conversations.value
  return conversations.value.filter(c => {
    const id = String(c.conversationId ?? '').toLowerCase()
    const name = String(c.userNickname ?? c.title ?? '').toLowerCase()
    const userId = String(c.userId ?? '').toLowerCase()
    return id.includes(q) || name.includes(q) || userId.includes(q)
  })
})

const platformTabs = [{ key: 'qq', label: 'QQ' }, { key: 'wecom', label: '企微' }, { key: 'wx', label: '微信' }]
const platform = ref<string | null>(null)
const scope = ref('all')
const convSearch = ref('')
const conversations = ref<ConversationSummary[]>([])
const loading = ref(false)
const err = ref<string | null>(null)

const selected = ref<ConversationSummary | null>(null)
const messages = ref<GroupMessageItem[]>([])
const msgLoading = ref(false)
const msgTotal = ref(0)
const page = ref(0)
const pageSize = 40
const appList = ref<KbApp[]>([])
const bindAppId = ref<number | string>('')
const bindSaving = ref(false)
const searchKeyword = ref('')

async function loadApps() {
  try {
    const resp = await api.get('/kb/app/list')
    appList.value = resp.data || []
  } catch (e) {
    console.error('加载应用列表失败', e)
  }
}

async function reload() {
  loading.value = true
  err.value = null
  try {
    const all = await fetchConversations()
    conversations.value = applyFilters(all)
    if (selected.value) {
      const still = conversations.value.find((c) => c.conversationId === selected.value!.conversationId && c.platform === selected.value!.platform)
      if (!still) { selected.value = null; messages.value = [] }
      else { selected.value = still; bindAppId.value = still.appId || '' }
    }
  } catch (e: any) { err.value = e?.message || '加载失败'; conversations.value = [] }
  finally { loading.value = false }
}

function applyFilters(list: ConversationSummary[]): ConversationSummary[] {
  let result = list
  if (platform.value) {
    result = result.filter(c => c.platform === platform.value)
  }
  if (scope.value === 'unassigned') {
    result = result.filter(c => !c.createdBy)
  } else if (scope.value === 'bound') {
    result = result.filter(c => c.createdBy || c.appId)
  }
  return result
}

function selectConversation(c: ConversationSummary) {
  selected.value = c
  bindAppId.value = c.appId || ''
  page.value = 0
  loadMessages()
}

async function loadMessages() {
  if (!selected.value) return
  msgLoading.value = true
  try {
    const res = await fetchPrivateMessages({
      conversationId: selected.value.conversationId,
      page: page.value,
      size: pageSize,
      keyword: searchKeyword.value || undefined
    })
    messages.value = res.records
    msgTotal.value = res.total
  } catch { messages.value = []; msgTotal.value = 0 }
  finally { msgLoading.value = false }
}

function searchMessages() {
  page.value = 0
  loadMessages()
}

function clearSearch() {
  searchKeyword.value = ''
  page.value = 0
  loadMessages()
}

async function saveBindApp() {
  if (!selected.value) return
  bindSaving.value = true
  try {
    const appId = bindAppId.value ? Number(bindAppId.value) : null
    const appName = appId ? appList.value.find(a => a.id === appId)?.name : null
    if (appId) {
      await api.put(`/console/conversations/${selected.value.id}/app`, { appId, appName })
      selected.value.appId = appId
      selected.value.appName = appName
    } else {
      await api.delete(`/console/conversations/${selected.value.id}/app`)
      selected.value.appId = null
      selected.value.appName = null
    }
    reload()
  } catch (e: any) {
    err.value = e?.response?.data?.message || '绑定失败'
  } finally {
    bindSaving.value = false
  }
}

function togglePlatform(key: string) {
  platform.value = platform.value === key ? null : key
}

function getPlatformColor(p: string): string { if (p === 'qq') return 'green'; if (p === 'wecom') return 'blue'; if (p === 'wx') return 'purple'; return 'purple' }
function getPlatformLabel(p: string): string { if (p === 'qq') return 'QQ'; if (p === 'wecom') return '企微'; if (p === 'wx') return '微信'; return '私聊' }
function getConvName(c: ConversationSummary): string {
  if (c.userNickname && c.userNickname.trim()) return c.userNickname.trim()
  if (c.title && c.title.trim()) return c.title.trim()
  return c.userId || c.conversationId
}
function formatTime(t: any): string {
  if (!t) return ''
  if (Array.isArray(t) && t.length >= 5) {
    const [y, m, d, h = 0, min = 0] = t
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}`
  }
  if (typeof t === 'string') return t.slice(0, 16).replace('T', ' ')
  return String(t)
}

watch([platform, scope], () => { selected.value = null; messages.value = []; reload() })
onMounted(() => {
  reload()
  loadApps()
})
</script>

<style scoped>
@keyframes spin { to { transform: rotate(360deg); } }
.animate-spin { animation: spin 1s linear infinite; }
.im-split-view { display: grid; grid-template-columns: minmax(240px, 320px) 1fr; gap: 0; min-height: 480px; }
@media (max-width: 800px) { .im-split-view { grid-template-columns: 1fr; } }
.im-list-panel { position: relative; max-height: min(70vh, 600px); overflow-y: auto; padding: 12px; }
.im-list-panel::after { content: ''; position: absolute; right: 0; top: 0; bottom: 0; width: 1px; background: linear-gradient(180deg, transparent, var(--anime-border), transparent); }
.im-group-item { display: flex; flex-direction: column; gap: 10px; width: 100%; padding: 14px; margin-bottom: 10px; background: var(--anime-bg-card); box-shadow: var(--anime-shadow-sm); border-radius: var(--anime-radius-lg); cursor: pointer; transition: all 0.3s ease; border: none; }
.im-group-item:hover { box-shadow: var(--anime-shadow-pink); transform: translateX(4px); }
.im-group-item.active { box-shadow: var(--anime-shadow-pink); background: var(--anime-pink-bg); }
.im-detail-panel { padding: 20px; max-height: min(70vh, 600px); overflow-y: auto; display: flex; flex-direction: column; gap: 16px; }
.message-list { display: flex; flex-direction: column; gap: 12px; }
.anime-app-select {
  background: var(--anime-bg);
  box-shadow: var(--anime-shadow-sm);
  border: none;
  border-radius: var(--anime-radius-lg);
  padding: 8px 12px;
  font-size: 14px;
  color: var(--anime-text-primary);
  cursor: pointer;
  outline: none;
}
.anime-app-select:focus {
  box-shadow: var(--anime-shadow-pink);
}
.anime-search-input {
  background: var(--anime-bg);
  box-shadow: var(--anime-shadow-sm);
  border: none;
  border-radius: var(--anime-radius-lg);
  padding: 8px 12px;
  font-size: 14px;
  color: var(--anime-text-primary);
  outline: none;
  transition: box-shadow 0.3s ease;
}
.anime-search-input:focus {
  box-shadow: var(--anime-shadow-pink);
}
.anime-scope-select {
  background: var(--anime-bg);
  box-shadow: var(--anime-shadow-sm);
  border: none;
  border-radius: var(--anime-radius-lg);
  padding: 6px 10px;
  font-size: 14px;
  color: var(--anime-text-primary);
  cursor: pointer;
  outline: none;
}
.anime-scope-select:focus {
  box-shadow: var(--anime-shadow-pink);
}
</style>
