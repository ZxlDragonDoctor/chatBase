/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { RouterLink } from 'vue-router';
import { RefreshCw, MessageCircle, BookOpen, BarChart3, Users, HelpCircle, MessageSquare, Zap, Cpu } from 'lucide-vue-next';
import { api } from '../api/client';
import { listBots } from '../api/bot';
const loading = ref(false);
const error = ref(undefined);
const displayName = computed(() => localStorage.getItem('chatbase_user') || '用户');
const overview = ref(null);
const bots = ref([]);
const botStatusList = computed(() => {
    const list = [];
    for (const b of bots.value) {
        list.push({
            platform: b.platform,
            name: b.name,
            botId: b.botId ?? undefined,
            online: b.online,
        });
    }
    if (overview.value?.bots?.qqEnabled) {
        if (!list.find(x => x.platform === 'qq')) {
            list.unshift({ platform: 'qq', name: 'QQ机器人', botId: overview.value.bots.qqSelfId, online: true });
        }
    }
    if (overview.value?.bots?.wxEnabled) {
        if (!list.find(x => x.platform === 'wx')) {
            list.unshift({ platform: 'wx', name: overview.value.bots.wxNickname || '微信机器人', online: true });
        }
    }
    if (overview.value?.bots?.wecomEnabled) {
        if (!list.find(x => x.platform === 'wecom')) {
            list.unshift({ platform: 'wecom', name: '企微机器人', online: true });
        }
    }
    return list;
});
const statCards = computed(() => {
    const s = overview.value;
    return [
        { label: '总对话', value: s?.totalConversations || 0, icon: MessageCircle, iconBg: 'linear-gradient(135deg, rgba(255,107,157,0.12), rgba(255,107,157,0.04))', color: '#ff6b9d' },
        { label: '总Token', value: formatNumber(s?.totalTokens || 0), icon: Zap, iconBg: 'linear-gradient(135deg, rgba(255,183,77,0.12), rgba(255,183,77,0.04))', color: '#ffb74d' },
        { label: '活跃群聊', value: s?.activeGroups || 0, icon: Users, iconBg: 'linear-gradient(135deg, rgba(79,195,247,0.12), rgba(79,195,247,0.04))', color: '#4fc3f7' },
        { label: '总知识库', value: s?.knowledgeBases || 0, icon: BookOpen, iconBg: 'linear-gradient(135deg, rgba(179,157,219,0.12), rgba(179,157,219,0.04))', color: '#b39ddb' },
        { label: '活跃用户', value: s?.activeUsers || 0, icon: Users, iconBg: 'linear-gradient(135deg, rgba(129,199,132,0.12), rgba(129,199,132,0.04))', color: '#81c784' },
        { label: '满意度', value: (s?.successRate || 0).toFixed(1) + '%', icon: HelpCircle, iconBg: 'linear-gradient(135deg, rgba(255,107,157,0.12), rgba(255,107,157,0.04))', color: '#ff6b9d' },
    ];
});
const quickActions = [
    { label: 'AI 问答', to: '/chat', icon: MessageSquare },
    { label: '知识库', to: '/console/knowledge', icon: BookOpen },
    { label: '数据统计', to: '/console/statistics', icon: BarChart3 },
    { label: '群聊管理', to: '/console/im', icon: Users },
    { label: 'FAQ', to: '/console/faq', icon: HelpCircle },
    { label: '提交反馈', to: '/feedback', icon: MessageCircle },
];
function formatNumber(n) {
    if (n >= 1000000)
        return (n / 1000000).toFixed(1) + 'M';
    if (n >= 1000)
        return (n / 1000).toFixed(1) + 'K';
    return n.toString();
}
async function loadOverview() {
    try {
        const res = await api.get('/statistics/system/overview');
        overview.value = res.data;
    }
    catch { /* ignore */ }
}
async function loadData() {
    loading.value = true;
    error.value = undefined;
    await Promise.all([loadOverview(), listBots().then(b => bots.value = b).catch(() => { })]);
    loading.value = false;
}
let interval;
onMounted(() => {
    loadData();
    interval = window.setInterval(loadOverview, 30000);
});
onUnmounted(() => {
    if (interval)
        clearInterval(interval);
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['dash-stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bot-card']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bot-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bot-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bot-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bot-status']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bot-status']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bot-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['online']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bot-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['offline']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-quick-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-info-value']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-stat-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bot-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bottom-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-stat-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-quick-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-info-grid']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-page-shell" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "dash-greeting" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "dash-greeting-title" },
});
(__VLS_ctx.displayName);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "dash-greeting-sub" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.loadData) },
    ...{ class: "anime-btn ghost" },
});
const __VLS_0 = {}.RefreshCw;
/** @type {[typeof __VLS_components.RefreshCw, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    size: (16),
    ...{ class: ({ 'animate-spin': __VLS_ctx.loading }) },
}));
const __VLS_2 = __VLS_1({
    size: (16),
    ...{ class: ({ 'animate-spin': __VLS_ctx.loading }) },
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
if (__VLS_ctx.error) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-error" },
        ...{ style: {} },
    });
    (__VLS_ctx.error);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "dash-stat-grid" },
});
for (const [s] of __VLS_getVForSourceType((__VLS_ctx.statCards))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        key: (s.label),
        ...{ class: "dash-stat-card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "dash-stat-icon" },
        ...{ style: ({ background: s.iconBg }) },
    });
    const __VLS_4 = ((s.icon));
    // @ts-ignore
    const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
        size: (20),
    }));
    const __VLS_6 = __VLS_5({
        size: (20),
    }, ...__VLS_functionalComponentArgsRest(__VLS_5));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "dash-stat-body" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "dash-stat-number" },
        ...{ style: ({ color: s.color }) },
    });
    (s.value);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "dash-stat-label" },
    });
    (s.label);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "dash-section" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "dash-section-header" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "dash-section-title" },
});
const __VLS_8 = {}.Cpu;
/** @type {[typeof __VLS_components.Cpu, ]} */ ;
// @ts-ignore
const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
    size: (18),
}));
const __VLS_10 = __VLS_9({
    size: (18),
}, ...__VLS_functionalComponentArgsRest(__VLS_9));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
const __VLS_12 = {}.RouterLink;
/** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
// @ts-ignore
const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
    to: "/console/bots",
    ...{ class: "anime-btn ghost sm" },
}));
const __VLS_14 = __VLS_13({
    to: "/console/bots",
    ...{ class: "anime-btn ghost sm" },
}, ...__VLS_functionalComponentArgsRest(__VLS_13));
__VLS_15.slots.default;
var __VLS_15;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "dash-bot-grid" },
});
for (const [bot] of __VLS_getVForSourceType((__VLS_ctx.botStatusList))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        key: (bot.platform),
        ...{ class: "dash-bot-card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "dash-bot-left" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "dash-bot-icon" },
        ...{ class: (bot.platform) },
    });
    if (bot.platform === 'qq') {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    }
    else if (bot.platform === 'wecom') {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    }
    else if (bot.platform === 'wx') {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    }
    else {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "dash-bot-info" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "dash-bot-name" },
    });
    (bot.name);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "dash-bot-meta" },
    });
    (bot.botId || bot.description || '-');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "dash-bot-right" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "dash-bot-status" },
        ...{ class: (bot.online ? 'online' : 'offline') },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "dash-bot-dot" },
        ...{ class: (bot.online ? 'online' : 'offline') },
    });
    (bot.online ? '在线' : '离线');
}
if (__VLS_ctx.botStatusList.length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "dash-bot-empty" },
    });
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "dash-bottom-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "dash-section dash-section-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "dash-section-header" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "dash-section-title" },
});
const __VLS_16 = {}.Zap;
/** @type {[typeof __VLS_components.Zap, ]} */ ;
// @ts-ignore
const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
    size: (18),
}));
const __VLS_18 = __VLS_17({
    size: (18),
}, ...__VLS_functionalComponentArgsRest(__VLS_17));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "dash-quick-actions" },
});
for (const [a] of __VLS_getVForSourceType((__VLS_ctx.quickActions))) {
    const __VLS_20 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
        key: (a.to),
        to: (a.to),
        ...{ class: "dash-quick-btn" },
    }));
    const __VLS_22 = __VLS_21({
        key: (a.to),
        to: (a.to),
        ...{ class: "dash-quick-btn" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_21));
    __VLS_23.slots.default;
    const __VLS_24 = ((a.icon));
    // @ts-ignore
    const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
        size: (20),
    }));
    const __VLS_26 = __VLS_25({
        size: (20),
    }, ...__VLS_functionalComponentArgsRest(__VLS_25));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (a.label);
    var __VLS_23;
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "dash-section dash-section-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "dash-section-header" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "dash-section-title" },
});
const __VLS_28 = {}.BarChart3;
/** @type {[typeof __VLS_components.BarChart3, ]} */ ;
// @ts-ignore
const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
    size: (18),
}));
const __VLS_30 = __VLS_29({
    size: (18),
}, ...__VLS_functionalComponentArgsRest(__VLS_29));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
const __VLS_32 = {}.RouterLink;
/** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
// @ts-ignore
const __VLS_33 = __VLS_asFunctionalComponent(__VLS_32, new __VLS_32({
    to: "/console/statistics",
    ...{ class: "anime-btn ghost sm" },
}));
const __VLS_34 = __VLS_33({
    to: "/console/statistics",
    ...{ class: "anime-btn ghost sm" },
}, ...__VLS_functionalComponentArgsRest(__VLS_33));
__VLS_35.slots.default;
var __VLS_35;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "dash-info-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "dash-info-item" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "dash-info-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "dash-info-value" },
});
(__VLS_ctx.overview?.totalConversations || 0);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "dash-info-item" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "dash-info-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "dash-info-value" },
});
(__VLS_ctx.formatNumber(__VLS_ctx.overview?.totalTokens || 0));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "dash-info-item" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "dash-info-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "dash-info-value" },
});
(__VLS_ctx.overview?.activeGroups || 0);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "dash-info-item" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "dash-info-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "dash-info-value" },
});
(__VLS_ctx.overview?.knowledgeBases || 0);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "dash-info-item" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "dash-info-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "dash-info-value green" },
});
((__VLS_ctx.overview?.successRate || 100).toFixed(1));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "dash-info-item" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "dash-info-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "dash-info-value" },
    ...{ style: {} },
});
/** @type {__VLS_StyleScopedClasses['anime-page-shell']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-greeting']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-greeting-title']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-greeting-sub']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-error']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-stat-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-stat-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-stat-body']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-stat-number']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-section']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-section-header']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bot-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bot-card']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bot-left']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bot-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bot-info']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bot-name']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bot-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bot-right']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bot-status']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bot-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bot-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-bottom-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-section']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-section-card']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-section-header']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-quick-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-quick-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-section']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-section-card']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-section-header']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-info-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-info-item']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-info-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-info-value']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-info-item']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-info-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-info-value']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-info-item']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-info-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-info-value']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-info-item']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-info-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-info-value']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-info-item']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-info-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-info-value']} */ ;
/** @type {__VLS_StyleScopedClasses['green']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-info-item']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-info-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dash-info-value']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            RouterLink: RouterLink,
            RefreshCw: RefreshCw,
            BarChart3: BarChart3,
            Zap: Zap,
            Cpu: Cpu,
            loading: loading,
            error: error,
            displayName: displayName,
            overview: overview,
            botStatusList: botStatusList,
            statCards: statCards,
            quickActions: quickActions,
            formatNumber: formatNumber,
            loadData: loadData,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
