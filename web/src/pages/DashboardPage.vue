<template>
  <div class="anime-page-shell dashboard-page">
    <header class="dash-page-header">
      <div>
        <h1 class="dash-greeting-title">你好，{{ displayName }} ✦</h1>
        <p class="dash-greeting-sub">欢迎回到 ChatBase 智能助手管理系统</p>
      </div>
      <button class="anime-btn ghost dash-refresh" :disabled="loading" @click="loadData">
        <RefreshCw :size="16" :class="{ 'animate-spin': loading }" />
        <span>刷新</span>
      </button>
    </header>

    <div v-if="error" class="anime-error dash-page-error" role="alert">{{ error }}</div>

    <section class="dash-today" aria-label="今日运行情况">
      <div class="dash-section-heading">
        <div>
          <p class="dash-eyebrow">今日</p>
          <h2 class="dash-section-title">运行概况</h2>
        </div>
      </div>
      <div class="dash-today-grid">
        <article class="dash-today-card">
          <div class="dash-today-mark conversations"><MessageCircle :size="18" /></div>
          <div class="dash-today-copy">
            <span class="dash-today-label">今日问答记录</span>
            <strong class="dash-today-value">{{ overview?.todayConversations ?? '—' }}</strong>
          </div>
          <span class="dash-today-period">今天</span>
        </article>
        <article class="dash-today-card">
          <div class="dash-today-mark tokens"><Zap :size="18" /></div>
          <div class="dash-today-copy">
            <span class="dash-today-label">今日 Token</span>
            <strong class="dash-today-value">{{ overview ? formatNumber(overview.todayTokens) : '—' }}</strong>
          </div>
          <span class="dash-today-period">今天</span>
        </article>
        <article class="dash-today-card">
          <div class="dash-today-mark success"><HelpCircle :size="18" /></div>
          <div class="dash-today-copy">
            <span class="dash-today-label">今日回答成功率</span>
            <strong class="dash-today-value success">{{ todaySuccessRateLabel }}</strong>
          </div>
          <span class="dash-today-period">今天</span>
        </article>
      </div>
    </section>

    <section class="dash-history" aria-label="累计与近30日指标">
      <div class="dash-section-heading dash-history-heading">
        <div>
          <p class="dash-eyebrow">历史指标</p>
          <h2 class="dash-section-title">累计与近30日</h2>
        </div>
      </div>
      <div class="dash-stat-grid">
        <article v-for="s in statCards" :key="s.label" class="dash-stat-card">
          <div class="dash-stat-icon" :style="{ background: s.iconBg, color: s.color }">
            <component :is="s.icon" :size="18" />
          </div>
          <div class="dash-stat-body">
            <strong class="dash-stat-number">{{ s.value }}</strong>
            <span class="dash-stat-label">{{ s.label }}</span>
          </div>
        </article>
      </div>
    </section>

    <div class="dash-workspace-grid">
      <div class="dash-main-column">
        <section class="dash-section dash-section-card">
          <div class="dash-section-header">
            <h2 class="dash-section-title">
              <Cpu :size="18" />
              <span>机器人状态</span>
            </h2>
            <RouterLink to="/console/bots" class="anime-btn ghost sm">管理</RouterLink>
          </div>
          <div class="dash-bot-grid">
            <div v-for="bot in botStatusList" :key="bot.platform" class="dash-bot-card">
              <div class="dash-bot-left">
                <div class="dash-bot-icon" :class="bot.platform">
                  <span v-if="bot.platform === 'qq'">QQ</span>
                  <span v-else-if="bot.platform === 'wecom'">企微</span>
                  <span v-else-if="bot.platform === 'wx'">微信</span>
                  <span v-else>Bot</span>
                </div>
                <div class="dash-bot-info">
                  <div class="dash-bot-name">{{ bot.name }}</div>
                  <div class="dash-bot-meta">{{ bot.botId || (bot.platform === 'wecom' ? 'HTTP 回调' : '-') }}</div>
                </div>
              </div>
              <div class="dash-bot-right">
                <span class="dash-bot-status" :class="bot.online ? 'online' : bot.statusLabel === '回调已配置' ? 'configured' : 'offline'">
                  <span class="dash-bot-dot" :class="bot.online ? 'online' : bot.statusLabel === '回调已配置' ? 'configured' : 'offline'"></span>
                  {{ bot.statusLabel }}
                </span>
              </div>
            </div>
            <div v-if="loading && botStatusList.length === 0" class="dash-bot-empty" role="status" aria-live="polite">正在加载机器人状态…</div>
            <div v-else-if="botsError" class="dash-bot-empty">机器人状态暂时无法加载</div>
            <div v-else-if="botStatusList.length === 0" class="dash-bot-empty" role="status">暂无机器人配置</div>
          </div>
        </section>

        <section class="dash-section dash-section-card dash-operations">
          <div class="dash-section-header">
            <h2 class="dash-section-title">
              <BarChart3 :size="18" />
              <span>系统概览</span>
            </h2>
            <RouterLink to="/console/statistics" class="anime-btn ghost sm">详情</RouterLink>
          </div>
          <div class="dash-info-grid">
            <div class="dash-info-item">
              <span class="dash-info-label">累计 IM 消息</span>
              <strong class="dash-info-value">{{ overview ? formatNumber(overview.totalMessages) : '—' }}</strong>
            </div>
            <div class="dash-info-item">
              <span class="dash-info-label">知识库文档</span>
              <strong class="dash-info-value">{{ overview?.documents ?? '—' }}</strong>
            </div>
            <div class="dash-info-item">
              <span class="dash-info-label">累计平均响应耗时</span>
              <strong class="dash-info-value">{{ averageLatencyLabel }}</strong>
            </div>
          </div>
        </section>
      </div>

      <aside class="dash-side-column">
        <section class="dash-section dash-section-card dash-quick-section">
          <div class="dash-section-header">
            <h2 class="dash-section-title">
              <Zap :size="18" />
              <span>快速入口</span>
            </h2>
          </div>
          <div class="dash-quick-actions">
            <RouterLink v-for="a in quickActions" :key="a.to" :to="a.to" class="dash-quick-btn">
              <span class="dash-quick-icon"><component :is="a.icon" :size="18" /></span>
              <span>{{ a.label }}</span>
            </RouterLink>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { RouterLink } from 'vue-router'
import { RefreshCw, MessageCircle, BookOpen, BarChart3, Users, HelpCircle, MessageSquare, Zap, Cpu } from 'lucide-vue-next'
import { api } from '../api/client'
import type { SystemOverview } from '../types/statistics'
import type { BotInfo } from '../api/bot'
import { listBots } from '../api/bot'

const loading = ref(true)
const overviewError = ref(false)
const botsError = ref(false)
const error = computed(() => [
  overviewError.value ? '统计数据加载失败，请刷新重试。' : '',
  botsError.value ? '机器人状态加载失败，请刷新重试。' : '',
].filter(Boolean).join(''))

const displayName = computed(() => localStorage.getItem('chatbase_user') || '用户')

const overview = ref<SystemOverview | null>(null)
const bots = ref<BotInfo[]>([])

const botStatusList = computed(() => {
  return bots.value.map(bot => ({
    ...bot,
    statusLabel: bot.statusLabel || (bot.online ? '在线' : '离线'),
  }))
})

const statCards = computed(() => {
  const s = overview.value
  return [
    { label: '累计问答记录', value: s ? s.totalConversations : '—', icon: MessageCircle, iconBg: 'var(--ui-primary-soft)', color: 'var(--ui-primary)' },
    { label: '累计 Token', value: s ? formatNumber(s.totalTokens) : '—', icon: Zap, iconBg: '#fbf2e5', color: 'var(--ui-accent)' },
    { label: '近30日活跃群聊', value: s ? s.activeGroups : '—', icon: Users, iconBg: '#e9f2f2', color: 'var(--anime-blue)' },
    { label: '知识库数量', value: s ? s.knowledgeBases : '—', icon: BookOpen, iconBg: 'var(--ui-surface-muted)', color: 'var(--ui-text-secondary)' },
    { label: '近30日群聊用户', value: s ? s.activeUsers : '—', icon: Users, iconBg: '#edf4ee', color: 'var(--ui-success)' },
    { label: '累计回答成功率', value: s ? successRateLabel(s.totalConversations, s.successRate) : '—', icon: HelpCircle, iconBg: 'var(--ui-primary-soft)', color: 'var(--ui-primary)' },
  ]
})

const todaySuccessRateLabel = computed(() => {
  const s = overview.value
  return s ? successRateLabel(s.todayConversations, s.todaySuccessRate) : '—'
})

const averageLatencyLabel = computed(() => {
  const value = overview.value?.avgLatencyMs
  if (value == null || value <= 0) return '—'
  return value >= 1000 ? `${(value / 1000).toFixed(2)} s` : `${Math.round(value)} ms`
})

const quickActions = [
  { label: 'AI 问答', to: '/chat', icon: MessageSquare },
  { label: '知识库', to: '/console/knowledge', icon: BookOpen },
  { label: '数据统计', to: '/console/statistics', icon: BarChart3 },
  { label: '群聊管理', to: '/console/im', icon: Users },
  { label: 'FAQ', to: '/console/faq', icon: HelpCircle },
  { label: '提交反馈', to: '/feedback', icon: MessageCircle },
]

function formatNumber(n: number): string {
  if (n >= 1000000) return (n / 1000000).toFixed(1) + 'M'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'K'
  return n.toString()
}

function successRateLabel(total: number, rate: number): string {
  return total > 0 ? `${rate.toFixed(1)}%` : '—'
}

async function loadOverview() {
  try {
    const res = await api.get<SystemOverview>('/statistics/system/overview')
    overview.value = res.data
    overviewError.value = false
  } catch {
    overview.value = null
    overviewError.value = true
  }
}

async function loadBotList() {
  try {
    bots.value = await listBots()
    botsError.value = false
  } catch {
    bots.value = []
    botsError.value = true
  }
}

async function loadData() {
  loading.value = true
  await Promise.all([loadOverview(), loadBotList()])
  loading.value = false
}

let interval: number | undefined

onMounted(() => {
  loadData()
  interval = window.setInterval(loadOverview, 30000)
})

onUnmounted(() => {
  if (interval) clearInterval(interval)
})
</script>

<style scoped>
.dashboard-page {
  --dash-card-radius: var(--ui-radius-lg);
}

.dash-page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
}

.dash-greeting-title {
  margin: 0;
  color: var(--ui-text);
  font-size: clamp(24px, 2.3vw, 30px);
  line-height: 1.2;
  font-weight: 750;
  letter-spacing: -0.045em;
}

.dash-greeting-sub {
  margin: 7px 0 0;
  color: var(--ui-text-secondary);
  font-size: 14px;
}

.dash-refresh { flex-shrink: 0; }
.dash-page-error { margin-bottom: 20px; }

.dash-section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.dash-eyebrow {
  margin: 0 0 3px;
  color: var(--ui-text-muted);
  font-size: 10px;
  font-weight: 750;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.dash-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: var(--ui-text);
  font-size: 16px;
  font-weight: 700;
}

.dash-today { margin-bottom: 25px; }

.dash-today-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.dash-today-card {
  min-width: 0;
  min-height: 108px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 19px 20px;
  background: var(--ui-surface);
  border: 1px solid var(--ui-border);
  border-radius: var(--dash-card-radius);
  box-shadow: var(--ui-shadow-card);
}

.dash-today-mark {
  width: 42px;
  height: 42px;
  display: grid;
  flex-shrink: 0;
  place-items: center;
  border-radius: var(--ui-radius-md);
}

.dash-today-mark.conversations { background: var(--ui-primary-soft); color: var(--ui-primary); }
.dash-today-mark.tokens { background: #fbf2e5; color: var(--ui-accent); }
.dash-today-mark.success { background: #edf4ee; color: var(--ui-success); }

.dash-today-copy {
  min-width: 0;
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 6px;
}

.dash-today-label,
.dash-stat-label {
  color: var(--ui-text-muted);
  font-size: 12px;
  font-weight: 550;
}

.dash-today-value {
  color: var(--ui-text);
  font-size: clamp(23px, 2vw, 30px);
  line-height: 1;
  font-weight: 760;
  font-variant-numeric: tabular-nums;
  overflow-wrap: anywhere;
}

.dash-today-value.success { color: var(--ui-success); }

.dash-today-period {
  align-self: flex-start;
  padding: 4px 8px;
  color: var(--ui-text-secondary);
  background: var(--ui-surface-muted);
  border-radius: var(--ui-radius-sm);
  font-size: 10px;
  font-weight: 650;
}

.dash-history { margin-bottom: 28px; }
.dash-history-heading { margin-bottom: 10px; }

.dash-stat-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}

.dash-stat-card {
  min-width: 0;
  min-height: 72px;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 12px;
  background: var(--ui-surface);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
}

.dash-stat-icon {
  width: 34px;
  height: 34px;
  display: grid;
  flex-shrink: 0;
  place-items: center;
  border-radius: var(--ui-radius-sm);
}

.dash-stat-body {
  min-width: 0;
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4px;
}

.dash-stat-number {
  color: var(--ui-text);
  font-size: 17px;
  line-height: 1.15;
  font-weight: 730;
  font-variant-numeric: tabular-nums;
}

.dash-workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(230px, 0.34fr);
  align-items: start;
  gap: 18px;
}

.dash-main-column,
.dash-side-column {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dash-section { margin: 0; }

.dash-section-card {
  padding: 20px;
  background: var(--ui-surface);
  border: 1px solid var(--ui-border);
  border-radius: var(--dash-card-radius);
  box-shadow: var(--ui-shadow-card);
}

.dash-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.dash-bot-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.dash-bot-card {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 13px;
  background: var(--ui-surface-muted);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
}

.dash-bot-left {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.dash-bot-icon {
  width: 36px;
  height: 36px;
  display: grid;
  flex-shrink: 0;
  place-items: center;
  border-radius: var(--ui-radius-sm);
  color: #fff;
  font-size: 11px;
  font-weight: 750;
}

.dash-bot-icon.qq { background: #397987; }
.dash-bot-icon.wecom { background: #21845b; }
.dash-bot-icon.wx { background: #25815c; }

.dash-bot-name {
  overflow: hidden;
  color: var(--ui-text);
  font-size: 14px;
  font-weight: 680;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dash-bot-meta {
  margin-top: 2px;
  color: var(--ui-text-muted);
  font-size: 12px;
}

.dash-bot-right { display: flex; align-items: center; }

.dash-bot-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 680;
  white-space: nowrap;
}

.dash-bot-status.online { background: #e9f4ed; color: var(--ui-success); }
.dash-bot-status.offline { background: #eef1ef; color: var(--ui-text-muted); }
.dash-bot-status.configured { background: var(--ui-primary-soft); color: var(--ui-primary); }

.dash-bot-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.dash-bot-dot.online { background: var(--ui-success); }
.dash-bot-dot.offline { background: var(--ui-text-muted); }
.dash-bot-dot.configured { background: var(--ui-primary); }

.dash-bot-empty {
  grid-column: 1 / -1;
  padding: 26px 16px;
  color: var(--ui-text-muted);
  font-size: 13px;
  text-align: center;
}

.dash-quick-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.dash-quick-btn {
  min-height: 88px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 9px;
  padding: 12px 8px;
  color: var(--ui-text-secondary);
  background: var(--ui-surface-muted);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  font-size: 12px;
  font-weight: 630;
  text-decoration: none;
  transition: background-color 0.18s ease, border-color 0.18s ease, color 0.18s ease, transform 0.18s ease;
}

.dash-quick-btn:hover {
  color: var(--ui-primary);
  background: var(--ui-primary-soft);
  border-color: #a6d2c8;
  transform: translateY(-1px);
}

.dash-quick-btn:focus-visible {
  outline: 2px solid var(--ui-focus);
  outline-offset: 2px;
}

.dash-quick-icon { color: var(--ui-primary); }

.dash-info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.dash-info-item {
  min-height: 86px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  background: var(--ui-surface-muted);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
}

.dash-info-label {
  color: var(--ui-text-muted);
  font-size: 11px;
  font-weight: 600;
}

.dash-info-value {
  color: var(--ui-text);
  font-size: 20px;
  line-height: 1.15;
  font-weight: 730;
  font-variant-numeric: tabular-nums;
}

.animate-spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 1180px) {
  .dash-stat-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}

@media (max-width: 900px) {
  .dash-workspace-grid { grid-template-columns: minmax(0, 1fr); }
  .dash-side-column { order: -1; }
  .dash-quick-actions { grid-template-columns: repeat(6, minmax(0, 1fr)); }
  .dash-quick-btn { min-height: 76px; }
}

@media (max-width: 640px) {
  .dash-page-header { align-items: flex-start; }
  .dash-refresh { padding: 8px 10px; }
  .dash-today-grid { grid-template-columns: 1fr; gap: 8px; }
  .dash-today-card { min-height: 82px; padding: 14px 16px; }
  .dash-stat-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .dash-bot-grid { grid-template-columns: 1fr; }
  .dash-quick-actions { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .dash-section-card { padding: 16px; }
  .dash-info-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 400px) {
  .dash-page-header { align-items: flex-start; gap: 12px; }
  .dash-greeting-title { font-size: 22px; }
  .dash-quick-actions { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .dash-info-grid { grid-template-columns: 1fr; }
}
</style>
