/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref, computed, onMounted } from 'vue';
import { RefreshCw } from 'lucide-vue-next';
import { listBots } from '../api/bot';
const bots = ref([]);
const loading = ref(false);
const error = ref(null);
const onlineCount = computed(() => bots.value.filter(b => b.online).length);
const todayMessages = computed(() => bots.value.reduce((sum, b) => sum + b.todayMessages, 0));
const totalMessages = computed(() => bots.value.reduce((sum, b) => sum + b.totalMessages, 0));
async function loadBots() {
    loading.value = true;
    error.value = null;
    try {
        bots.value = await listBots();
    }
    catch (e) {
        error.value = e?.response?.data?.message || '加载机器人列表失败';
    }
    finally {
        loading.value = false;
    }
}
function formatNumber(n) {
    if (n >= 10000)
        return (n / 10000).toFixed(1) + 'w';
    if (n >= 1000)
        return (n / 1000).toFixed(1) + 'k';
    return n.toString();
}
function formatDate(dateStr) {
    const d = new Date(dateStr);
    return d.toLocaleString('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}
onMounted(() => {
    loadBots();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['anime-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-card']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-status-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-status-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-status-text']} */ ;
/** @type {__VLS_StyleScopedClasses['online']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-status-text']} */ ;
/** @type {__VLS_StyleScopedClasses['offline']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stats']} */ ;
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
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.loadBots) },
    ...{ class: "anime-btn ghost" },
});
const __VLS_0 = {}.RefreshCw;
/** @type {[typeof __VLS_components.RefreshCw, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    size: (18),
    ...{ class: ({ 'animate-spin': __VLS_ctx.loading }) },
}));
const __VLS_2 = __VLS_1({
    size: (18),
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
    ...{ class: "anime-card-body" },
});
if (__VLS_ctx.loading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-loading-state" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "anime-loader-spinner" },
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-stat-grid" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-stat-card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-stat-value" },
    });
    (__VLS_ctx.bots.length);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-stat-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-stat-card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-stat-value green" },
    });
    (__VLS_ctx.onlineCount);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-stat-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-stat-card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-stat-value pink" },
    });
    (__VLS_ctx.todayMessages);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-stat-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-stat-card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-stat-value purple" },
    });
    (__VLS_ctx.totalMessages);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-stat-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-divider" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-list" },
    });
    for (const [bot] of __VLS_getVForSourceType((__VLS_ctx.bots))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            key: (bot.platform),
            ...{ class: "bot-card" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "bot-header" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "bot-identity" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "bot-icon" },
            ...{ class: (bot.platform) },
        });
        if (bot.platform === 'qq') {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        }
        else {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "bot-info" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "bot-name-row" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-name" },
        });
        (bot.name);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-status-dot" },
            ...{ class: (bot.online ? 'online' : 'offline') },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-status-text" },
            ...{ class: (bot.online ? 'online' : 'offline') },
        });
        (bot.online ? '在线' : '离线');
        if (bot.botId) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "bot-detail" },
            });
            (bot.botId);
        }
        else {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "bot-detail" },
            });
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "bot-stats" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "bot-stat-item" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-stat-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-stat-value" },
        });
        (bot.groupCount);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "bot-stat-item" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-stat-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-stat-value" },
        });
        (bot.todayMessages);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "bot-stat-item" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-stat-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-stat-value" },
        });
        (__VLS_ctx.formatNumber(bot.totalMessages));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "bot-stat-item" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-stat-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-stat-value" },
        });
        (bot.lastActiveTime ? __VLS_ctx.formatDate(bot.lastActiveTime) : '无');
    }
}
/** @type {__VLS_StyleScopedClasses['anime-page-shell']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-header']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-title']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-desc']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-error']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-body']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-loading-state']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-loader-spinner']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-label']} */ ;
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
/** @type {__VLS_StyleScopedClasses['purple']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-divider']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-list']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-card']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-header']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-identity']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-info']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-name-row']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-name']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-status-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-status-text']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-detail']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-detail']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stats']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-item']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-item']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-item']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-item']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-value']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            RefreshCw: RefreshCw,
            bots: bots,
            loading: loading,
            error: error,
            onlineCount: onlineCount,
            todayMessages: todayMessages,
            totalMessages: totalMessages,
            loadBots: loadBots,
            formatNumber: formatNumber,
            formatDate: formatDate,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
