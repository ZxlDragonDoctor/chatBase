<template>
  <div class="anime-page-shell">
    <section class="anime-card">
      <div class="anime-card-header">
        <div>
          <div class="anime-card-title">系统概览</div>
          <div class="anime-card-desc">实时系统状态监控 · 机器人配置 · 消息采集统计</div>
        </div>
        <div class="anime-card-actions">
          <button class="anime-btn ghost" @click="reload">
            <RefreshCw :size="18" :class="{ 'animate-spin': loading }" />
            <span>刷新</span>
          </button>
        </div>
      </div>

      <div v-if="error" class="anime-error" style="margin: 16px 28px;">{{ error }}</div>

      <div class="anime-card-body">
        <div class="anime-stat-grid">
          <div class="anime-stat-card">
            <div class="anime-stat-value">{{ formatNumber(overview?.totalMessages) }}</div>
            <div class="anime-stat-label">已采集消息</div>
          </div>
          <div class="anime-stat-card">
            <div class="anime-stat-value blue">{{ formatNumber(overview?.totalConversations) }}</div>
            <div class="anime-stat-label">问答对话次数</div>
          </div>
          <div class="anime-stat-card">
            <div class="anime-stat-value purple">{{ formatTokens(overview?.totalTokens) }}</div>
            <div class="anime-stat-label">Token消耗总量</div>
          </div>
          <div class="anime-stat-card">
            <div class="anime-stat-value">{{ overview?.activeGroups || 0 }}</div>
            <div class="anime-stat-label">活跃群聊数</div>
          </div>
          <div class="anime-stat-card">
            <div class="anime-stat-value blue">{{ overview?.activeUsers || 0 }}</div>
            <div class="anime-stat-label">活跃用户数</div>
          </div>
          <div class="anime-stat-card">
            <div class="anime-stat-value">{{ overview?.successRate?.toFixed(1) || 0 }}%</div>
            <div class="anime-stat-label">问答成功率</div>
          </div>
        </div>

        <div class="anime-divider"></div>

        <div class="dashboard-grid">
          <div class="anime-card" style="padding: 20px;">
            <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 16px;">
              <BookOpen :size="20" class="anime-nav-icon" style="color: var(--anime-pink);" />
              <span style="font-size: 16px; font-weight: 700; color: var(--anime-pink);">知识库状态</span>
            </div>
            <div style="display: flex; gap: 30px; margin-bottom: 12px;">
              <div>
                <div style="font-size: 28px; font-weight: 700; color: var(--anime-pink);">{{ overview?.knowledgeBases || 0 }}</div>
                <div style="font-size: 13px; color: var(--anime-text-muted);">知识库</div>
              </div>
              <div>
                <div style="font-size: 28px; font-weight: 700; color: var(--anime-blue);">{{ overview?.documents || 0 }}</div>
                <div style="font-size: 13px; color: var(--anime-text-muted);">文档数</div>
              </div>
            </div>
            <RouterLink class="anime-btn ghost" to="/console/knowledge">管理知识库</RouterLink>
          </div>

          <div class="anime-card" style="padding: 20px;">
            <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 16px;">
              <Users :size="20" class="anime-nav-icon" style="color: var(--anime-blue);" />
              <span style="font-size: 16px; font-weight: 700; color: var(--anime-blue);">机器人状态</span>
            </div>
            <div style="display: flex; flex-direction: column; gap: 12px;">
              <div style="display: flex; align-items: center; gap: 12px;">
                <span class="anime-badge pink">QQ</span>
                <span style="color: var(--anime-text-primary);">NapCat / OneBot</span>
                <span class="anime-badge" :class="overview?.bots?.qqEnabled ? 'green' : 'muted'">
                  {{ overview?.bots?.qqEnabled ? '运行中' : '未启用' }}
                </span>
              </div>
              <div style="display: flex; align-items: center; gap: 12px;">
                <span class="anime-badge blue">企微</span>
                <span style="color: var(--anime-text-primary);">智能机器人</span>
                <span class="anime-badge" :class="overview?.bots?.wecomEnabled ? 'green' : 'muted'">
                  {{ overview?.bots?.wecomEnabled ? '运行中' : '未启用' }}
                </span>
              </div>
              <div style="display: flex; align-items: center; gap: 12px;">
                <span class="anime-badge purple">微信</span>
                <span style="color: var(--anime-text-primary);">{{ overview?.bots?.wxNickname || '个人号' }}</span>
                <span class="anime-badge" :class="overview?.bots?.wxEnabled ? 'green' : 'muted'">
                  {{ overview?.bots?.wxEnabled ? '运行中' : '未启用' }}
                </span>
              </div>
            </div>
          </div>

          <div class="anime-card" style="padding: 20px;">
            <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 16px;">
              <Zap :size="20" class="anime-nav-icon" style="color: var(--anime-purple);" />
              <span style="font-size: 16px; font-weight: 700; color: var(--anime-purple);">性能指标</span>
            </div>
            <div style="display: flex; flex-direction: column; gap: 12px;">
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span style="color: var(--anime-text-muted); font-size: 14px;">平均响应延迟</span>
                <span style="color: var(--anime-blue); font-weight: 700;">{{ overview?.avgLatencyMs || 0 }} ms</span>
              </div>
              <div class="anime-progress">
                <div class="anime-progress-bar" :style="{ width: Math.min(100, (overview?.avgLatencyMs || 0) / 50) + '%' }"></div>
              </div>
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span style="color: var(--anime-text-muted); font-size: 14px;">系统状态</span>
                <span class="anime-badge green">✧ 正常 ✧</span>
              </div>
            </div>
          </div>
        </div>

        <div class="anime-divider"></div>

        <div style="display: flex; gap: 14px; flex-wrap: wrap;">
          <RouterLink class="anime-btn primary" to="/console/im">
            <Users :size="18" />
            <span>查看群聊采集</span>
          </RouterLink>
          <RouterLink class="anime-btn blue" to="/chat">
            <MessageCircle :size="18" />
            <span>AI问答</span>
          </RouterLink>
          <RouterLink class="anime-btn ghost" to="/console/statistics">
            <BarChart3 :size="18" />
            <span>详细统计</span>
          </RouterLink>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { RouterLink } from 'vue-router'
import { Home, BarChart3, Users, BookOpen, MessageCircle, RefreshCw, Zap } from 'lucide-vue-next'
import { fetchSystemOverview } from '../api/statistics'
import type { SystemOverview } from '../types/statistics'

const overview = ref<SystemOverview | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

async function reload() {
  loading.value = true
  error.value = null
  try {
    overview.value = await fetchSystemOverview()
  } catch (e: any) {
    error.value = e?.message || '无法加载系统概览'
  } finally {
    loading.value = false
  }
}

function formatNumber(n: number | undefined): string {
  if (!n) return '0'
  if (n >= 1000000) return `${(n / 1000000).toFixed(1)}M`
  if (n >= 1000) return `${(n / 1000).toFixed(1)}K`
  return n.toString()
}

function formatTokens(n: number | undefined): string {
  if (!n) return '0'
  if (n >= 1000000) return `${(n / 1000000).toFixed(1)}M`
  if (n >= 1000) return `${(n / 1000).toFixed(1)}K`
  return n.toString()
}

onMounted(reload)
</script>

<style scoped>
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.animate-spin {
  animation: spin 1s linear infinite;
}
</style>