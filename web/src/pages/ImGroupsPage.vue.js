/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref, watch, onMounted } from 'vue';
import { fetchGroups, fetchGroupMessages } from '../api/console';
import { platformLabel } from '../lib/platformLabel';
const platformTabs = [
    { key: 'all', label: '全部' },
    { key: 'qq', label: 'QQ 群' },
    { key: 'wecom', label: '企微群聊' },
];
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
        const res = await fetchGroupMessages({
            groupId: gid,
            platform: apiPlat,
            page: page.value,
            size: pageSize,
        });
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
watch(platform, () => {
    selected.value = null;
    messages.value = [];
    reload();
});
onMounted(() => reload());
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "pageShell" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "card imCard" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "cardHeader" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "h1" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "muted" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.code, __VLS_intrinsicElements.code)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "right" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.reload) },
    type: "button",
    ...{ class: "btn btnGhost" },
    disabled: (__VLS_ctx.loading),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "imToolbar" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "tabs" },
});
for (const [t] of __VLS_getVForSourceType((__VLS_ctx.platformTabs))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.platform = t.key;
            } },
        key: (t.key),
        type: "button",
        ...{ class: "tab" },
        ...{ class: ({ active: __VLS_ctx.platform === t.key }) },
    });
    (t.label);
}
if (__VLS_ctx.err) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "error" },
        ...{ style: {} },
    });
    (__VLS_ctx.err);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "imSplit" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "imListPane" },
});
if (__VLS_ctx.loading && __VLS_ctx.groups.length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "muted" },
        ...{ style: {} },
    });
}
else if (__VLS_ctx.groups.length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "empty" },
        ...{ style: {} },
    });
}
for (const [g] of __VLS_getVForSourceType((__VLS_ctx.groups))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.selectGroup(g);
            } },
        key: (g.platform + ':' + g.groupId),
        type: "button",
        ...{ class: "imRow" },
        ...{ class: ({ active: __VLS_ctx.selected?.groupId === g.groupId && __VLS_ctx.selected?.platform === g.platform }) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "imRowPlat" },
    });
    (__VLS_ctx.platformLabel(g.platform));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "imRowId" },
    });
    (g.groupId);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "imRowMeta" },
    });
    (g.messageCount);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "imDetailPane" },
});
if (!__VLS_ctx.selected) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "muted" },
        ...{ style: {} },
    });
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "imDetailHead" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "h2" },
        ...{ style: {} },
    });
    (__VLS_ctx.platformLabel(__VLS_ctx.selected.platform));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.code, __VLS_intrinsicElements.code)({
        ...{ class: "imDetailGid" },
    });
    (__VLS_ctx.selected.groupId);
    if (__VLS_ctx.msgLoading) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "muted" },
            ...{ style: {} },
        });
    }
    else {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "msgScroll" },
        });
        for (const [m] of __VLS_getVForSourceType((__VLS_ctx.messages))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                key: (m.id),
                ...{ class: "msgItem" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "msgItemTop" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "badge" },
            });
            (m.messageType || 'text');
            if (m.synced != null) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "badge" },
                });
                (m.synced ? '已同步' : '未同步');
            }
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "muted" },
            });
            (m.messageTime || '');
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "muted" },
            });
            (m.userId || '—');
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "msgItemBody" },
            });
            (m.rawMessage);
        }
    }
    if (__VLS_ctx.msgTotal > __VLS_ctx.messages.length) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "imPager" },
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
            type: "button",
            ...{ class: "btn btnGhost" },
            disabled: (__VLS_ctx.msgLoading || __VLS_ctx.page <= 0),
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "muted" },
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
            type: "button",
            ...{ class: "btn btnGhost" },
            disabled: (__VLS_ctx.msgLoading || (__VLS_ctx.page + 1) * __VLS_ctx.pageSize >= __VLS_ctx.msgTotal),
        });
    }
}
/** @type {__VLS_StyleScopedClasses['pageShell']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['imCard']} */ ;
/** @type {__VLS_StyleScopedClasses['cardHeader']} */ ;
/** @type {__VLS_StyleScopedClasses['h1']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['right']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btnGhost']} */ ;
/** @type {__VLS_StyleScopedClasses['imToolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['tabs']} */ ;
/** @type {__VLS_StyleScopedClasses['tab']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
/** @type {__VLS_StyleScopedClasses['imSplit']} */ ;
/** @type {__VLS_StyleScopedClasses['imListPane']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['empty']} */ ;
/** @type {__VLS_StyleScopedClasses['imRow']} */ ;
/** @type {__VLS_StyleScopedClasses['imRowPlat']} */ ;
/** @type {__VLS_StyleScopedClasses['imRowId']} */ ;
/** @type {__VLS_StyleScopedClasses['imRowMeta']} */ ;
/** @type {__VLS_StyleScopedClasses['imDetailPane']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['imDetailHead']} */ ;
/** @type {__VLS_StyleScopedClasses['h2']} */ ;
/** @type {__VLS_StyleScopedClasses['imDetailGid']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['msgScroll']} */ ;
/** @type {__VLS_StyleScopedClasses['msgItem']} */ ;
/** @type {__VLS_StyleScopedClasses['msgItemTop']} */ ;
/** @type {__VLS_StyleScopedClasses['badge']} */ ;
/** @type {__VLS_StyleScopedClasses['badge']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['msgItemBody']} */ ;
/** @type {__VLS_StyleScopedClasses['imPager']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btnGhost']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btnGhost']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            platformLabel: platformLabel,
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
            reload: reload,
            selectGroup: selectGroup,
            loadMessages: loadMessages,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
