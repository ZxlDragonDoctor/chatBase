<template>
  <div class="anime-page-shell">
    <section class="anime-card">
      <div class="anime-card-header">
        <div>
          <div class="anime-card-title">✿ 机器人管理 ✿</div>
          <div class="anime-card-desc">QQ 机器人 · 企业微信机器人 · 运行状态监控</div>
        </div>
        <div class="anime-card-actions">
          <button class="anime-btn ghost" @click="loadBots">
            <RefreshCw :size="18" :class="{ 'animate-spin': loading }" />
            <span>刷新</span>
          </button>
        </div>
      </div>

      <div v-if="error" class="anime-error" style="margin: 16px 28px;">{{ error }}</div>

      <div class="anime-card-body">
        <div v-if="loading" class="anime-loading-state">
          <span class="anime-loader-spinner" style="width: 32px; height: 32px;"></span>
          <div>加载中...</div>
        </div>

        <template v-else>
          <div class="anime-stat-grid">
            <div class="anime-stat-card">
              <div class="anime-stat-value">{{ bots.length }}</div>
              <div class="anime-stat-label">机器人总数</div>
            </div>
            <div class="anime-stat-card">
              <div class="anime-stat-value green">{{ onlineCount }}</div>
              <div class="anime-stat-label">在线数量</div>
            </div>
            <div class="anime-stat-card">
              <div class="anime-stat-value pink">{{ todayMessages }}</div>
              <div class="anime-stat-label">今日消息</div>
            </div>
            <div class="anime-stat-card">
              <div class="anime-stat-value purple">{{ totalMessages }}</div>
              <div class="anime-stat-label">总消息数</div>
            </div>
          </div>

          <div class="anime-divider"></div>

          <div class="bot-list">
            <div v-for="bot in bots" :key="bot.platform" class="bot-card">
              <div class="bot-header">
                <div class="bot-identity">
                  <div class="bot-icon" :class="bot.platform">
                    <span v-if="bot.platform === 'qq'">🐧</span>
                    <span v-else>🏢</span>
                  </div>
                  <div class="bot-info">
                    <div class="bot-name-row">
                      <span class="bot-name">{{ bot.name }}</span>
                      <span class="bot-status-dot" :class="bot.online ? 'online' : 'offline'"></span>
                      <span class="bot-status-text" :class="bot.online ? 'online' : 'offline'">
                        {{ bot.online ? '在线' : '离线' }}
                      </span>
                    </div>
                    <div class="bot-detail" v-if="bot.botId">
                      QQ: {{ bot.botId }}
                    </div>
                    <div class="bot-detail" v-else>
                      企业微信回调模式
                    </div>
                  </div>
                </div>
              </div>

              <div class="bot-stats">
                <div class="bot-stat-item">
                  <span class="bot-stat-label">群聊数</span>
                  <span class="bot-stat-value">{{ bot.groupCount }}</span>
                </div>
                <div class="bot-stat-item">
                  <span class="bot-stat-label">今日消息</span>
                  <span class="bot-stat-value">{{ bot.todayMessages }}</span>
                </div>
                <div class="bot-stat-item">
                  <span class="bot-stat-label">总消息</span>
                  <span class="bot-stat-value">{{ formatNumber(bot.totalMessages) }}</span>
                </div>
                <div class="bot-stat-item">
                  <span class="bot-stat-label">最后活跃</span>
                  <span class="bot-stat-value">{{ bot.lastActiveTime ? formatDate(bot.lastActiveTime) : '无' }}</span>
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
import { RefreshCw } from 'lucide-vue-next'
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
  return d.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadBots()
})
</script>

<style scoped>
.anime-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.anime-stat-card {
  text-align: center;
  padding: 20px 16px;
  background: var(--anime-bg);
  border-radius: var(--anime-radius-lg);
  border: 2px solid var(--anime-border);
}

.anime-stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--anime-pink);
  margin-bottom: 4px;
}

.anime-stat-value.green { color: #22c55e; }
.anime-stat-value.pink { color: var(--anime-pink); }
.anime-stat-value.purple { color: var(--anime-purple); }

.anime-stat-label {
  font-size: 13px;
  color: var(--anime-text-muted);
}

.anime-divider {
  height: 2px;
  background: var(--anime-border);
  margin: 24px 0;
}

.bot-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.bot-card {
  background: var(--anime-bg);
  border-radius: var(--anime-radius-lg);
  border: 2px solid var(--anime-border);
  overflow: hidden;
  transition: all 0.2s ease;
}

.bot-card:hover {
  border-color: var(--anime-pink);
  box-shadow: var(--anime-shadow-card);
}

.bot-header {
  padding: 16px 20px;
  border-bottom: 2px solid var(--anime-border);
  background: var(--anime-bg-card);
}

.bot-identity {
  display: flex;
  align-items: center;
  gap: 16px;
}

.bot-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 24px;
  flex-shrink: 0;
}

.bot-icon.qq {
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
}

.bot-icon.wecom {
  background: linear-gradient(135deg, #34d399, #10b981);
}

.bot-info {
  flex: 1;
}

.bot-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.bot-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--anime-text-primary);
}

.bot-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.bot-status-dot.online {
  background: #22c55e;
  box-shadow: 0 0 6px #22c55e;
}

.bot-status-dot.offline {
  background: #9ca3af;
}

.bot-status-text {
  font-size: 12px;
  font-weight: 600;
}

.bot-status-text.online { color: #22c55e; }
.bot-status-text.offline { color: #9ca3af; }

.bot-detail {
  font-size: 13px;
  color: var(--anime-text-muted);
}

.bot-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1px;
  background: var(--anime-border);
}

.bot-stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  background: var(--anime-bg);
  gap: 6px;
}

.bot-stat-label {
  font-size: 12px;
  color: var(--anime-text-muted);
}

.bot-stat-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--anime-text-primary);
}

.anime-loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 48px 24px;
  color: var(--anime-text-muted);
  font-size: 14px;
}

@media (max-width: 768px) {
  .anime-stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .bot-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
