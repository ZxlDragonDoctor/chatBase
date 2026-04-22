/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, ref, watch, nextTick } from 'vue';
import { Zap, Users, Search, MessageCircle, RefreshCw, ThumbsUp, Database } from 'lucide-vue-next';
import { fetchGroupActive, fetchHotKeywords, fetchConversationOverview, fetchKeywordCloud, syncKeywordsFromMessages, fetchTokenChartData, fetchTokenMonthlyData, aggregateStatistics, fetchCostChartData, fetchCostMonthlyData } from '../api/statistics';
import { fetchFeedbackDailyStats } from '../api/feedbackStats';
import * as echarts from 'echarts';
const period = ref(7);
const loading = ref(false);
const error = ref(null);
const tokenStats = ref(null);
const tokenChartData = ref(null);
const tokenMonthlyData = ref(null);
const chartView = ref('daily');
const tokenChartRef = ref(null);
let tokenChart = null;
const costChartData = ref(null);
const costMonthlyData = ref(null);
const costChartRef = ref(null);
let costChart = null;
const groupActive = ref(null);
const selectedPlatform = ref('all');
const platforms = [{ value: 'all', label: '全部' }, { value: 'qq', label: 'QQ' }, { value: 'wecom', label: '企微' }];
const keywordHot = ref(null);
const keywordLoading = ref(false);
const keywordSyncing = ref(false);
const convStats = ref(null);
const feedbackStats = ref(null);
const maxDailyToken = computed(() => {
    const tokens = tokenStats.value?.dailyTokens || [];
    return Math.max(...tokens.map(d => d.tokens), 1);
});
async function reload() {
    error.value = null;
    loading.value = true;
    await Promise.all([loadTokenChart(), loadCostChart(), loadGroupActive(), loadKeywords(), loadConvStats(), loadFeedbackStats()]);
    loading.value = false;
}
async function loadCostChart() {
    try {
        costChartData.value = await fetchCostChartData(period.value);
        await nextTick();
        renderCostChart();
    }
    catch {
        costChartData.value = null;
    }
}
async function loadCostMonthly() {
    try {
        costMonthlyData.value = await fetchCostMonthlyData();
        await nextTick();
        renderCostChartMonthly();
    }
    catch {
        costMonthlyData.value = null;
    }
}
function renderCostChart() {
    if (!costChartRef.value || !costChartData.value)
        return;
    if (!costChart) {
        costChart = echarts.init(costChartRef.value);
    }
    const data = costChartData.value;
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
                axisLabel: { color: '#666', formatter: (val) => '$' + val.toFixed(4) }
            },
            {
                type: 'value',
                name: 'Tokens',
                position: 'right',
                axisLine: { show: false },
                axisTick: { show: false },
                splitLine: { show: false },
                axisLabel: { color: '#666', formatter: (val) => val >= 1000 ? (val / 1000) + 'K' : val }
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
    };
    costChart.setOption(option);
}
function renderCostChartMonthly() {
    if (!costChartRef.value || !costMonthlyData.value)
        return;
    if (!costChart) {
        costChart = echarts.init(costChartRef.value);
    }
    const data = costMonthlyData.value;
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
                axisLabel: { color: '#666', formatter: (val) => '$' + val.toFixed(4) }
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
    };
    costChart.setOption(option);
}
async function loadTokenChart() {
    try {
        tokenChartData.value = await fetchTokenChartData(period.value);
        await nextTick();
        renderTokenChart();
    }
    catch {
        tokenChartData.value = null;
    }
}
async function loadTokenMonthly() {
    try {
        tokenMonthlyData.value = await fetchTokenMonthlyData();
        tokenChartData.value = tokenMonthlyData.value;
        await nextTick();
        renderTokenChart();
    }
    catch {
        tokenMonthlyData.value = null;
    }
}
function renderTokenChart() {
    if (!tokenChartRef.value || !tokenChartData.value)
        return;
    if (!tokenChart) {
        tokenChart = echarts.init(tokenChartRef.value);
    }
    const data = tokenChartData.value;
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
                axisLabel: { color: '#666', formatter: (val) => val >= 1000 ? (val / 1000) + 'K' : val }
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
    };
    tokenChart.setOption(option);
}
async function aggregateData() {
    loading.value = true;
    try {
        await aggregateStatistics(period.value);
        await reload();
    }
    catch (e) {
        error.value = '聚合统计失败';
    }
    loading.value = false;
}
async function loadGroupActive() {
    try {
        groupActive.value = await fetchGroupActive(selectedPlatform.value, 10);
    }
    catch {
        groupActive.value = null;
    }
}
async function loadKeywords() {
    keywordLoading.value = true;
    try {
        keywordHot.value = await fetchKeywordCloud('all', period.value, 50);
    }
    catch {
        try {
            keywordHot.value = await fetchHotKeywords('all', undefined, 30);
        }
        catch {
            keywordHot.value = null;
        }
    }
    keywordLoading.value = false;
}
async function syncKeywords() {
    keywordSyncing.value = true;
    try {
        const result = await syncKeywordsFromMessages(period.value);
        if (result.success) {
            await loadKeywords();
        }
    }
    catch (e) {
        console.error('同步关键词失败', e);
    }
    keywordSyncing.value = false;
}
async function loadConvStats() {
    try {
        convStats.value = await fetchConversationOverview(period.value);
    }
    catch {
        convStats.value = null;
    }
}
async function loadFeedbackStats() {
    try {
        feedbackStats.value = await fetchFeedbackDailyStats(period.value);
    }
    catch {
        feedbackStats.value = null;
    }
}
function formatDate(date) { return date.slice(5); }
function formatTime(time) { return time ? time.slice(0, 16).replace('T', ' ') : ''; }
function formatNumber(n) { if (n >= 1000)
    return `${(n / 1000).toFixed(1)}K`; return n.toString(); }
function formatTokens(n) { if (!n)
    return '0'; if (n >= 1000)
    return `${(n / 1000).toFixed(1)}K`; return n.toString(); }
function getBarWidth(value, max) { if (!value)
    return 0; return Math.min(100, (value / max) * 100); }
function getWordStyle(kw, idx) {
    const colors = [
        'var(--anime-pink)', 'var(--anime-blue)', 'var(--anime-purple)',
        'var(--anime-green)', '#ff6b6b', '#4ecdc4', '#45b7d1', '#96ceb4',
        '#ffeaa7', '#dfe6e9', '#fd79a8', '#74b9ff', '#a29bfe', '#00b894'
    ];
    const maxCount = keywordHot.value?.keywords?.[0]?.count || 1;
    const scale = Math.max(12, Math.min(32, 12 + (kw.count / maxCount) * 20));
    const color = colors[idx % colors.length];
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
    };
}
watch(selectedPlatform, loadGroupActive);
onMounted(reload);
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['word-cloud-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-value']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-page-shell" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "anime-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-card-header" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-card-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-card-desc" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-card-actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-tabs" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.period = 7;
            __VLS_ctx.reload();
        } },
    ...{ class: "anime-tab" },
    ...{ class: ({ active: __VLS_ctx.period === 7 }) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.period = 30;
            __VLS_ctx.reload();
        } },
    ...{ class: "anime-tab" },
    ...{ class: ({ active: __VLS_ctx.period === 30 }) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.aggregateData) },
    ...{ class: "anime-btn ghost" },
});
const __VLS_0 = {}.Database;
/** @type {[typeof __VLS_components.Database, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    size: (18),
}));
const __VLS_2 = __VLS_1({
    size: (18),
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.reload) },
    ...{ class: "anime-btn ghost" },
});
const __VLS_4 = {}.RefreshCw;
/** @type {[typeof __VLS_components.RefreshCw, ]} */ ;
// @ts-ignore
const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
    size: (18),
    ...{ class: ({ 'animate-spin': __VLS_ctx.loading }) },
}));
const __VLS_6 = __VLS_5({
    size: (18),
    ...{ class: ({ 'animate-spin': __VLS_ctx.loading }) },
}, ...__VLS_functionalComponentArgsRest(__VLS_5));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
if (__VLS_ctx.error) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-error" },
        ...{ style: {} },
    });
    (__VLS_ctx.error);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-card-body" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
const __VLS_8 = {}.Zap;
/** @type {[typeof __VLS_components.Zap, ]} */ ;
// @ts-ignore
const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
    size: (20),
    ...{ style: {} },
}));
const __VLS_10 = __VLS_9({
    size: (20),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_9));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-tabs" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.chartView = 'daily';
            __VLS_ctx.loadTokenChart();
        } },
    ...{ class: "anime-tab sm" },
    ...{ class: ({ active: __VLS_ctx.chartView === 'daily' }) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.chartView = 'monthly';
            __VLS_ctx.loadTokenMonthly();
        } },
    ...{ class: "anime-tab sm" },
    ...{ class: ({ active: __VLS_ctx.chartView === 'monthly' }) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-value" },
});
(__VLS_ctx.formatTokens(__VLS_ctx.tokenChartData?.totalTokens || 0));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-value blue" },
});
(__VLS_ctx.tokenChartData?.avgTokens?.toFixed(1) || 0);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-value purple" },
});
(__VLS_ctx.tokenChartData?.totalConversations || 0);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-label" },
});
if (__VLS_ctx.chartView === 'monthly') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-stat-card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-stat-value green" },
    });
    (__VLS_ctx.formatTokens(__VLS_ctx.tokenMonthlyData?.projectedMonthlyTokens || 0));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-stat-label" },
    });
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "chart-container" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ref: "tokenChartRef",
    ...{ class: "echarts-chart" },
});
/** @type {typeof __VLS_ctx.tokenChartRef} */ ;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-divider" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
const __VLS_12 = {}.Zap;
/** @type {[typeof __VLS_components.Zap, ]} */ ;
// @ts-ignore
const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
    size: (20),
    ...{ style: {} },
}));
const __VLS_14 = __VLS_13({
    size: (20),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_13));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-tabs" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.chartView = 'daily';
            __VLS_ctx.loadCostChart();
        } },
    ...{ class: "anime-tab sm" },
    ...{ class: ({ active: __VLS_ctx.chartView === 'daily' }) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.chartView = 'monthly';
            __VLS_ctx.loadCostMonthly();
        } },
    ...{ class: "anime-tab sm" },
    ...{ class: ({ active: __VLS_ctx.chartView === 'monthly' }) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-value" },
    ...{ style: {} },
});
(((__VLS_ctx.chartView === 'daily' ? __VLS_ctx.costChartData?.totalCost : __VLS_ctx.costMonthlyData?.totalCost) || 0).toFixed(4));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-value blue" },
});
(__VLS_ctx.formatTokens(__VLS_ctx.costChartData?.totalPromptTokens || 0));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-value purple" },
});
(__VLS_ctx.formatTokens(__VLS_ctx.costChartData?.totalCompletionTokens || 0));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "chart-container" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ref: "costChartRef",
    ...{ class: "echarts-chart" },
});
/** @type {typeof __VLS_ctx.costChartRef} */ ;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-divider" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
const __VLS_16 = {}.Users;
/** @type {[typeof __VLS_components.Users, ]} */ ;
// @ts-ignore
const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
    size: (20),
    ...{ style: {} },
}));
const __VLS_18 = __VLS_17({
    size: (20),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_17));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-tabs" },
    ...{ style: {} },
});
for (const [p] of __VLS_getVForSourceType((__VLS_ctx.platforms))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.selectedPlatform = p.value;
                __VLS_ctx.loadGroupActive();
            } },
        key: (p.value),
        ...{ class: "anime-tab" },
        ...{ class: ({ active: __VLS_ctx.selectedPlatform === p.value }) },
    });
    (p.label);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
for (const [g] of __VLS_getVForSourceType((__VLS_ctx.groupActive?.topGroups || []))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        key: (g.groupId),
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "anime-badge" },
        ...{ class: (g.rank === 1 ? 'green' : g.rank === 2 ? 'blue' : 'purple') },
    });
    (g.rank);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "anime-badge muted" },
    });
    (g.platform);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "anime-code" },
    });
    (g.groupId);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ style: {} },
    });
    (__VLS_ctx.formatNumber(g.messageCount));
    if (g.lastMessageTime) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.formatTime(g.lastMessageTime));
    }
}
if (!__VLS_ctx.groupActive?.topGroups?.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-empty" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "anime-empty-icon" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "anime-empty-text" },
    });
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-divider" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
const __VLS_20 = {}.Search;
/** @type {[typeof __VLS_components.Search, ]} */ ;
// @ts-ignore
const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
    size: (20),
    ...{ style: {} },
}));
const __VLS_22 = __VLS_21({
    size: (20),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_21));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.syncKeywords) },
    ...{ class: "anime-btn ghost" },
    disabled: (__VLS_ctx.keywordSyncing),
});
const __VLS_24 = {}.RefreshCw;
/** @type {[typeof __VLS_components.RefreshCw, ]} */ ;
// @ts-ignore
const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
    size: (16),
    ...{ class: ({ 'animate-spin': __VLS_ctx.keywordSyncing }) },
}));
const __VLS_26 = __VLS_25({
    size: (16),
    ...{ class: ({ 'animate-spin': __VLS_ctx.keywordSyncing }) },
}, ...__VLS_functionalComponentArgsRest(__VLS_25));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.keywordSyncing ? '同步中...' : '同步关键词');
if (__VLS_ctx.keywordLoading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-empty" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "anime-loader-spinner" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "anime-empty-text" },
    });
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-word-cloud" },
    });
    for (const [kw, idx] of __VLS_getVForSourceType((__VLS_ctx.keywordHot?.keywords || []))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            key: (kw.keyword),
            ...{ class: "word-cloud-item" },
            ...{ style: (__VLS_ctx.getWordStyle(kw, idx)) },
        });
        (kw.keyword);
    }
    if (!__VLS_ctx.keywordHot?.keywords?.length) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "anime-empty" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-empty-text" },
        });
    }
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-divider" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
const __VLS_28 = {}.MessageCircle;
/** @type {[typeof __VLS_components.MessageCircle, ]} */ ;
// @ts-ignore
const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
    size: (20),
    ...{ style: {} },
}));
const __VLS_30 = __VLS_29({
    size: (20),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_29));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-value" },
});
(__VLS_ctx.convStats?.totalConversations || 0);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-value blue" },
});
(__VLS_ctx.convStats?.successRate?.toFixed(1) || 0);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-value purple" },
});
(__VLS_ctx.convStats?.avgLatencyMs || 0);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-progress" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
    ...{ style: ({ width: (__VLS_ctx.convStats?.successRate || 0) + '%' }) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ style: {} },
});
(__VLS_ctx.convStats?.successfulConversations || 0);
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ style: {} },
});
(__VLS_ctx.convStats?.failedConversations || 0);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-divider" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
const __VLS_32 = {}.ThumbsUp;
/** @type {[typeof __VLS_components.ThumbsUp, ]} */ ;
// @ts-ignore
const __VLS_33 = __VLS_asFunctionalComponent(__VLS_32, new __VLS_32({
    size: (20),
    ...{ style: {} },
}));
const __VLS_34 = __VLS_33({
    size: (20),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_33));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-value green" },
});
(__VLS_ctx.feedbackStats?.totalThumbsUp || 0);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-value pink" },
});
(__VLS_ctx.feedbackStats?.totalThumbsDown || 0);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-value blue" },
});
((__VLS_ctx.feedbackStats?.positiveRate || 100).toFixed(1));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-progress" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
    ...{ style: ({ width: (__VLS_ctx.feedbackStats?.positiveRate || 100) + '%' }) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ style: {} },
});
(__VLS_ctx.feedbackStats?.totalThumbsUp || 0);
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ style: {} },
});
(__VLS_ctx.feedbackStats?.totalThumbsDown || 0);
/** @type {__VLS_StyleScopedClasses['anime-page-shell']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-header']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-title']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-desc']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tabs']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tab']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tab']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-error']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-body']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tabs']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tab']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tab']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['blue']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['purple']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['green']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['chart-container']} */ ;
/** @type {__VLS_StyleScopedClasses['echarts-chart']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-divider']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tabs']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tab']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tab']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['blue']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['purple']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['chart-container']} */ ;
/** @type {__VLS_StyleScopedClasses['echarts-chart']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-divider']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tabs']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tab']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-code']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-divider']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-loader-spinner']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-word-cloud']} */ ;
/** @type {__VLS_StyleScopedClasses['word-cloud-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-divider']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['blue']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['purple']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-progress']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-divider']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['green']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['pink']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['blue']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-progress']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            Zap: Zap,
            Users: Users,
            Search: Search,
            MessageCircle: MessageCircle,
            RefreshCw: RefreshCw,
            ThumbsUp: ThumbsUp,
            Database: Database,
            period: period,
            loading: loading,
            error: error,
            tokenChartData: tokenChartData,
            tokenMonthlyData: tokenMonthlyData,
            chartView: chartView,
            tokenChartRef: tokenChartRef,
            costChartData: costChartData,
            costMonthlyData: costMonthlyData,
            costChartRef: costChartRef,
            groupActive: groupActive,
            selectedPlatform: selectedPlatform,
            platforms: platforms,
            keywordHot: keywordHot,
            keywordLoading: keywordLoading,
            keywordSyncing: keywordSyncing,
            convStats: convStats,
            feedbackStats: feedbackStats,
            reload: reload,
            loadCostChart: loadCostChart,
            loadCostMonthly: loadCostMonthly,
            loadTokenChart: loadTokenChart,
            loadTokenMonthly: loadTokenMonthly,
            aggregateData: aggregateData,
            loadGroupActive: loadGroupActive,
            syncKeywords: syncKeywords,
            formatTime: formatTime,
            formatNumber: formatNumber,
            formatTokens: formatTokens,
            getWordStyle: getWordStyle,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
