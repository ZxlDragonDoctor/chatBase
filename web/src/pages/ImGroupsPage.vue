<template>
  <div class="anime-page-shell">
    <section class="anime-card">
      <div class="anime-card-header">
        <div>
          <div class="anime-card-title">群聊采集</div>
          <div class="anime-card-desc">
            <span class="anime-code">group_message</span> 表数据 · QQ/企微群聊消息采集
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
        <div class="anime-tabs" style="margin-bottom: 16px;">
          <button v-for="t in platformTabs" :key="t.key" class="anime-tab" :class="{ active: platform === t.key }" @click="platform = t.key">{{ t.label }}</button>
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
            <button v-for="g in groups" :key="g.platform + ':' + g.groupId" class="im-group-item" :class="{ 'active': selected?.groupId === g.groupId && selected?.platform === g.platform }" @click="selectGroup(g)">
              <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 8px;">
                <span class="anime-badge" :class="getPlatformColor(g.platform)">{{ g.platform }}</span>
                <span class="anime-code" style="font-size: 12px;">{{ g.groupId }}</span>
              </div>
              <div style="display: flex; justify-content: space-between; font-size: 13px; color: var(--anime-text-muted);">
                <span style="color: var(--anime-blue);">{{ g.messageCount }} 条消息</span>
                <span v-if="g.lastMessageTime">{{ formatTime(g.lastMessageTime) }}</span>
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
                <span class="anime-badge" :class="getPlatformColor(selected.platform)">{{ selected.platform }}</span>
                <span class="anime-code">{{ selected.groupId }}</span>
              </div>
              <div v-if="msgLoading" class="anime-empty">
                <span class="anime-loader-spinner"></span>
                <span class="anime-empty-text">消息加载中...</span>
              </div>
              <div v-else class="message-list">
                <div v-for="m in messages" :key="m.id" style="padding: 14px; background: rgba(255, 183, 197, 0.05); border: 2px solid var(--anime-border); border-radius: var(--anime-radius-lg); margin-bottom: 12px;">
                  <div style="display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 8px;">
                    <span class="anime-badge muted">{{ m.messageType || 'text' }}</span>
                    <span class="anime-badge" :class="m.synced ? 'green' : 'pink'">{{ m.synced ? '已同步' : '未同步' }}</span>
                    <span style="font-size: 13px; color: var(--anime-text-muted);">{{ formatTime(m.messageTime) }}</span>
                    <span class="anime-pill" style="font-size: 12px;">用户: {{ m.userId || '—' }}</span>
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
import { ref, watch, onMounted } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import { fetchGroups, fetchGroupMessages } from '../api/console'
import type { GroupMessageItem, GroupSummary } from '../types/console'

const platformTabs = [{ key: 'all' as const, label: '全部' }, { key: 'qq' as const, label: 'QQ 群' }, { key: 'wecom' as const, label: '企微群' }]
const platform = ref<'all' | 'qq' | 'wecom'>('all')
const groups = ref<GroupSummary[]>([])
const loading = ref(false)
const err = ref<string | null>(null)

const selected = ref<GroupSummary | null>(null)
const messages = ref<GroupMessageItem[]>([])
const msgLoading = ref(false)
const msgTotal = ref(0)
const page = ref(0)
const pageSize = 40

async function reload() {
  loading.value = true
  err.value = null
  try {
    groups.value = await fetchGroups(platform.value)
    if (selected.value) {
      const still = groups.value.find((g) => g.groupId === selected.value!.groupId && g.platform === selected.value!.platform)
      if (!still) { selected.value = null; messages.value = [] }
    }
  } catch (e: any) { err.value = e?.message || '加载失败'; groups.value = [] }
  finally { loading.value = false }
}

function selectGroup(g: GroupSummary) { selected.value = g; page.value = 0; loadMessages() }

async function loadMessages() {
  if (!selected.value) return
  const gid = selected.value.groupId
  if (gid == null || String(gid).trim() === '') { messages.value = []; msgTotal.value = 0; return }
  msgLoading.value = true
  try {
    const apiPlat = selected.value.platform === 'qq' ? 'qq' : selected.value.platform === 'wx' ? 'wx' : 'all'
    const res = await fetchGroupMessages({ groupId: gid, platform: apiPlat, page: page.value, size: pageSize })
    messages.value = res.records
    msgTotal.value = res.total
  } catch { messages.value = []; msgTotal.value = 0 }
  finally { msgLoading.value = false }
}

function getPlatformColor(p: string): string { if (p === 'qq') return 'green'; if (p === 'wecom') return 'blue'; return 'purple' }
function formatTime(t: any): string {
  if (!t) return ''
  if (Array.isArray(t) && t.length >= 5) {
    const [y, m, d, h = 0, min = 0] = t
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}`
  }
  if (typeof t === 'string') return t.slice(0, 16).replace('T', ' ')
  return String(t)
}

watch(platform, () => { selected.value = null; messages.value = []; reload() })
onMounted(reload)
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
</style>