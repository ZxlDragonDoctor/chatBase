<template>
  <div class="anime-page-shell statistics-page">
    <header class="stats-page-header">
      <div class="stats-page-title">
        <span class="stats-page-kicker">分析与洞察</span>
        <h1>数据统计</h1>
        <p>Token消耗 · 群活跃度 · 关键词热度 · 问答趋势</p>
      </div>
      <div class="stats-toolbar">
        <div class="anime-tabs" role="group" aria-label="统计周期">
          <button class="anime-tab" :class="{ active: period === 7 }" :aria-pressed="period === 7" @click="period = 7; reload()">近7天</button>
          <button class="anime-tab" :class="{ active: period === 30 }" :aria-pressed="period === 30" @click="period = 30; reload()">近30天</button>
        </div>
        <div v-if="isAdmin" class="anime-tabs" role="group" aria-label="数据范围">
          <button class="anime-tab" :class="{ active: showAllData }" :aria-pressed="showAllData" @click="showAllData = true; reload()">全部数据</button>
          <button class="anime-tab" :class="{ active: !showAllData }" :aria-pressed="!showAllData" @click="showAllData = false; reload()">我的数据</button>
        </div>
        <button class="anime-btn ghost" @click="aggregateData">
          <Database :size="18" />
          <span>聚合统计</span>
        </button>
        <button class="anime-btn ghost" :disabled="loading" @click="reload">
          <RefreshCw :size="18" :class="{ 'animate-spin': loading }" />
          <span>刷新</span>
        </button>
      </div>
    </header>

    <div v-if="error" class="anime-error stats-page-error" role="alert">{{ error }}</div>

    <div class="stats-content">
      <section class="stats-panel stats-chart-panel">
        <div class="stats-panel-heading">
          <div class="stats-panel-title">
            <Zap :size="19" />
            <h2>Token 消耗趋势</h2>
          </div>
          <div class="anime-tabs">
            <button class="anime-tab sm" :class="{ active: chartView === 'daily' }" :aria-pressed="chartView === 'daily'" @click="chartView = 'daily'; loadTokenChart()">按日</button>
            <button class="anime-tab sm" :class="{ active: chartView === 'monthly' }" :aria-pressed="chartView === 'monthly'" @click="chartView = 'monthly'; loadTokenMonthly()">本月</button>
          </div>
        </div>

        <div class="stats-metric-grid anime-stat-grid">
          <div class="anime-stat-card">
            <div class="anime-stat-value">{{ formatTokens(tokenChartData?.totalTokens || 0) }}</div>
            <div class="anime-stat-label">总Token消耗</div>
          </div>
          <div class="anime-stat-card">
            <div class="anime-stat-value blue">{{ tokenChartData?.avgTokens?.toFixed(1) || 0 }}</div>
            <div class="anime-stat-label">平均每对话</div>
          </div>
          <div class="anime-stat-card">
            <div class="anime-stat-value purple">{{ tokenChartData?.totalConversations || 0 }}</div>
            <div class="anime-stat-label">对话总数</div>
          </div>
          <div v-if="chartView === 'monthly'" class="anime-stat-card">
            <div class="anime-stat-value green">{{ formatTokens(tokenMonthlyData?.projectedMonthlyTokens || 0) }}</div>
            <div class="anime-stat-label">预计月消耗</div>
          </div>
        </div>

        <div class="chart-container">
          <div ref="tokenChartRef" class="echarts-chart"></div>
        </div>
      </section>

      <section class="stats-panel stats-chart-panel">
        <div class="stats-panel-heading">
          <div class="stats-panel-title accent">
            <Database :size="19" />
            <h2>费用消耗趋势</h2>
          </div>
          <div class="anime-tabs">
            <button class="anime-tab sm" :class="{ active: chartView === 'daily' }" :aria-pressed="chartView === 'daily'" @click="chartView = 'daily'; loadCostChart()">按日</button>
            <button class="anime-tab sm" :class="{ active: chartView === 'monthly' }" :aria-pressed="chartView === 'monthly'" @click="chartView = 'monthly'; loadCostMonthly()">本月</button>
          </div>
        </div>

        <div class="stats-metric-grid anime-stat-grid">
          <div class="anime-stat-card">
            <div class="anime-stat-value accent">&#36;{{ ((chartView === 'daily' ? costChartData?.totalCost : costMonthlyData?.totalCost) || 0).toFixed(4) }}</div>
            <div class="anime-stat-label">总费用</div>
          </div>
          <div class="anime-stat-card">
            <div class="anime-stat-value blue">{{ formatTokens(costChartData?.totalPromptTokens || 0) }}</div>
            <div class="anime-stat-label">Prompt Tokens</div>
          </div>
          <div class="anime-stat-card">
            <div class="anime-stat-value purple">{{ formatTokens(costChartData?.totalCompletionTokens || 0) }}</div>
            <div class="anime-stat-label">Completion Tokens</div>
          </div>
        </div>

        <div class="chart-container">
          <div ref="costChartRef" class="echarts-chart"></div>
        </div>
      </section>

      <section class="stats-panel stats-ranking-panel">
        <div class="stats-panel-heading stats-panel-heading-stacked">
          <div class="stats-panel-title blue">
            <Users :size="19" />
            <h2>群聊活跃度排行</h2>
          </div>
          <div class="anime-tabs" role="group" aria-label="平台筛选">
            <button v-for="p in platforms" :key="p.value" class="anime-tab" :class="{ active: selectedPlatform === p.value }" :aria-pressed="selectedPlatform === p.value" @click="selectedPlatform = p.value; loadGroupActive()">{{ p.label }}</button>
          </div>
        </div>

        <div class="stats-rank-list">
          <div v-if="!groupActiveLoaded" class="anime-empty stats-group-state" role="status" aria-live="polite">
            <span class="anime-loader-spinner"></span>
            <span class="anime-empty-text">加载中...</span>
          </div>
          <template v-else>
            <article v-for="g in groupActive?.topGroups || []" :key="g.groupId" class="stats-rank-item">
              <span class="anime-badge stats-rank-badge" :class="g.rank === 1 ? 'green' : g.rank === 2 ? 'blue' : 'purple'">#{{ g.rank }}</span>
              <div class="stats-rank-content">
                <div class="stats-rank-identity">
                  <span class="anime-badge muted">{{ g.platform }}</span>
                  <span class="anime-code">{{ g.groupId }}</span>
                </div>
                <div class="stats-rank-meta">
                  <span class="stats-rank-count">{{ formatNumber(g.messageCount) }} 条消息</span>
                  <span v-if="g.lastMessageTime">最后活跃: {{ formatTime(g.lastMessageTime) }}</span>
                </div>
              </div>
            </article>
            <div v-if="!groupActive?.topGroups?.length" class="anime-empty">
              <span class="anime-empty-icon">📭</span>
              <span class="anime-empty-text">暂无群聊数据</span>
            </div>
          </template>
          <span class="stats-sr-only" role="status" aria-live="polite" aria-atomic="true">{{ groupActiveLoaded && !groupActive?.topGroups?.length ? '暂无群聊数据' : '' }}</span>
        </div>
      </section>

      <section class="stats-panel stats-keyword-panel">
        <div class="stats-panel-heading">
          <div class="stats-panel-title neutral">
            <Search :size="19" />
            <h2>关键词热度分析</h2>
          </div>
          <button class="anime-btn ghost" @click="syncKeywords" :disabled="keywordSyncing">
            <RefreshCw :size="16" :class="{ 'animate-spin': keywordSyncing }" />
            <span>{{ keywordSyncing ? '同步中...' : '同步关键词' }}</span>
          </button>
          <span class="stats-sr-only" role="status" aria-live="polite" aria-atomic="true">{{ keywordSyncing ? '关键词同步中' : !keywordLoading && !keywordHot?.keywords?.length ? '暂无关键词数据' : '' }}</span>
        </div>
        <div v-if="keywordLoading" class="anime-empty stats-keyword-state" role="status" aria-live="polite">
          <span class="anime-loader-spinner"></span>
          <span class="anime-empty-text">分析中...</span>
        </div>
        <div v-else class="anime-word-cloud">
          <span
            v-for="(kw, idx) in keywordHot?.keywords || []"
            :key="kw.keyword"
            class="word-cloud-item"
            :style="getWordStyle(kw, idx)"
          >
            {{ kw.keyword }}
          </span>
          <div v-if="!keywordHot?.keywords?.length" class="anime-empty">
            <span class="anime-empty-text">暂无关键词数据</span>
          </div>
        </div>
      </section>

      <section class="stats-panel stats-outcome-panel">
        <div class="stats-panel-heading">
          <div class="stats-panel-title success">
            <MessageCircle :size="19" />
            <h2>问答成功率统计</h2>
          </div>
        </div>
        <div class="stats-metric-grid anime-stat-grid">
          <div class="anime-stat-card">
            <div class="anime-stat-value">{{ convStats?.totalConversations || 0 }}</div>
            <div class="anime-stat-label">总问答次数</div>
          </div>
          <div class="anime-stat-card">
            <div class="anime-stat-value blue">{{ convStats?.successRate?.toFixed(1) || 0 }}%</div>
            <div class="anime-stat-label">成功率</div>
          </div>
          <div class="anime-stat-card">
            <div class="anime-stat-value purple">{{ convStats?.avgLatencyMs || 0 }} ms</div>
            <div class="anime-stat-label">平均延迟</div>
          </div>
        </div>
        <div class="stats-progress">
          <div class="stats-progress-track">
            <div class="stats-progress-fill success" :style="{ width: (convStats?.successRate || 0) + '%' }"></div>
          </div>
          <div class="stats-progress-labels">
            <span class="success">成功 {{ convStats?.successfulConversations || 0 }}</span>
            <span class="danger">失败 {{ convStats?.failedConversations || 0 }}</span>
          </div>
        </div>
      </section>

      <section class="stats-panel stats-outcome-panel">
        <div class="stats-panel-heading">
          <div class="stats-panel-title success">
            <ThumbsUp :size="19" />
            <h2>用户满意度统计</h2>
          </div>
        </div>
        <div class="stats-metric-grid anime-stat-grid">
          <div class="anime-stat-card">
            <div class="anime-stat-value green">{{ feedbackStats?.totalThumbsUp || 0 }}</div>
            <div class="anime-stat-label">点赞数</div>
          </div>
          <div class="anime-stat-card">
            <div class="anime-stat-value danger">{{ feedbackStats?.totalThumbsDown || 0 }}</div>
            <div class="anime-stat-label">踩数</div>
          </div>
          <div class="anime-stat-card">
            <div class="anime-stat-value blue">{{ (feedbackStats?.positiveRate || 100).toFixed(1) }}%</div>
            <div class="anime-stat-label">满意度</div>
          </div>
        </div>
        <div class="stats-progress">
          <div class="stats-progress-track">
            <div class="stats-progress-fill success" :style="{ width: (feedbackStats?.positiveRate || 100) + '%' }"></div>
          </div>
          <div class="stats-progress-labels">
            <span class="success">点赞 {{ feedbackStats?.totalThumbsUp || 0 }}</span>
            <span class="danger">踩 {{ feedbackStats?.totalThumbsDown || 0 }}</span>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch, nextTick } from 'vue'
import { Zap, Users, Search, MessageCircle, RefreshCw, ThumbsUp, Database } from 'lucide-vue-next'
import { fetchTokenDaily, fetchGroupActive, fetchHotKeywords, fetchConversationOverview, fetchKeywordCloud, syncKeywordsFromMessages, fetchTokenChartData, fetchTokenMonthlyData, aggregateStatistics, fetchCostChartData, fetchCostMonthlyData } from '../api/statistics'
import { fetchFeedbackDailyStats } from '../api/feedbackStats'
import type { TokenStatistics, GroupActive, KeywordHot, ConversationStatistics, KeywordItem } from '../types/statistics'
import type { TokenChartData, TokenMonthlyData, CostChartData, CostMonthlyData } from '../api/statistics'
import type { FeedbackStats } from '../api/feedbackStats'
import * as echarts from 'echarts'

const uiToken = (name: string, fallback: string) =>
  getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback

const chartTheme = {
  text: uiToken('--ui-text-secondary', '#53615d'),
  border: uiToken('--ui-border', '#dce4e0'),
  grid: uiToken('--anime-border-light', '#e8eeeb'),
  primary: uiToken('--ui-primary', '#087f70'),
  secondary: uiToken('--anime-blue', '#397987'),
  accent: uiToken('--ui-accent', '#c57a25'),
}

const period = ref(7)
const loading = ref(false)
const error = ref<string | null>(null)

const isAdmin = localStorage.getItem('chatbase_role') === 'admin'
const showAllData = ref(true)
const scope = computed(() => showAllData.value ? 'all' : 'mine')

const tokenStats = ref<TokenStatistics | null>(null)
const tokenChartData = ref<TokenChartData | null>(null)
const tokenMonthlyData = ref<TokenMonthlyData | null>(null)
const chartView = ref<'daily' | 'monthly'>('daily')
const tokenChartRef = ref<HTMLElement | null>(null)
let tokenChart: echarts.ECharts | null = null

const costChartData = ref<CostChartData | null>(null)
const costMonthlyData = ref<CostMonthlyData | null>(null)
const costChartRef = ref<HTMLElement | null>(null)
let costChart: echarts.ECharts | null = null

const groupActive = ref<GroupActive | null>(null)
const groupActiveLoaded = ref(false)
let groupActiveLoadCount = 0
const selectedPlatform = ref('all')
const platforms = [{ value: 'all', label: '全部' }, { value: 'qq', label: 'QQ' }, { value: 'wecom', label: '企微' }]
const keywordHot = ref<KeywordHot | null>(null)
const keywordLoading = ref(true)
const keywordSyncing = ref(false)
let keywordLoadCount = 0
const convStats = ref<ConversationStatistics | null>(null)
const feedbackStats = ref<FeedbackStats | null>(null)

const maxDailyToken = computed(() => {
  const tokens = tokenStats.value?.dailyTokens || []
  return Math.max(...tokens.map(d => d.tokens), 1)
})

async function reload() {
  error.value = null
  loading.value = true
  await Promise.all([loadTokenChart(), loadCostChart(), loadGroupActive(), loadKeywords(), loadConvStats(), loadFeedbackStats()])
  loading.value = false
}

async function loadCostChart() {
  try {
    costChartData.value = await fetchCostChartData(period.value, scope.value)
    await nextTick()
    renderCostChart()
  } catch {
    costChartData.value = null
  }
}

async function loadCostMonthly() {
  try {
    costMonthlyData.value = await fetchCostMonthlyData(scope.value)
    await nextTick()
    renderCostChartMonthly()
  } catch {
    costMonthlyData.value = null
  }
}

function renderCostChart() {
  if (!costChartRef.value || !costChartData.value) return

  if (!costChart) {
    costChart = echarts.init(costChartRef.value)
  }

  const data = costChartData.value
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: ['费用($)', 'Prompt Tokens', 'Completion Tokens'],
      textStyle: { color: chartTheme.text }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: data.dates.map(d => d.slice(5)),
      axisLine: { lineStyle: { color: chartTheme.border } },
      axisLabel: { color: chartTheme.text }
    },
    yAxis: [
      {
        type: 'value',
        name: '费用($)',
        position: 'left',
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: chartTheme.grid } },
        axisLabel: { color: chartTheme.text, formatter: (val: number) => '$' + val.toFixed(4) }
      },
      {
        type: 'value',
        name: 'Tokens',
        position: 'right',
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { color: chartTheme.text, formatter: (val: number) => val >= 1000 ? (val / 1000) + 'K' : val }
      }
    ],
    series: [
      {
        name: '费用($)',
        type: 'line',
        smooth: true,
        data: data.costs,
        itemStyle: { color: chartTheme.accent },
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(197, 122, 37, 0.24)' },
            { offset: 1, color: 'rgba(197, 122, 37, 0.03)' }
          ])
        }
      },
      {
        name: 'Prompt Tokens',
        type: 'bar',
        yAxisIndex: 1,
        data: data.promptTokens,
        itemStyle: { color: chartTheme.primary, borderRadius: [4, 4, 0, 0] },
        barWidth: '30%'
      },
      {
        name: 'Completion Tokens',
        type: 'bar',
        yAxisIndex: 1,
        data: data.completionTokens,
        itemStyle: { color: chartTheme.secondary, borderRadius: [4, 4, 0, 0] },
        barWidth: '30%'
      }
    ]
  }

  costChart.setOption(option)
}

function renderCostChartMonthly() {
  if (!costChartRef.value || !costMonthlyData.value) return

  if (!costChart) {
    costChart = echarts.init(costChartRef.value)
  }

  const data = costMonthlyData.value
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: ['费用($)'],
      textStyle: { color: chartTheme.text }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: data.dates.map(d => d.slice(5)),
      axisLine: { lineStyle: { color: chartTheme.border } },
      axisLabel: { color: chartTheme.text }
    },
    yAxis: [
      {
        type: 'value',
        name: '费用($)',
        position: 'left',
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: chartTheme.grid } },
        axisLabel: { color: chartTheme.text, formatter: (val: number) => '$' + val.toFixed(4) }
      }
    ],
    series: [
      {
        name: '费用($)',
        type: 'line',
        smooth: true,
        data: data.costs,
        itemStyle: { color: chartTheme.accent },
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(197, 122, 37, 0.24)' },
            { offset: 1, color: 'rgba(197, 122, 37, 0.03)' }
          ])
        }
      }
    ]
  }

  costChart.setOption(option)
}

async function loadTokenChart() {
  try {
    tokenChartData.value = await fetchTokenChartData(period.value, scope.value)
    await nextTick()
    renderTokenChart()
  } catch {
    tokenChartData.value = null
  }
}

async function loadTokenMonthly() {
  try {
    tokenMonthlyData.value = await fetchTokenMonthlyData(scope.value)
    tokenChartData.value = tokenMonthlyData.value
    await nextTick()
    renderTokenChart()
  } catch {
    tokenMonthlyData.value = null
  }
}

function renderTokenChart() {
  if (!tokenChartRef.value || !tokenChartData.value) return

  if (!tokenChart) {
    tokenChart = echarts.init(tokenChartRef.value)
  }

  const data = tokenChartData.value
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: ['Token消耗', '对话数'],
      textStyle: { color: chartTheme.text }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: data.dates.map(d => d.slice(5)),
      axisLine: { lineStyle: { color: chartTheme.border } },
      axisLabel: { color: chartTheme.text }
    },
    yAxis: [
      {
        type: 'value',
        name: 'Token',
        position: 'left',
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: chartTheme.grid } },
        axisLabel: { color: chartTheme.text, formatter: (val: number) => val >= 1000 ? (val / 1000) + 'K' : val }
      },
      {
        type: 'value',
        name: '对话数',
        position: 'right',
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { color: chartTheme.text }
      }
    ],
    series: [
      {
        name: 'Token消耗',
        type: 'line',
        smooth: true,
        data: data.tokens,
        itemStyle: { color: chartTheme.primary },
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(8, 127, 112, 0.24)' },
            { offset: 1, color: 'rgba(8, 127, 112, 0.03)' }
          ])
        }
      },
      {
        name: '对话数',
        type: 'bar',
        yAxisIndex: 1,
        data: data.conversations,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#5c929d' },
            { offset: 1, color: '#a8c8ca' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        barWidth: '40%'
      }
    ]
  }

  tokenChart.setOption(option)
}

async function aggregateData() {
  loading.value = true
  try {
    await aggregateStatistics(period.value)
    await reload()
  } catch (e) {
    error.value = '聚合统计失败'
  }
  loading.value = false
}

async function loadGroupActive() {
  groupActiveLoadCount += 1
  groupActiveLoaded.value = false
  try {
    groupActive.value = await fetchGroupActive(selectedPlatform.value, 10, scope.value)
  } catch {
    groupActive.value = null
  } finally {
    groupActiveLoadCount -= 1
    if (groupActiveLoadCount === 0) groupActiveLoaded.value = true
  }
}

async function loadKeywords() {
  keywordLoadCount += 1
  keywordLoading.value = true
  try {
    keywordHot.value = await fetchKeywordCloud('all', period.value, 50)
  } catch {
    try {
      keywordHot.value = await fetchHotKeywords('all', undefined, 30, scope.value)
    } catch { keywordHot.value = null }
  } finally {
    keywordLoadCount -= 1
    if (keywordLoadCount === 0) keywordLoading.value = false
  }
}

async function syncKeywords() {
  keywordSyncing.value = true
  try {
    const result = await syncKeywordsFromMessages(period.value)
    if (result.success) {
      await loadKeywords()
    }
  } catch (e) {
    console.error('同步关键词失败', e)
  }
  keywordSyncing.value = false
}

async function loadConvStats() {
  try { convStats.value = await fetchConversationOverview(period.value, scope.value) } catch { convStats.value = null }
}

async function loadFeedbackStats() {
  try { feedbackStats.value = await fetchFeedbackDailyStats(period.value) } catch { feedbackStats.value = null }
}

function formatDate(date: string): string { return date.slice(5) }
function formatTime(time: string): string { return time ? time.slice(0, 16).replace('T', ' ') : '' }
function formatNumber(n: number): string { if (n >= 1000) return `${(n / 1000).toFixed(1)}K`; return n.toString() }
function formatTokens(n: number | undefined): string { if (!n) return '0'; if (n >= 1000) return `${(n / 1000).toFixed(1)}K`; return n.toString() }
function getBarWidth(value: number | undefined, max: number): number { if (!value) return 0; return Math.min(100, (value / max) * 100) }
function getWordStyle(kw: KeywordItem, idx: number): Record<string, string> {
  const colors = [
    'var(--ui-primary)', 'var(--ui-accent)', 'var(--ui-success)',
    'var(--ui-text-secondary)', 'var(--ui-primary-hover)', 'var(--ui-accent)'
  ]
  const maxCount = keywordHot.value?.keywords?.[0]?.count || 1
  const scale = Math.max(12, Math.min(32, 12 + (kw.count / maxCount) * 20))
  const color = colors[idx % colors.length]
  return {
    fontSize: `${scale}px`,
    color: color,
    fontWeight: kw.rank <= 3 ? '700' : kw.rank <= 10 ? '600' : '400',
    padding: '4px 10px',
    margin: '3px',
    display: 'inline-block',
    borderRadius: '6px',
    background: 'var(--ui-primary-soft)',
    cursor: 'default',
    transition: 'all 0.3s ease',
  }
}

watch(selectedPlatform, loadGroupActive)
onMounted(reload)
</script>

<style scoped>
.statistics-page {
  min-width: 0;
}

.stats-page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.stats-page-title { min-width: 220px; }

.stats-page-kicker {
  display: block;
  margin-bottom: 6px;
  color: var(--ui-primary);
  font-size: 10px;
  font-weight: 750;
  letter-spacing: 0.13em;
  text-transform: uppercase;
}

.stats-page-title h1 {
  margin: 0;
  color: var(--ui-text);
  font-size: clamp(24px, 2.3vw, 30px);
  line-height: 1.2;
  font-weight: 750;
  letter-spacing: -0.04em;
}

.stats-page-title p {
  margin: 7px 0 0;
  color: var(--ui-text-secondary);
  font-size: 13px;
}

.stats-toolbar {
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.stats-toolbar .anime-tabs { margin: 0; }

.stats-page-error { margin-bottom: 16px; }

.stats-content {
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
}

.stats-panel {
  min-width: 0;
  padding: 20px;
  background: var(--ui-surface);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-lg);
  box-shadow: var(--ui-shadow-card);
}

.stats-panel-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.stats-panel-heading-stacked {
  align-items: flex-start;
  flex-direction: column;
}

.stats-panel-title {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 9px;
  color: var(--ui-primary);
}

.stats-panel-title h2 {
  margin: 0;
  color: inherit;
  font-size: 16px;
  font-weight: 700;
}

.stats-panel-title.accent { color: var(--ui-accent); }
.stats-panel-title.blue { color: var(--ui-text-secondary); }
.stats-panel-title.neutral { color: var(--ui-text-secondary); }
.stats-panel-title.success { color: var(--ui-success); }

.stats-panel-heading .anime-tabs {
  flex-wrap: wrap;
}

.stats-sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.stats-metric-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.stats-metric-grid .anime-stat-card {
  min-width: 0;
  min-height: 82px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  padding: 14px;
  background: var(--ui-surface-muted);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
}

.statistics-page .anime-stat-value {
  color: var(--ui-primary);
  font-size: 22px;
  line-height: 1.15;
  font-weight: 740;
  font-variant-numeric: tabular-nums;
}

.statistics-page .anime-stat-value.blue { color: var(--ui-text-secondary); }
.statistics-page .anime-stat-value.purple { color: var(--ui-accent); }
.statistics-page .anime-stat-value.green { color: var(--ui-success); }
.statistics-page .anime-stat-value.accent { color: var(--ui-accent); }
.statistics-page .anime-stat-value.danger { color: var(--ui-danger); }

.statistics-page .anime-stat-label {
  color: var(--ui-text-muted);
  font-size: 11px;
  font-weight: 550;
}

.chart-container {
  min-width: 0;
  margin-top: 12px;
  padding: 10px 4px 0;
  background: var(--ui-surface);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  box-shadow: none;
}

.echarts-chart {
  width: 100%;
  height: 300px;
}

.stats-rank-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stats-group-state { min-height: 190px; }

.stats-rank-item {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: var(--ui-surface-muted);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
}

.stats-rank-badge {
  min-width: 34px;
  justify-content: center;
  font-variant-numeric: tabular-nums;
}

.stats-rank-content { min-width: 0; flex: 1; }

.stats-rank-identity,
.stats-rank-meta {
  min-width: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.stats-rank-identity { margin-bottom: 5px; }

.stats-rank-meta {
  color: var(--ui-text-muted);
  font-size: 12px;
}

.stats-rank-count { color: var(--ui-primary); font-weight: 650; }
.stats-keyword-state { min-height: 190px; }

.anime-word-cloud {
  min-height: 190px;
  display: flex;
  flex-wrap: wrap;
  align-content: center;
  align-items: center;
  justify-content: flex-start;
  gap: 4px;
  padding: 18px;
  background: var(--ui-surface-muted);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
}

.word-cloud-item {
  display: inline-block;
  border: 1px solid transparent;
  border-radius: var(--ui-radius-sm);
  transition: background-color 0.16s ease, border-color 0.16s ease, transform 0.16s ease;
}

.word-cloud-item:hover {
  background: var(--ui-primary-soft) !important;
  border-color: var(--ui-border);
  transform: translateY(-1px);
}

.stats-progress { margin-top: 16px; }

.stats-progress-track {
  height: 10px;
  overflow: hidden;
  background: var(--ui-surface-muted);
  border: 1px solid var(--ui-border);
  border-radius: 999px;
}

.stats-progress-fill {
  height: 100%;
  border-radius: inherit;
  transition: width 0.25s ease;
}

.stats-progress-fill.success { background: var(--ui-success); }

.stats-progress-labels {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
  font-size: 12px;
  font-weight: 600;
}

.stats-progress-labels .success { color: var(--ui-success); }
.stats-progress-labels .danger { color: var(--ui-danger); }

.animate-spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (min-width: 1360px) {
  .stats-content { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 900px) {
  .stats-page-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 16px;
  }

  .stats-toolbar { justify-content: flex-start; }
}

@media (max-width: 560px) {
  .stats-panel { padding: 15px; }
  .stats-panel-heading:not(.stats-panel-heading-stacked) { align-items: flex-start; }
  .stats-toolbar { align-items: stretch; }
  .stats-toolbar > * { max-width: 100%; }
  .stats-metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .stats-panel-heading .anime-btn { padding: 8px 10px; }
  .echarts-chart { height: 260px; }
}
</style>
