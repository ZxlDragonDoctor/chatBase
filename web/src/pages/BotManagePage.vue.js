/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref, computed, onMounted } from 'vue';
import { RefreshCw, Cpu, Activity, MessageCircle, Database } from 'lucide-vue-next';
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
    return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
}
onMounted(() => { loadBots(); });
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['bot-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-card']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-card-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-card-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-card-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-status-indicator']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-status-indicator']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-status-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['online']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-status-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['offline']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-s-item']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-card-stats']} */ ;
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
    ...{ class: "anime-card-body" },
});
if (__VLS_ctx.loading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-loading" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "anime-loader-spinner" },
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-stat-grid" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-stat-card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-stat-icon" },
        ...{ style: {} },
    });
    const __VLS_4 = {}.Cpu;
    /** @type {[typeof __VLS_components.Cpu, ]} */ ;
    // @ts-ignore
    const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
        size: (20),
    }));
    const __VLS_6 = __VLS_5({
        size: (20),
    }, ...__VLS_functionalComponentArgsRest(__VLS_5));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-stat-body" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-stat-value" },
    });
    (__VLS_ctx.bots.length);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-stat-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-stat-card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-stat-icon" },
        ...{ style: {} },
    });
    const __VLS_8 = {}.Activity;
    /** @type {[typeof __VLS_components.Activity, ]} */ ;
    // @ts-ignore
    const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
        size: (20),
    }));
    const __VLS_10 = __VLS_9({
        size: (20),
    }, ...__VLS_functionalComponentArgsRest(__VLS_9));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-stat-body" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-stat-value green" },
    });
    (__VLS_ctx.onlineCount);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-stat-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-stat-card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-stat-icon" },
        ...{ style: {} },
    });
    const __VLS_12 = {}.MessageCircle;
    /** @type {[typeof __VLS_components.MessageCircle, ]} */ ;
    // @ts-ignore
    const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
        size: (20),
    }));
    const __VLS_14 = __VLS_13({
        size: (20),
    }, ...__VLS_functionalComponentArgsRest(__VLS_13));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-stat-body" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-stat-value pink" },
    });
    (__VLS_ctx.todayMessages);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-stat-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-stat-card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-stat-icon" },
        ...{ style: {} },
    });
    const __VLS_16 = {}.Database;
    /** @type {[typeof __VLS_components.Database, ]} */ ;
    // @ts-ignore
    const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
        size: (20),
    }));
    const __VLS_18 = __VLS_17({
        size: (20),
    }, ...__VLS_functionalComponentArgsRest(__VLS_17));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-stat-body" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-stat-value purple" },
    });
    (__VLS_ctx.formatNumber(__VLS_ctx.totalMessages));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bot-stat-label" },
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
            ...{ class: "bot-card-header" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "bot-card-identity" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "bot-card-icon" },
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
            ...{ class: "bot-card-info" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "bot-card-name-row" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-card-name" },
        });
        (bot.name);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-status-indicator" },
            ...{ class: (bot.online ? 'online' : 'offline') },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-status-dot" },
            ...{ class: (bot.online ? 'online' : 'offline') },
        });
        (bot.online ? '在线' : '离线');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "bot-card-detail" },
        });
        if (bot.platform === 'qq' && bot.botId) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
            (bot.botId);
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
            ...{ class: "bot-card-stats" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "bot-s-item" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-s-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-s-value" },
        });
        (bot.groupCount);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "bot-s-item" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-s-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-s-value" },
        });
        (bot.todayMessages);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "bot-s-item" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-s-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-s-value" },
        });
        (__VLS_ctx.formatNumber(bot.totalMessages));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "bot-s-item" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-s-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "bot-s-value" },
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
/** @type {__VLS_StyleScopedClasses['bot-loading']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-loader-spinner']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-body']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-body']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['green']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-body']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['pink']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-body']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['purple']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-divider']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-list']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-card']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-card-header']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-card-identity']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-card-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-card-info']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-card-name-row']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-card-name']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-status-indicator']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-status-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-card-detail']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-card-stats']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-s-item']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-s-label']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-s-value']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-s-item']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-s-label']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-s-value']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-s-item']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-s-label']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-s-value']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-s-item']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-s-label']} */ ;
/** @type {__VLS_StyleScopedClasses['bot-s-value']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            RefreshCw: RefreshCw,
            Cpu: Cpu,
            Activity: Activity,
            MessageCircle: MessageCircle,
            Database: Database,
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
