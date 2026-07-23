<template>
  <div class="anime-page-shell">
    <div class="dash-greeting">
      <div>
        <div class="dash-greeting-title">
          你好，{{ displayName }} ✦
        </div>
        <div class="dash-greeting-sub">
          欢迎回到 ChatBase 智能助手管理系统
        </div>
      </div>
      <button class="anime-btn ghost" @click="loadData">
        <RefreshCw :size="16" :class="{ 'animate-spin': loading }" />
        <span>刷新</span>
      </button>
    </div>

    <div v-if="error" class="anime-error" style="margin-bottom: 20px;">{{ error }}</div>

    <!-- Overview Stats -->
    <div class="dash-stat-grid">
      <div v-for="s in statCards" :key="s.label" class="dash-stat-card">
        <div class="dash-stat-icon" :style="{ background: s.iconBg }">
          <component :is="s.icon" :size="20" />
        </div>
        <div class="dash-stat-body">
          <div class="dash-stat-number" :style="{ color: s.color }">{{ s.value }}</div>
          <div class="dash-stat-label">{{ s.label }}</div>
        </div>
      </div>
    </div>

    <!-- Bot Status -->
    <section class="dash-section">
      <div class="dash-section-header">
        <div class="dash-section-title">
          <Cpu :size="18" />
          <span>机器人状态</span>
        </div>
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
              <div class="dash-bot-meta">{{ bot.botId || bot.description || '-' }}</div>
            </div>
          </div>
          <div class="dash-bot-right">
            <span class="dash-bot-status" :class="bot.online ? 'online' : 'offline'">
              <span class="dash-bot-dot" :class="bot.online ? 'online' : 'offline'"></span>
              {{ bot.online ? '在线' : '离线' }}
            </span>
          </div>
        </div>
        <div v-if="botStatusList.length === 0" class="dash-bot-empty">
          暂无机器人配置
        </div>
      </div>
    </section>

    <!-- Quick Actions & Info -->
    <div class="dash-bottom-grid">
      <section class="dash-section dash-section-card">
        <div class="dash-section-header">
          <div class="dash-section-title">
            <Zap :size="18" />
            <span>快速入口</span>
          </div>
        </div>
        <div class="dash-quick-actions">
          <RouterLink v-for="a in quickActions" :key="a.to" :to="a.to" class="dash-quick-btn">
            <component :is="a.icon" :size="20" />
            <span>{{ a.label }}</span>
          </RouterLink>
        </div>
      </section>

      <section class="dash-section dash-section-card">
        <div class="dash-section-header">
          <div class="dash-section-title">
            <BarChart3 :size="18" />
            <span>系统概览</span>
          </div>
          <RouterLink to="/console/statistics" class="anime-btn ghost sm">详情</RouterLink>
        </div>
        <div class="dash-info-grid">
          <div class="dash-info-item">
            <span class="dash-info-label">今日对话</span>
            <span class="dash-info-value">{{ overview?.totalConversations || 0 }}</span>
          </div>
          <div class="dash-info-item">
            <span class="dash-info-label">今日Token</span>
            <span class="dash-info-value">{{ formatNumber(overview?.totalTokens || 0) }}</span>
          </div>
          <div class="dash-info-item">
            <span class="dash-info-label">活跃群聊</span>
            <span class="dash-info-value">{{ overview?.activeGroups || 0 }}</span>
          </div>
          <div class="dash-info-item">
            <span class="dash-info-label">知识库</span>
            <span class="dash-info-value">{{ overview?.knowledgeBases || 0 }}</span>
          </div>
          <div class="dash-info-item">
            <span class="dash-info-label">用户满意度</span>
            <span class="dash-info-value green">{{ (overview?.successRate || 100).toFixed(1) }}%</span>
          </div>
          <div class="dash-info-item">
            <span class="dash-info-label">系统运行</span>
            <span class="dash-info-value" style="color: var(--anime-green);">正常</span>
          </div>
        </div>
      </section>
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

const loading = ref(false)
const error = ref<string | undefined>(undefined)

const displayName = computed(() => localStorage.getItem('chatbase_user') || '用户')

const overview = ref<SystemOverview | null>(null)
const bots = ref<BotInfo[]>([])

const botStatusList = computed(() => {
  const list: Array<{ platform: string; name: string; botId?: string; description?: string; online: boolean; }> = []
  for (const b of bots.value) {
    list.push({
      platform: b.platform,
      name: b.name,
      botId: b.botId ?? undefined,
      online: b.online,
    })
  }
  if (overview.value?.bots?.qqEnabled) {
    if (!list.find(x => x.platform === 'qq')) {
      list.unshift({ platform: 'qq', name: 'QQ机器人', botId: overview.value.bots.qqSelfId, online: true })
    }
  }
  if (overview.value?.bots?.wxEnabled) {
    if (!list.find(x => x.platform === 'wx')) {
      list.unshift({ platform: 'wx', name: overview.value.bots.wxNickname || '微信机器人', online: true })
    }
  }
  if (overview.value?.bots?.wecomEnabled) {
    if (!list.find(x => x.platform === 'wecom')) {
      list.unshift({ platform: 'wecom', name: '企微机器人', online: true })
    }
  }
  return list
})

const statCards = computed(() => {
  const s = overview.value
  return [
    { label: '总对话', value: s?.totalConversations || 0, icon: MessageCircle, iconBg: 'linear-gradient(135deg, rgba(255,107,157,0.12), rgba(255,107,157,0.04))', color: '#ff6b9d' },
    { label: '总Token', value: formatNumber(s?.totalTokens || 0), icon: Zap, iconBg: 'linear-gradient(135deg, rgba(255,183,77,0.12), rgba(255,183,77,0.04))', color: '#ffb74d' },
    { label: '活跃群聊', value: s?.activeGroups || 0, icon: Users, iconBg: 'linear-gradient(135deg, rgba(79,195,247,0.12), rgba(79,195,247,0.04))', color: '#4fc3f7' },
    { label: '总知识库', value: s?.knowledgeBases || 0, icon: BookOpen, iconBg: 'linear-gradient(135deg, rgba(179,157,219,0.12), rgba(179,157,219,0.04))', color: '#b39ddb' },
    { label: '活跃用户', value: s?.activeUsers || 0, icon: Users, iconBg: 'linear-gradient(135deg, rgba(129,199,132,0.12), rgba(129,199,132,0.04))', color: '#81c784' },
    { label: '满意度', value: (s?.successRate || 0).toFixed(1) + '%', icon: HelpCircle, iconBg: 'linear-gradient(135deg, rgba(255,107,157,0.12), rgba(255,107,157,0.04))', color: '#ff6b9d' },
  ]
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

async function loadOverview() {
  try {
    const res = await api.get('/statistics/system/overview')
    overview.value = res.data
  } catch { /* ignore */ }
}

async function loadData() {
  loading.value = true
  error.value = undefined
  await Promise.all([loadOverview(), listBots().then(b => bots.value = b).catch(() => {})])
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
.dash-greeting {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
}

.dash-greeting-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--anime-text-primary);
  letter-spacing: -0.3px;
}

.dash-greeting-sub {
  font-size: 14px;
  color: var(--anime-text-muted);
  margin-top: 4px;
}

/* Stat Grid */
.dash-stat-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 14px;
  margin-bottom: 28px;
}

.dash-stat-card {
  background: #ffffff;
  border-radius: var(--anime-radius-lg);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: var(--anime-shadow-sm);
  border: 1px solid var(--anime-border);
  transition: all 0.3s ease;
}

.dash-stat-card:hover {
  box-shadow: var(--anime-shadow-md);
  transform: translateY(-2px);
}

.dash-stat-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--anime-radius);
  display: grid;
  place-items: center;
  flex-shrink: 0;
  color: var(--anime-text-primary);
}

.dash-stat-body {
  flex: 1;
  min-width: 0;
}

.dash-stat-number {
  font-size: 22px;
  font-weight: 800;
  line-height: 1.2;
}

.dash-stat-label {
  font-size: 12px;
  color: var(--anime-text-muted);
  margin-top: 2px;
  font-weight: 500;
}

/* Section */
.dash-section {
  margin-bottom: 24px;
}

.dash-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.dash-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: var(--anime-text-primary);
}

.dash-section-card {
  background: #ffffff;
  border-radius: var(--anime-radius-lg);
  padding: 22px 24px;
  box-shadow: var(--anime-shadow-sm);
  border: 1px solid var(--anime-border);
}

/* Bot Grid */
.dash-bot-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.dash-bot-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: #ffffff;
  border-radius: var(--anime-radius);
  box-shadow: var(--anime-shadow-sm);
  border: 1px solid var(--anime-border);
  transition: all 0.2s ease;
}

.dash-bot-card:hover {
  box-shadow: var(--anime-shadow-md);
  border-color: var(--anime-border-accent);
}

.dash-bot-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.dash-bot-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--anime-radius);
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 800;
  color: #fff;
  flex-shrink: 0;
}

.dash-bot-icon.qq { background: linear-gradient(135deg, #4fc3f7, #29b6f6); }
.dash-bot-icon.wecom { background: linear-gradient(135deg, #81c784, #66bb6a); }
.dash-bot-icon.wx { background: linear-gradient(135deg, #81c784, #4fc3f7); }

.dash-bot-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--anime-text-primary);
}

.dash-bot-meta {
  font-size: 12px;
  color: var(--anime-text-muted);
  margin-top: 2px;
}

.dash-bot-right {
  display: flex;
  align-items: center;
}

.dash-bot-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
}

.dash-bot-status.online {
  background: rgba(129, 199, 132, 0.12);
  color: #388e3c;
}

.dash-bot-status.offline {
  background: rgba(0, 0, 0, 0.04);
  color: var(--anime-text-muted);
}

.dash-bot-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.dash-bot-dot.online {
  background: #81c784;
  box-shadow: 0 0 6px rgba(129, 199, 132, 0.5);
}

.dash-bot-dot.offline {
  background: var(--anime-text-muted);
}

.dash-bot-empty {
  grid-column: 1 / -1;
  text-align: center;
  padding: 24px;
  color: var(--anime-text-muted);
  font-size: 13px;
}

/* Bottom Grid */
.dash-bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.dash-quick-actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.dash-quick-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 12px;
  background: rgba(255, 107, 157, 0.04);
  border: 1px solid var(--anime-border);
  border-radius: var(--anime-radius);
  color: var(--anime-text-secondary);
  text-decoration: none;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.2s ease;
}

.dash-quick-btn:hover {
  background: rgba(255, 107, 157, 0.08);
  border-color: var(--anime-border-accent);
  color: var(--anime-pink);
  transform: translateY(-1px);
}

.dash-info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.dash-info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  background: rgba(0, 0, 0, 0.02);
  border-radius: var(--anime-radius);
}

.dash-info-label {
  font-size: 11px;
  color: var(--anime-text-muted);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.dash-info-value {
  font-size: 18px;
  font-weight: 800;
  color: var(--anime-text-primary);
}

.dash-info-value.green {
  color: #388e3c;
}

.animate-spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 1024px) {
  .dash-stat-grid { grid-template-columns: repeat(3, 1fr); }
  .dash-bot-grid { grid-template-columns: 1fr; }
  .dash-bottom-grid { grid-template-columns: 1fr; }
}

@media (max-width: 600px) {
  .dash-stat-grid { grid-template-columns: repeat(2, 1fr); }
  .dash-quick-actions { grid-template-columns: repeat(2, 1fr); }
  .dash-info-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
