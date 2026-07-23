<template>
  <div class="anime-page-shell">
    <section class="anime-card">
      <div class="anime-card-header">
        <div>
          <div class="anime-card-title">数据统计</div>
          <div class="anime-card-desc">Token消耗 · 群活跃度 · 关键词热度 · 问答趋势</div>
        </div>
        <div class="anime-card-actions">
          <div class="anime-tabs">
            <button class="anime-tab" :class="{ active: period === 7 }" @click="period = 7; reload()">近7天</button>
            <button class="anime-tab" :class="{ active: period === 30 }" @click="period = 30; reload()">近30天</button>
          </div>
          <div v-if="isAdmin" class="anime-tabs" style="margin-left: 8px;">
            <button class="anime-tab" :class="{ active: showAllData }" @click="showAllData = true; reload()">全部数据</button>
            <button class="anime-tab" :class="{ active: !showAllData }" @click="showAllData = false; reload()">我的数据</button>
          </div>
          <button class="anime-btn ghost" @click="aggregateData">
            <Database :size="18" />
            <span>聚合统计</span>
          </button>
          <button class="anime-btn ghost" @click="reload">
            <RefreshCw :size="18" :class="{ 'animate-spin': loading }" />
            <span>刷新</span>
          </button>
        </div>
      </div>

      <div v-if="error" class="anime-error" style="margin: 16px 28px;">{{ error }}</div>

      <div class="anime-card-body">
        <div style="margin-bottom: 24px;">
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;">
            <div style="display: flex; align-items: center; gap: 10px;">
              <Zap :size="20" style="color: var(--anime-pink);" />
              <span style="font-size: 16px; font-weight: 700; color: var(--anime-pink);">Token 消耗趋势</span>
            </div>
            <div class="anime-tabs">
              <button class="anime-tab sm" :class="{ active: chartView === 'daily' }" @click="chartView = 'daily'; loadTokenChart()">按日</button>
              <button class="anime-tab sm" :class="{ active: chartView === 'monthly' }" @click="chartView = 'monthly'; loadTokenMonthly()">本月</button>
            </div>
          </div>
          
          <div class="anime-stat-grid">
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
        </div>

        <div class="anime-divider"></div>

        <div style="margin-bottom: 24px;">
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;">
            <div style="display: flex; align-items: center; gap: 10px;">
              <Zap :size="20" style="color: #f39c12;" />
              <span style="font-size: 16px; font-weight: 700; color: #f39c12;">费用消耗趋势</span>
            </div>
            <div class="anime-tabs">
              <button class="anime-tab sm" :class="{ active: chartView === 'daily' }" @click="chartView = 'daily'; loadCostChart()">按日</button>
              <button class="anime-tab sm" :class="{ active: chartView === 'monthly' }" @click="chartView = 'monthly'; loadCostMonthly()">本月</button>
            </div>
          </div>
          
          <div class="anime-stat-grid">
            <div class="anime-stat-card">
              <div class="anime-stat-value" style="color: #f39c12;">${{ ((chartView === 'daily' ? costChartData?.totalCost : costMonthlyData?.totalCost) || 0).toFixed(4) }}</div>
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
        </div>

        <div class="anime-divider"></div>

        <div style="margin-bottom: 24px;">
          <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 16px;">
            <Users :size="20" style="color: var(--anime-blue);" />
            <span style="font-size: 16px; font-weight: 700; color: var(--anime-blue);">群聊活跃度排行</span>
          </div>
          <div class="anime-tabs" style="margin-bottom: 16px;">
            <button v-for="p in platforms" :key="p.value" class="anime-tab" :class="{ active: selectedPlatform === p.value }" @click="selectedPlatform = p.value; loadGroupActive()">{{ p.label }}</button>
          </div>
          <div style="display: flex; flex-direction: column; gap: 14px;">
            <div v-for="g in groupActive?.topGroups || []" :key="g.groupId" style="display: flex; align-items: center; gap: 14px; padding: 14px 20px; background: var(--anime-bg-card); box-shadow: var(--anime-shadow-sm); border-radius: var(--anime-radius-lg); transition: all 0.3s ease;">
              <span class="anime-badge" :class="g.rank === 1 ? 'green' : g.rank === 2 ? 'blue' : 'purple'">#{{ g.rank }}</span>
              <div style="flex: 1;">
                <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 6px;">
                  <span class="anime-badge muted">{{ g.platform }}</span>
                  <span class="anime-code">{{ g.groupId }}</span>
                </div>
                <div style="display: flex; gap: 16px; font-size: 13px; color: var(--anime-text-muted);">
                  <span style="color: var(--anime-blue);">{{ formatNumber(g.messageCount) }} 条消息</span>
                  <span v-if="g.lastMessageTime">最后活跃: {{ formatTime(g.lastMessageTime) }}</span>
                </div>
              </div>
            </div>
            <div v-if="!groupActive?.topGroups?.length" class="anime-empty">
              <span class="anime-empty-icon">📭</span>
              <span class="anime-empty-text">暂无群聊数据</span>
            </div>
          </div>
        </div>

        <div class="anime-divider"></div>

        <div style="margin-bottom: 24px;">
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;">
            <div style="display: flex; align-items: center; gap: 10px;">
              <Search :size="20" style="color: var(--anime-purple);" />
              <span style="font-size: 16px; font-weight: 700; color: var(--anime-purple);">关键词热度分析</span>
            </div>
            <button class="anime-btn ghost" @click="syncKeywords" :disabled="keywordSyncing">
              <RefreshCw :size="16" :class="{ 'animate-spin': keywordSyncing }" />
              <span>{{ keywordSyncing ? '同步中...' : '同步关键词' }}</span>
            </button>
          </div>
          <div v-if="keywordLoading" class="anime-empty">
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
        </div>

        <div class="anime-divider"></div>

        <div>
          <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 16px;">
            <MessageCircle :size="20" style="color: var(--anime-green);" />
            <span style="font-size: 16px; font-weight: 700; color: var(--anime-green);">问答成功率统计</span>
          </div>
          <div class="anime-stat-grid">
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
          <div style="margin-top: 20px;">
            <div class="anime-progress" style="height: 28px; border-radius: 14px;">
              <div style="height: 100%; background: var(--anime-gradient-green); border-radius: 14px;" :style="{ width: (convStats?.successRate || 0) + '%' }"></div>
            </div>
            <div style="display: flex; justify-content: space-between; margin-top: 10px; font-size: 13px;">
              <span style="color: var(--anime-green);">成功 {{ convStats?.successfulConversations || 0 }}</span>
              <span style="color: var(--anime-pink);">失败 {{ convStats?.failedConversations || 0 }}</span>
            </div>
          </div>
        </div>

        <div class="anime-divider"></div>

        <div style="margin-bottom: 24px;">
          <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 16px;">
            <ThumbsUp :size="20" style="color: var(--anime-green);" />
            <span style="font-size: 16px; font-weight: 700; color: var(--anime-green);">用户满意度统计</span>
          </div>
          <div class="anime-stat-grid">
            <div class="anime-stat-card">
              <div class="anime-stat-value green">{{ feedbackStats?.totalThumbsUp || 0 }}</div>
              <div class="anime-stat-label">点赞数</div>
            </div>
            <div class="anime-stat-card">
              <div class="anime-stat-value pink">{{ feedbackStats?.totalThumbsDown || 0 }}</div>
              <div class="anime-stat-label">踩数</div>
            </div>
            <div class="anime-stat-card">
              <div class="anime-stat-value blue">{{ (feedbackStats?.positiveRate || 100).toFixed(1) }}%</div>
              <div class="anime-stat-label">满意度</div>
            </div>
          </div>
          <div style="margin-top: 20px;">
            <div class="anime-progress" style="height: 28px; border-radius: 14px;">
              <div style="height: 100%; background: var(--anime-gradient-green); border-radius: 14px;" :style="{ width: (feedbackStats?.positiveRate || 100) + '%' }"></div>
            </div>
            <div style="display: flex; justify-content: space-between; margin-top: 10px; font-size: 13px;">
              <span style="color: var(--anime-green);">点赞 {{ feedbackStats?.totalThumbsUp || 0 }}</span>
              <span style="color: var(--anime-pink);">踩 {{ feedbackStats?.totalThumbsDown || 0 }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>
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
const selectedPlatform = ref('all')
const platforms = [{ value: 'all', label: '全部' }, { value: 'qq', label: 'QQ' }, { value: 'wecom', label: '企微' }]
const keywordHot = ref<KeywordHot | null>(null)
const keywordLoading = ref(false)
const keywordSyncing = ref(false)
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
      textStyle: { color: '#666' }
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
      axisLine: { lineStyle: { color: '#ddd' } },
      axisLabel: { color: '#666' }
    },
    yAxis: [
      {
        type: 'value',
        name: '费用($)',
        position: 'left',
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: '#eee' } },
        axisLabel: { color: '#666', formatter: (val: number) => '$' + val.toFixed(4) }
      },
      {
        type: 'value',
        name: 'Tokens',
        position: 'right',
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { color: '#666', formatter: (val: number) => val >= 1000 ? (val / 1000) + 'K' : val }
      }
    ],
    series: [
      {
        name: '费用($)',
        type: 'line',
        smooth: true,
        data: data.costs,
        itemStyle: { color: '#f39c12' },
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(243, 156, 18, 0.3)' },
            { offset: 1, color: 'rgba(243, 156, 18, 0.05)' }
          ])
        }
      },
      {
        name: 'Prompt Tokens',
        type: 'bar',
        yAxisIndex: 1,
        data: data.promptTokens,
        itemStyle: { color: '#3498db', borderRadius: [4, 4, 0, 0] },
        barWidth: '30%'
      },
      {
        name: 'Completion Tokens',
        type: 'bar',
        yAxisIndex: 1,
        data: data.completionTokens,
        itemStyle: { color: '#9b59b6', borderRadius: [4, 4, 0, 0] },
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
      textStyle: { color: '#666' }
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
      axisLine: { lineStyle: { color: '#ddd' } },
      axisLabel: { color: '#666' }
    },
    yAxis: [
      {
        type: 'value',
        name: '费用($)',
        position: 'left',
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: '#eee' } },
        axisLabel: { color: '#666', formatter: (val: number) => '$' + val.toFixed(4) }
      }
    ],
    series: [
      {
        name: '费用($)',
        type: 'line',
        smooth: true,
        data: data.costs,
        itemStyle: { color: '#f39c12' },
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(243, 156, 18, 0.3)' },
            { offset: 1, color: 'rgba(243, 156, 18, 0.05)' }
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
      textStyle: { color: '#666' }
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
      axisLine: { lineStyle: { color: '#ddd' } },
      axisLabel: { color: '#666' }
    },
    yAxis: [
      {
        type: 'value',
        name: 'Token',
        position: 'left',
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: '#eee' } },
        axisLabel: { color: '#666', formatter: (val: number) => val >= 1000 ? (val / 1000) + 'K' : val }
      },
      {
        type: 'value',
        name: '对话数',
        position: 'right',
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { color: '#666' }
      }
    ],
    series: [
      {
        name: 'Token消耗',
        type: 'line',
        smooth: true,
        data: data.tokens,
        itemStyle: { color: '#ff6b9d' },
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255, 107, 157, 0.3)' },
            { offset: 1, color: 'rgba(255, 107, 157, 0.05)' }
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
            { offset: 0, color: '#74b9ff' },
            { offset: 1, color: '#a29bfe' }
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
  try { groupActive.value = await fetchGroupActive(selectedPlatform.value, 10, scope.value) } catch { groupActive.value = null }
}

async function loadKeywords() {
  keywordLoading.value = true
  try {
    keywordHot.value = await fetchKeywordCloud('all', period.value, 50)
  } catch {
    try {
      keywordHot.value = await fetchHotKeywords('all', undefined, 30, scope.value)
    } catch { keywordHot.value = null }
  }
  keywordLoading.value = false
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
    'var(--anime-pink)', 'var(--anime-blue)', 'var(--anime-purple)',
    'var(--anime-green)', '#ff6b6b', '#4ecdc4', '#45b7d1', '#96ceb4',
    '#ffeaa7', '#dfe6e9', '#fd79a8', '#74b9ff', '#a29bfe', '#00b894'
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
    background: `rgba(255, 255, 255, 0.1)`,
    cursor: 'default',
    transition: 'all 0.3s ease',
  }
}

watch(selectedPlatform, loadGroupActive)
onMounted(reload)
</script>

<style scoped>
@keyframes spin { to { transform: rotate(360deg); } }
.animate-spin { animation: spin 1s linear infinite; }
.anime-word-cloud {
  padding: 24px;
  background: linear-gradient(135deg, var(--anime-pink-bg) 0%, var(--anime-blue-bg) 100%);
  border-radius: var(--anime-radius-xl);
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 4px;
  min-height: 200px;
}
.word-cloud-item {
  display: inline-block;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}
.word-cloud-item:hover {
  transform: scale(1.15);
  background: rgba(255, 255, 255, 0.25);
  box-shadow: 0 4px 12px rgba(255, 183, 197, 0.3);
}
.anime-stat-value.green { color: var(--anime-green); }
.anime-stat-value.pink { color: var(--anime-pink); }
.anime-tabs .anime-tab.sm {
  padding: 6px 14px;
  font-size: 13px;
}
.chart-container {
  margin-top: 20px;
  padding: 20px;
  background: var(--anime-bg-card);
  border-radius: var(--anime-radius-xl);
  box-shadow: var(--anime-shadow-sm);
}
.echarts-chart {
  width: 100%;
  height: 300px;
}
</style>