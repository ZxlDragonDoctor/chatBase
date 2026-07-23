<template>
  <div class="anime-page-shell">
    <section class="anime-card">
      <div class="anime-card-header">
        <div>
          <div class="anime-card-title">机器人管理</div>
          <div class="anime-card-desc">QQ 机器人 · 企业微信机器人 · 微信个人号 · 运行状态监控</div>
        </div>
        <div class="anime-card-actions">
          <button class="anime-btn ghost" @click="loadBots">
            <RefreshCw :size="16" :class="{ 'animate-spin': loading }" />
            <span>刷新</span>
          </button>
        </div>
      </div>

      <div v-if="error" class="anime-error" style="margin: 0 28px 16px;">{{ error }}</div>

      <div class="anime-card-body">
        <div v-if="loading" class="bot-loading">
          <span class="anime-loader-spinner" style="width: 28px; height: 28px;"></span>
          <div style="color: var(--anime-text-muted); font-size: 14px;">加载中...</div>
        </div>

        <template v-else>
          <div class="bot-stat-grid">
            <div class="bot-stat-card">
              <div class="bot-stat-icon" style="background: linear-gradient(135deg, rgba(79,195,247,0.12), rgba(79,195,247,0.04)); color: #4fc3f7;">
                <Cpu :size="20" />
              </div>
              <div class="bot-stat-body">
                <div class="bot-stat-value">{{ bots.length }}</div>
                <div class="bot-stat-label">机器人总数</div>
              </div>
            </div>
            <div class="bot-stat-card">
              <div class="bot-stat-icon" style="background: linear-gradient(135deg, rgba(129,199,132,0.12), rgba(129,199,132,0.04)); color: #81c784;">
                <Activity :size="20" />
              </div>
              <div class="bot-stat-body">
                <div class="bot-stat-value green">{{ onlineCount }}</div>
                <div class="bot-stat-label">在线数量</div>
              </div>
            </div>
            <div class="bot-stat-card">
              <div class="bot-stat-icon" style="background: linear-gradient(135deg, rgba(255,107,157,0.12), rgba(255,107,157,0.04)); color: #ff6b9d;">
                <MessageCircle :size="20" />
              </div>
              <div class="bot-stat-body">
                <div class="bot-stat-value pink">{{ todayMessages }}</div>
                <div class="bot-stat-label">今日消息</div>
              </div>
            </div>
            <div class="bot-stat-card">
              <div class="bot-stat-icon" style="background: linear-gradient(135deg, rgba(179,157,219,0.12), rgba(179,157,219,0.04)); color: #b39ddb;">
                <Database :size="20" />
              </div>
              <div class="bot-stat-body">
                <div class="bot-stat-value purple">{{ formatNumber(totalMessages) }}</div>
                <div class="bot-stat-label">总消息数</div>
              </div>
            </div>
          </div>

          <div class="anime-divider"></div>

          <div class="bot-list">
            <div v-for="bot in bots" :key="bot.platform" class="bot-card">
              <div class="bot-card-header">
                <div class="bot-card-identity">
                  <div class="bot-card-icon" :class="bot.platform">
                    <span v-if="bot.platform === 'qq'">Q</span>
                    <span v-else-if="bot.platform === 'wecom'">W</span>
                    <span v-else-if="bot.platform === 'wx'">WX</span>
                    <span v-else>B</span>
                  </div>
                  <div class="bot-card-info">
                    <div class="bot-card-name-row">
                      <span class="bot-card-name">{{ bot.name }}</span>
                      <span class="bot-status-indicator" :class="bot.online ? 'online' : 'offline'">
                        <span class="bot-status-dot" :class="bot.online ? 'online' : 'offline'"></span>
                        {{ bot.online ? '在线' : '离线' }}
                      </span>
                    </div>
                    <div class="bot-card-detail">
                      <span v-if="bot.platform === 'qq' && bot.botId">QQ: {{ bot.botId }}</span>
                      <span v-else-if="bot.platform === 'wecom'">企业微信 · 回调模式</span>
                      <span v-else-if="bot.platform === 'wx'">微信个人号 · ilink 协议</span>
                      <span v-else>-</span>
                    </div>
                  </div>
                </div>
              </div>
              <div class="bot-card-stats">
                <div class="bot-s-item">
                  <span class="bot-s-label">群聊数</span>
                  <span class="bot-s-value">{{ bot.groupCount }}</span>
                </div>
                <div class="bot-s-item">
                  <span class="bot-s-label">今日消息</span>
                  <span class="bot-s-value">{{ bot.todayMessages }}</span>
                </div>
                <div class="bot-s-item">
                  <span class="bot-s-label">总消息</span>
                  <span class="bot-s-value">{{ formatNumber(bot.totalMessages) }}</span>
                </div>
                <div class="bot-s-item">
                  <span class="bot-s-label">最后活跃</span>
                  <span class="bot-s-value">{{ bot.lastActiveTime ? formatDate(bot.lastActiveTime) : '无' }}</span>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { RefreshCw, Cpu, Activity, MessageCircle, Database } from 'lucide-vue-next'
import { listBots, type BotInfo } from '../api/bot'

const bots = ref<BotInfo[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

const onlineCount = computed(() => bots.value.filter(b => b.online).length)
const todayMessages = computed(() => bots.value.reduce((sum, b) => sum + b.todayMessages, 0))
const totalMessages = computed(() => bots.value.reduce((sum, b) => sum + b.totalMessages, 0))

async function loadBots() {
  loading.value = true
  error.value = null
  try {
    bots.value = await listBots()
  } catch (e: any) {
    error.value = e?.response?.data?.message || '加载机器人列表失败'
  } finally {
    loading.value = false
  }
}

function formatNumber(n: number): string {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return n.toString()
}

function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(() => { loadBots() })
</script>

<style scoped>
.bot-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 48px;
}

/* Stat Grid */
.bot-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 8px;
}

.bot-stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #ffffff;
  border-radius: var(--anime-radius-lg);
  box-shadow: var(--anime-shadow-sm);
  border: 1px solid var(--anime-border);
}

.bot-stat-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--anime-radius);
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.bot-stat-body {
  flex: 1;
}

.bot-stat-value {
  font-size: 22px;
  font-weight: 800;
  color: var(--anime-text-primary);
  line-height: 1.2;
}

.bot-stat-value.green { color: #388e3c; }
.bot-stat-value.pink { color: #ff6b9d; }
.bot-stat-value.purple { color: #b39ddb; }

.bot-stat-label {
  font-size: 12px;
  color: var(--anime-text-muted);
  margin-top: 2px;
}

/* Bot Cards */
.bot-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bot-card {
  background: #ffffff;
  border-radius: var(--anime-radius-lg);
  box-shadow: var(--anime-shadow-sm);
  border: 1px solid var(--anime-border);
  overflow: hidden;
  transition: all 0.2s ease;
}

.bot-card:hover {
  box-shadow: var(--anime-shadow-md);
  border-color: var(--anime-border-accent);
}

.bot-card-header {
  padding: 18px 22px;
  border-bottom: 1px solid var(--anime-border);
}

.bot-card-identity {
  display: flex;
  align-items: center;
  gap: 16px;
}

.bot-card-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--anime-radius);
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 800;
  color: #fff;
  flex-shrink: 0;
}

.bot-card-icon.qq { background: linear-gradient(135deg, #4fc3f7, #29b6f6); }
.bot-card-icon.wecom { background: linear-gradient(135deg, #81c784, #66bb6a); }
.bot-card-icon.wx { background: linear-gradient(135deg, #81c784, #4fc3f7); }

.bot-card-info {
  flex: 1;
}

.bot-card-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.bot-card-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--anime-text-primary);
}

.bot-status-indicator {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 700;
}

.bot-status-indicator.online { color: #388e3c; }
.bot-status-indicator.offline { color: var(--anime-text-muted); }

.bot-status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.bot-status-dot.online {
  background: #81c784;
  box-shadow: 0 0 6px rgba(129, 199, 132, 0.5);
}

.bot-status-dot.offline { background: var(--anime-text-muted); }

.bot-card-detail {
  font-size: 13px;
  color: var(--anime-text-muted);
}

.bot-card-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
}

.bot-s-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 12px;
  gap: 4px;
  border-right: 1px solid var(--anime-border-light);
}

.bot-s-item:last-child { border-right: none; }

.bot-s-label {
  font-size: 11px;
  color: var(--anime-text-muted);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.bot-s-value {
  font-size: 16px;
  font-weight: 800;
  color: var(--anime-text-primary);
}

.animate-spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 768px) {
  .bot-stat-grid { grid-template-columns: repeat(2, 1fr); }
  .bot-card-stats { grid-template-columns: repeat(2, 1fr); }
}
</style>
