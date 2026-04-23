/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref, watch, onMounted } from 'vue';
import { RefreshCw } from 'lucide-vue-next';
import { fetchGroups, fetchGroupMessages } from '../api/console';
import { api } from '../api/client';
const platformTabs = [{ key: 'all', label: '全部' }, { key: 'qq', label: 'QQ 群' }, { key: 'wecom', label: '企微群' }];
const platform = ref('all');
const groups = ref([]);
const loading = ref(false);
const err = ref(null);
const selected = ref(null);
const messages = ref([]);
const msgLoading = ref(false);
const msgTotal = ref(0);
const page = ref(0);
const pageSize = 40;
const appList = ref([]);
const bindAppId = ref('');
const bindSaving = ref(false);
async function loadApps() {
    try {
        const resp = await api.get('/kb/app/list');
        appList.value = resp.data || [];
    }
    catch (e) {
        console.error('加载应用列表失败', e);
    }
}
async function reload() {
    loading.value = true;
    err.value = null;
    try {
        groups.value = await fetchGroups(platform.value);
        if (selected.value) {
            const still = groups.value.find((g) => g.groupId === selected.value.groupId && g.platform === selected.value.platform);
            if (!still) {
                selected.value = null;
                messages.value = [];
            }
            else {
                selected.value = still;
                bindAppId.value = still.appId || '';
            }
        }
    }
    catch (e) {
        err.value = e?.message || '加载失败';
        groups.value = [];
    }
    finally {
        loading.value = false;
    }
}
function selectGroup(g) {
    selected.value = g;
    bindAppId.value = g.appId || '';
    page.value = 0;
    loadMessages();
}
async function loadMessages() {
    if (!selected.value)
        return;
    const gid = selected.value.groupId;
    if (gid == null || String(gid).trim() === '') {
        messages.value = [];
        msgTotal.value = 0;
        return;
    }
    msgLoading.value = true;
    try {
        const apiPlat = selected.value.platform === 'qq' ? 'qq' : selected.value.platform === 'wx' ? 'wx' : 'all';
        const res = await fetchGroupMessages({ groupId: gid, platform: apiPlat, page: page.value, size: pageSize });
        messages.value = res.records;
        msgTotal.value = res.total;
    }
    catch {
        messages.value = [];
        msgTotal.value = 0;
    }
    finally {
        msgLoading.value = false;
    }
}
async function saveBindApp() {
    if (!selected.value)
        return;
    bindSaving.value = true;
    try {
        const appId = bindAppId.value ? Number(bindAppId.value) : null;
        const appName = appId ? appList.value.find(a => a.id === appId)?.name : null;
        if (appId) {
            await api.put(`/console/groups/${selected.value.id}/app`, { appId, appName });
            selected.value.appId = appId;
            selected.value.appName = appName;
        }
        else {
            await api.delete(`/console/groups/${selected.value.id}/app`);
            selected.value.appId = null;
            selected.value.appName = null;
        }
        reload();
    }
    catch (e) {
        err.value = e?.response?.data?.message || '绑定失败';
    }
    finally {
        bindSaving.value = false;
    }
}
function getPlatformColor(p) { if (p === 'qq')
    return 'green'; if (p === 'wecom')
    return 'blue'; return 'purple'; }
function getPlatformLabel(p) { if (p === 'qq')
    return 'QQ群'; if (p === 'wecom')
    return '企微群'; return '群聊'; }
function getGroupName(g) {
    if (g.groupName && g.groupName.trim())
        return g.groupName.trim();
    if (g.platform === 'qq')
        return `QQ群`;
    if (g.platform === 'wecom')
        return `企微群`;
    return `群聊`;
}
function formatTime(t) {
    if (!t)
        return '';
    if (Array.isArray(t) && t.length >= 5) {
        const [y, m, d, h = 0, min = 0] = t;
        return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}`;
    }
    if (typeof t === 'string')
        return t.slice(0, 16).replace('T', ' ');
    return String(t);
}
watch(platform, () => { selected.value = null; messages.value = []; reload(); });
onMounted(() => {
    reload();
    loadApps();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['im-split-view']} */ ;
/** @type {__VLS_StyleScopedClasses['im-group-item']} */ ;
/** @type {__VLS_StyleScopedClasses['im-group-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-app-select']} */ ;
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
    ...{ onClick: (__VLS_ctx.reload) },
    ...{ class: "anime-btn ghost" },
    disabled: (__VLS_ctx.loading),
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
if (__VLS_ctx.err) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-error" },
        ...{ style: {} },
    });
    (__VLS_ctx.err);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-card-body" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-tabs" },
    ...{ style: {} },
});
for (const [t] of __VLS_getVForSourceType((__VLS_ctx.platformTabs))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.platform = t.key;
            } },
        key: (t.key),
        ...{ class: "anime-tab" },
        ...{ class: ({ active: __VLS_ctx.platform === t.key }) },
    });
    (t.label);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "im-split-view" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "im-list-panel" },
});
if (__VLS_ctx.loading && __VLS_ctx.groups.length === 0) {
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
else if (__VLS_ctx.groups.length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-empty" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-empty-icon" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-empty-text" },
    });
}
for (const [g] of __VLS_getVForSourceType((__VLS_ctx.groups))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.selectGroup(g);
            } },
        key: (g.platform + ':' + g.groupId),
        ...{ class: "im-group-item" },
        ...{ class: ({ 'active': __VLS_ctx.selected?.groupId === g.groupId && __VLS_ctx.selected?.platform === g.platform }) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "anime-badge" },
        ...{ class: (__VLS_ctx.getPlatformColor(g.platform)) },
    });
    (__VLS_ctx.getPlatformLabel(g.platform));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ style: {} },
    });
    (__VLS_ctx.getGroupName(g));
    if (g.appName) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-badge blue" },
            ...{ style: {} },
        });
        (g.appName);
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ style: {} },
    });
    (g.messageCount);
    if (g.lastMessageTime) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.formatTime(g.lastMessageTime));
    }
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "im-detail-panel" },
});
if (!__VLS_ctx.selected) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-empty" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-empty-icon" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-empty-text" },
    });
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "anime-badge" },
        ...{ class: (__VLS_ctx.getPlatformColor(__VLS_ctx.selected.platform)) },
    });
    (__VLS_ctx.getPlatformLabel(__VLS_ctx.selected.platform));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ style: {} },
    });
    (__VLS_ctx.getGroupName(__VLS_ctx.selected));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
        value: (__VLS_ctx.bindAppId),
        ...{ class: "anime-app-select" },
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        value: "",
    });
    for (const [app] of __VLS_getVForSourceType((__VLS_ctx.appList))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
            key: (app.id),
            value: (app.id),
        });
        (app.icon || '🤖');
        (app.name);
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.saveBindApp) },
        ...{ class: "anime-btn primary" },
        disabled: (__VLS_ctx.bindSaving),
    });
    if (__VLS_ctx.bindSaving) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    }
    else {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    }
    if (__VLS_ctx.selected.appName) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ style: {} },
        });
        (__VLS_ctx.selected.appName);
    }
    if (__VLS_ctx.msgLoading) {
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
            ...{ class: "message-list" },
        });
        for (const [m] of __VLS_getVForSourceType((__VLS_ctx.messages))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                key: (m.id),
                ...{ style: {} },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ style: {} },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-badge muted" },
            });
            (m.messageType || '文本');
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-badge" },
                ...{ class: (m.synced ? 'green' : 'pink') },
            });
            (m.synced ? '已同步' : '未同步');
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ style: {} },
            });
            (__VLS_ctx.formatTime(m.messageTime));
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ style: {} },
            });
            (m.rawMessage);
        }
    }
    if (__VLS_ctx.msgTotal > __VLS_ctx.messages.length) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!!(!__VLS_ctx.selected))
                        return;
                    if (!(__VLS_ctx.msgTotal > __VLS_ctx.messages.length))
                        return;
                    __VLS_ctx.page--;
                    __VLS_ctx.loadMessages();
                } },
            ...{ class: "anime-btn ghost" },
            disabled: (__VLS_ctx.msgLoading || __VLS_ctx.page <= 0),
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-code" },
        });
        (__VLS_ctx.page + 1);
        (__VLS_ctx.msgTotal);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!!(!__VLS_ctx.selected))
                        return;
                    if (!(__VLS_ctx.msgTotal > __VLS_ctx.messages.length))
                        return;
                    __VLS_ctx.page++;
                    __VLS_ctx.loadMessages();
                } },
            ...{ class: "anime-btn ghost" },
            disabled: (__VLS_ctx.msgLoading || (__VLS_ctx.page + 1) * __VLS_ctx.pageSize >= __VLS_ctx.msgTotal),
        });
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
/** @type {__VLS_StyleScopedClasses['anime-tabs']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tab']} */ ;
/** @type {__VLS_StyleScopedClasses['im-split-view']} */ ;
/** @type {__VLS_StyleScopedClasses['im-list-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-loader-spinner']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['im-group-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['blue']} */ ;
/** @type {__VLS_StyleScopedClasses['im-detail-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-app-select']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-loader-spinner']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['message-list']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-code']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            RefreshCw: RefreshCw,
            platformTabs: platformTabs,
            platform: platform,
            groups: groups,
            loading: loading,
            err: err,
            selected: selected,
            messages: messages,
            msgLoading: msgLoading,
            msgTotal: msgTotal,
            page: page,
            pageSize: pageSize,
            appList: appList,
            bindAppId: bindAppId,
            bindSaving: bindSaving,
            reload: reload,
            selectGroup: selectGroup,
            loadMessages: loadMessages,
            saveBindApp: saveBindApp,
            getPlatformColor: getPlatformColor,
            getPlatformLabel: getPlatformLabel,
            getGroupName: getGroupName,
            formatTime: formatTime,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
