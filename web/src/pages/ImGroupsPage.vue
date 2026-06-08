<template>
  <div class="anime-page-shell">
    <section class="anime-card">
      <div class="anime-card-header">
        <div>
          <div class="anime-card-title">群聊采集</div>
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
          <input v-model="groupSearch" type="text" placeholder="搜索群名或群ID..." class="anime-search-input" style="min-width: 200px; flex: 1;" />
        </div>

              <div class="im-split-view">
                <div class="im-list-panel">
                  <div v-if="loading && groups.length === 0" class="anime-empty">
                    <span class="anime-loader-spinner"></span>
                    <span class="anime-empty-text">加载中...</span>
                  </div>
                  <div v-else-if="groups.length === 0" class="anime-empty">
                    <div class="anime-empty-icon">📭</div>
                    <div class="anime-empty-text">暂无群聊记录</div>
                  </div>
                  <button v-for="g in filteredGroups" :key="g.platform + ':' + g.groupId" class="im-group-item" :class="{ 'active': selected?.groupId === g.groupId && selected?.platform === g.platform }" @click="selectGroup(g)">
                    <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 8px;">
                      <span class="anime-badge" :class="getPlatformColor(g.platform)">{{ getPlatformLabel(g.platform) }}</span>
                      <span style="font-weight: 600; color: var(--anime-text-primary);">{{ getGroupName(g) }}</span>
                      <span v-if="g.appName" class="anime-badge blue" style="margin-left: 4px;">{{ g.appName }}</span>
                    </div>
                    <div style="display: flex; justify-content: space-between; font-size: 13px; color: var(--anime-text-muted);">
                      <span style="color: var(--anime-blue);">{{ g.messageCount }} 条消息</span>
                      <span v-if="g.lastMessageTime">{{ formatTime(g.lastMessageTime) }}</span>
                    </div>
                    <div style="font-size: 12px; color: var(--anime-text-muted); margin-top: 4px;">
                      <span v-if="g.createdBy">归属: {{ g.createdBy }}</span>
                    </div>
                  </button>
                </div>

          <div class="im-detail-panel">
            <template v-if="!selected">
              <div class="anime-empty">
                <div class="anime-empty-icon">👈</div>
                <div class="anime-empty-text">请选择左侧群聊查看消息 ✿</div>
              </div>
            </template>
            <template v-else>
              <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 16px;">
                <span class="anime-badge" :class="getPlatformColor(selected.platform)">{{ getPlatformLabel(selected.platform) }}</span>
                <span style="font-weight: 600; color: var(--anime-text-primary);">{{ getGroupName(selected) }}</span>
              </div>
              
              <div v-if="!selected.createdBy || selected.createdBy === currentUserName" style="padding: 16px; background: rgba(168, 216, 234, 0.1); border: 2px solid var(--anime-border); border-radius: var(--anime-radius-lg); margin-bottom: 16px;">
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
                <div v-for="m in messages" :key="m.id" style="padding: 14px; background: rgba(255, 183, 197, 0.05); border: 2px solid var(--anime-border); border-radius: var(--anime-radius-lg); margin-bottom: 12px;">
                  <div style="display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 8px;">
                    <span class="anime-badge blue">用户: {{ m.userId || '未知' }}</span>
                    <span class="anime-badge muted">{{ m.messageType || '文本' }}</span>
                    <span class="anime-badge" :class="m.synced ? 'green' : 'pink'">{{ m.synced ? '已同步' : '未同步' }}</span>
                    <span style="font-size: 13px; color: var(--anime-text-muted);">{{ formatTime(m.messageTime) }}</span>
                  </div>
                  <div style="font-size: 14px; color: var(--anime-text-primary); white-space: pre-wrap; line-height: 1.5;">{{ m.rawMessage }}</div>
                </div>
              </div>
              <div v-if="msgTotal > messages.length" style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap; padding-top: 12px; border-top: 2px solid var(--anime-border);">
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
.im-split-view { display: grid; grid-template-columns: minmax(240px, 320px) 1fr; gap: 0; min-height: 480px; }
@media (max-width: 800px) { .im-split-view { grid-template-columns: 1fr; } }
.im-list-panel { border-right: 2px solid var(--anime-border); max-height: min(70vh, 600px); overflow-y: auto; padding: 12px; }
.im-group-item { display: flex; flex-direction: column; gap: 10px; width: 100%; padding: 14px; margin-bottom: 10px; background: var(--anime-bg-card); border: 2px solid var(--anime-border); border-radius: var(--anime-radius-lg); cursor: pointer; transition: all 0.3s ease; }
.im-group-item:hover { border-color: var(--anime-pink); transform: translateX(4px); }
.im-group-item.active { border-color: var(--anime-pink); box-shadow: var(--anime-shadow-soft); }
.im-detail-panel { padding: 20px; max-height: min(70vh, 600px); overflow-y: auto; display: flex; flex-direction: column; gap: 16px; }
.message-list { display: flex; flex-direction: column; gap: 12px; }
.animate-spin { animation: spin 1s linear infinite; }
.anime-app-select {
  background: var(--anime-bg);
  border: 2px solid var(--anime-border);
  border-radius: var(--anime-radius-lg);
  padding: 8px 12px;
  font-size: 14px;
  color: var(--anime-text-primary);
  cursor: pointer;
  outline: none;
}
.anime-app-select:focus {
  border-color: var(--anime-pink);
}
.anime-search-input {
  background: var(--anime-bg);
  border: 2px solid var(--anime-border);
  border-radius: var(--anime-radius-lg);
  padding: 8px 12px;
  font-size: 14px;
  color: var(--anime-text-primary);
  outline: none;
  transition: border-color 0.3s ease;
}
.anime-search-input:focus {
  border-color: var(--anime-blue);
}
.anime-scope-select {
  background: var(--anime-bg);
  border: 2px solid var(--anime-border);
  border-radius: var(--anime-radius-lg);
  padding: 6px 10px;
  font-size: 14px;
  color: var(--anime-text-primary);
  cursor: pointer;
  outline: none;
}
.anime-scope-select:focus {
  border-color: var(--anime-pink);
}
</style>