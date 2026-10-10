<template>
  <div class="anime-page-shell">
    <section class="anime-card">
      <div class="anime-card-header">
        <div>
          <h1 class="anime-card-title">群聊采集</h1>
          <div class="anime-card-desc">
            QQ群/企微群/微信群消息采集记录 · 应用绑定
          </div>
        </div>
        <div class="anime-card-actions">
          <button class="anime-btn ghost" :disabled="loading" @click="reload">
            <RefreshCw :size="18" :class="{ 'animate-spin': loading }" />
            <span>刷新数据</span>
          </button>
        </div>
      </div>

      <div v-if="err" class="anime-error im-page-error" role="alert">{{ err }}</div>

      <div class="anime-card-body">
        <div class="im-toolbar">
          <select v-model="scope" class="anime-scope-select im-scope-select" aria-label="群聊范围">
            <option value="all">全部</option>
            <option value="unassigned">未分配</option>
            <option value="bound">已绑定</option>
          </select>
          <div class="anime-tabs" role="group" aria-label="按平台筛选群聊">
            <button v-for="t in platformTabs" :key="t.key" class="anime-tab" :class="{ active: platform === t.key }" :aria-pressed="platform === t.key" @click="togglePlatform(t.key)">{{ t.label }}</button>
          </div>
          <input v-model="groupSearch" type="text" aria-label="搜索群名或群ID" placeholder="搜索群名或群ID..." class="anime-search-input im-list-search" />
        </div>

        <div class="im-split-view">
          <div class="im-list-panel">
            <div v-if="loading && groups.length === 0" class="anime-empty">
              <span class="anime-loader-spinner"></span>
              <span class="anime-empty-text">加载中...</span>
            </div>
            <div v-else-if="groups.length === 0" class="anime-empty">
              <div class="anime-empty-text">暂无群聊记录</div>
            </div>
            <button v-for="g in filteredGroups" :key="g.platform + ':' + g.groupId" class="im-group-item" :class="{ 'active': selected?.groupId === g.groupId && selected?.platform === g.platform }" :aria-current="selected?.groupId === g.groupId && selected?.platform === g.platform ? 'true' : undefined" @click="selectGroup(g)">
              <div class="im-item-heading">
                <span class="anime-badge" :class="getPlatformColor(g.platform)">{{ getPlatformLabel(g.platform) }}</span>
                <span class="im-item-name">{{ getGroupName(g) }}</span>
                <span v-if="g.appName" class="anime-badge blue">{{ g.appName }}</span>
              </div>
              <div class="im-item-meta">
                <span class="im-message-count">{{ g.messageCount }} 条消息</span>
                <span v-if="g.lastMessageTime">{{ formatTime(g.lastMessageTime) }}</span>
              </div>
              <div class="im-item-owner">
                <span v-if="g.createdBy">归属: {{ g.createdBy }}</span>
              </div>
            </button>
          </div>

          <div class="im-detail-panel">
            <template v-if="!selected">
              <div class="anime-empty">
                <div class="anime-empty-text">请选择左侧群聊查看消息</div>
              </div>
            </template>
            <template v-else>
              <div class="im-detail-heading">
                <span class="anime-badge" :class="getPlatformColor(selected.platform)">{{ getPlatformLabel(selected.platform) }}</span>
                <span class="im-detail-title">{{ getGroupName(selected) }}</span>
              </div>
              
              <div v-if="!selected.createdBy || selected.createdBy === currentUserName" class="im-binding-panel">
                <div class="im-binding-title">应用绑定</div>
                <div class="im-binding-controls">
                  <select v-model="bindAppId" class="anime-app-select im-binding-select" aria-label="选择群聊绑定应用">
                    <option value="">不绑定应用</option>
                    <option v-for="app in appList" :key="app.id" :value="app.id">{{ app.icon || '🤖' }} {{ app.name }}</option>
                  </select>
                  <button class="anime-btn primary" @click="saveBindApp" :disabled="bindSaving">
                    <span v-if="bindSaving">保存中...</span>
                    <span v-else>保存绑定</span>
                  </button>
                </div>
                <div v-if="selected.appName" class="im-current-binding">
                  当前绑定：<span>{{ selected.appName }}</span>
                </div>
              </div>
              
              <div class="im-message-toolbar">
                <input v-model="searchKeyword" type="text" aria-label="搜索群聊消息内容或用户ID" placeholder="搜索消息内容或用户ID..." class="anime-search-input im-message-search" @keyup.enter="searchMessages" />
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
                <div class="anime-empty-text">{{ searchKeyword ? '未找到匹配消息' : '暂无消息记录' }}</div>
              </div>
              <div v-else class="message-list">
                <div v-for="m in messages" :key="m.id" class="im-message-card">
                  <div class="im-message-meta">
                    <span class="anime-badge blue">用户: {{ m.userId || '未知' }}</span>
                    <span class="anime-badge muted">{{ m.messageType || '文本' }}</span>
                    <span class="anime-badge im-sync-state" :class="m.synced ? 'im-sync-state--synced' : 'im-sync-state--unsynced'">{{ m.synced ? '已同步' : '未同步' }}</span>
                    <span class="im-message-time">{{ formatTime(m.messageTime) }}</span>
                  </div>
                  <div class="im-message-body">{{ m.rawMessage }}</div>
                </div>
              </div>
              <div v-if="msgTotal > messages.length" class="im-pagination">
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
import { fetchGroups, fetchGroupMessages } from '../api/console'
import { api } from '../api/client'
import type { GroupMessageItem, GroupSummary } from '../types/console'

interface KbApp {
  id: number
  name: string
  icon: string
  isDefault: boolean
}

const currentUserName = computed(() => localStorage.getItem('chatbase_original_username') || localStorage.getItem('chatbase_user') || '')

const filteredGroups = computed(() => {
  const q = groupSearch.value.trim().toLowerCase()
  if (!q) return groups.value
  return groups.value.filter(g => {
    const id = String(g.groupId ?? '').toLowerCase()
    const name = String(g.groupName ?? '').toLowerCase()
    return id.includes(q) || name.includes(q)
  })
})

const platformTabs = [{ key: 'qq', label: 'QQ 群' }, { key: 'wecom', label: '企微群' }, { key: 'wx', label: '微信群' }]
const platform = ref<string | null>(null)
const scope = ref('all')
const groupSearch = ref('')
const groups = ref<GroupSummary[]>([])
const loading = ref(false)
const err = ref<string | null>(null)

const selected = ref<GroupSummary | null>(null)
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
    groups.value = await fetchGroups(platform.value ?? 'all', scope.value)
    if (selected.value) {
      const still = groups.value.find((g) => g.groupId === selected.value!.groupId && g.platform === selected.value!.platform)
      if (!still) { selected.value = null; messages.value = [] }
      else { selected.value = still; bindAppId.value = still.appId || '' }
    }
  } catch (e: any) { err.value = e?.message || '加载失败'; groups.value = [] }
  finally { loading.value = false }
}

function selectGroup(g: GroupSummary) { 
  selected.value = g
  bindAppId.value = g.appId || ''
  page.value = 0
  loadMessages() 
}

async function loadMessages() {
  if (!selected.value) return
  const gid = selected.value.groupId
  if (gid == null || String(gid).trim() === '') { messages.value = []; msgTotal.value = 0; return }
  msgLoading.value = true
  try {
    const apiPlat = selected.value.platform === 'qq' ? 'qq' : selected.value.platform === 'wx' ? 'wx' : 'all'
    const res = await fetchGroupMessages({ 
      groupId: gid, 
      platform: apiPlat, 
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
      await api.put(`/console/groups/${selected.value.id}/app`, { appId, appName })
      selected.value.appId = appId
      selected.value.appName = appName
    } else {
      await api.delete(`/console/groups/${selected.value.id}/app`)
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
function getPlatformLabel(p: string): string { if (p === 'qq') return 'QQ群'; if (p === 'wecom') return '企微群'; if (p === 'wx') return '微信群'; return '群聊' }
function getGroupName(g: GroupSummary): string {
  if (g.groupName && g.groupName.trim()) return g.groupName.trim()
  if (g.platform === 'qq') return `QQ群`
  if (g.platform === 'wecom') return `企微群`
  if (g.platform === 'wx') return `微信群`
  return `群聊`
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
.im-page-error { margin: 0 24px 16px; }
.im-toolbar, .im-message-toolbar, .im-binding-controls {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
.im-toolbar { margin-bottom: 14px; }
.im-toolbar .anime-tabs { margin: 0; }
.im-scope-select { flex: 0 0 auto; min-width: 112px; }
.im-list-search { flex: 1 1 220px; min-width: min(220px, 100%); }
.im-split-view {
  display: grid;
  grid-template-columns: minmax(240px, 320px) minmax(0, 1fr);
  min-height: 480px;
  overflow: hidden;
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-surface);
}
.im-list-panel { min-width: 0; max-height: min(70vh, 600px); overflow-y: auto; padding: 12px; border-right: 1px solid var(--ui-border); }
.im-group-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  min-width: 0;
  padding: 12px;
  margin: 0 0 8px;
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-surface);
  color: var(--ui-text);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color .16s ease, background-color .16s ease;
}
.im-group-item:hover { border-color: color-mix(in srgb, var(--ui-primary) 45%, var(--ui-border)); background: var(--ui-surface-muted); }
.im-group-item.active { border-color: var(--ui-primary); background: var(--ui-primary-soft); box-shadow: inset 3px 0 var(--ui-primary); }
.im-item-heading, .im-item-meta { display: flex; align-items: center; gap: 8px; min-width: 0; }
.im-item-heading { flex-wrap: wrap; }
.im-item-name { flex: 1 1 90px; min-width: 0; color: var(--ui-text); font-weight: 650; overflow-wrap: anywhere; }
.im-item-meta { justify-content: space-between; color: var(--ui-text-muted); font-size: 12px; }
.im-message-count { color: var(--ui-primary); font-weight: 600; }
.im-item-owner { min-height: 16px; color: var(--ui-text-muted); font-size: 12px; }
.im-detail-panel { display: flex; min-width: 0; max-height: min(70vh, 600px); flex-direction: column; gap: 14px; overflow-y: auto; padding: 18px; }
.im-detail-heading { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; }
.im-detail-title { min-width: 0; color: var(--ui-text); font-weight: 650; overflow-wrap: anywhere; }
.im-binding-panel { margin-bottom: 2px; padding: 14px; border: 1px solid var(--ui-border); border-radius: var(--ui-radius-md); background: var(--ui-surface-muted); }
.im-binding-title { margin-bottom: 10px; color: var(--ui-text-secondary); font-size: 13px; font-weight: 700; }
.im-binding-controls { align-items: stretch; }
.im-binding-select { flex: 1 1 220px; min-width: min(200px, 100%); }
.im-current-binding { margin-top: 9px; color: var(--ui-text-muted); font-size: 12px; overflow-wrap: anywhere; }
.im-current-binding span { color: var(--ui-primary); font-weight: 600; }
.im-message-search { flex: 1 1 220px; min-width: min(220px, 100%); }
.im-message-toolbar .anime-btn { flex: 0 0 auto; }
.message-list { display: flex; min-width: 0; flex-direction: column; gap: 9px; }
.im-message-card { min-width: 0; padding: 12px; border: 1px solid var(--ui-border); border-radius: var(--ui-radius-md); background: var(--ui-surface); }
.im-message-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 7px; margin-bottom: 8px; }
.im-message-time { color: var(--ui-text-muted); font-size: 12px; }
.im-message-body { color: var(--ui-text); font-size: 14px; line-height: 1.6; white-space: pre-wrap; overflow-wrap: anywhere; }
.im-sync-state--synced { background: color-mix(in srgb, var(--ui-success) 12%, var(--ui-surface)); color: var(--ui-success); }
.im-sync-state--unsynced { background: color-mix(in srgb, var(--ui-accent) 14%, var(--ui-surface)); color: color-mix(in srgb, var(--ui-accent) 55%, var(--ui-text)); }
.im-pagination { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; padding-top: 12px; border-top: 1px solid var(--ui-border); }
.anime-app-select, .anime-search-input, .anime-scope-select {
  min-height: 38px;
  padding: 8px 11px;
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-sm);
  background: var(--ui-surface);
  color: var(--ui-text);
  font: inherit;
  box-shadow: none;
}
.anime-app-select:focus, .anime-search-input:focus, .anime-scope-select:focus { border-color: var(--ui-focus); outline: 2px solid color-mix(in srgb, var(--ui-focus) 18%, transparent); }
@media (max-width: 800px) {
  .im-split-view { grid-template-columns: minmax(0, 1fr); }
  .im-list-panel { max-height: min(34vh, 280px); border-right: 0; border-bottom: 1px solid var(--ui-border); }
  .im-detail-panel { max-height: min(70vh, 680px); }
}
@media (max-width: 480px) {
  .im-page-error { margin: 0 14px 14px; }
  .im-toolbar { align-items: stretch; }
  .im-toolbar .anime-tabs { flex: 1 1 100%; }
  .im-scope-select { flex: 1 1 100%; }
  .im-list-search { flex-basis: 100%; }
  .im-detail-panel { padding: 14px; }
  .im-binding-controls > * { flex: 1 1 100%; }
  .im-message-toolbar .anime-btn { flex: 1 1 auto; }
}
</style>
