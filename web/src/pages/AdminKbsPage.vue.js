/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref, computed, onMounted } from 'vue';
import { Search, RefreshCw } from 'lucide-vue-next';
import { api } from '../api/client';
const kbList = ref([]);
const categoryList = ref([]);
const loading = ref(true);
const err = ref('');
const keyword = ref('');
const showDetail = ref(false);
const detail = ref(null);
const detailTab = ref('info');
const docList = ref([]);
const docLoading = ref(false);
const docPage = ref(1);
const docPageSize = 10;
const docTotal = ref(0);
const docTotalPages = computed(() => Math.ceil(docTotal.value / docPageSize));
const filteredList = computed(() => {
    const kw = keyword.value.toLowerCase().trim();
    if (!kw)
        return kbList.value;
    return kbList.value.filter(kb => kb.name.toLowerCase().includes(kw) ||
        kb.description?.toLowerCase().includes(kw) ||
        kb.createBy?.toLowerCase().includes(kw) ||
        getCategoryName(kb.categoryId).toLowerCase().includes(kw));
});
function getCategoryName(id) {
    if (!id)
        return '无分类';
    const cat = categoryList.value.find(c => c.id === id);
    return cat?.name || '未知';
}
function maskKey(key) {
    if (!key)
        return '-';
    if (key.length <= 8)
        return '****';
    return key.slice(0, 4) + '****' + key.slice(-4);
}
function formatSize(bytes) {
    if (bytes == null)
        return '-';
    if (bytes < 1024)
        return bytes + ' B';
    if (bytes < 1024 * 1024)
        return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}
function getSyncClass(doc) {
    if (doc.syncStatus === 1)
        return 'green';
    if (doc.syncStatus === 2)
        return 'pink';
    return 'muted';
}
function getSyncLabel(doc) {
    if (doc.syncStatus === 1)
        return '已同步';
    if (doc.syncStatus === 2)
        return '失败';
    return '未同步';
}
function switchDocTab() {
    detailTab.value = 'docs';
    if (docList.value.length === 0) {
        docPage.value = 1;
        loadDocuments();
    }
}
async function loadDocuments() {
    if (!detail.value)
        return;
    docLoading.value = true;
    try {
        const res = await api.get(`/kb/${detail.value.id}/document/page`, {
            params: { pageNum: docPage.value, pageSize: docPageSize }
        });
        docList.value = res.data.records || [];
        docTotal.value = res.data.total || 0;
    }
    catch {
        docList.value = [];
        docTotal.value = 0;
    }
    finally {
        docLoading.value = false;
    }
}
async function openDetail(id) {
    detailTab.value = 'info';
    docList.value = [];
    docTotal.value = 0;
    docPage.value = 1;
    try {
        const res = await api.get(`/kb/${id}`);
        detail.value = res.data;
        showDetail.value = true;
    }
    catch {
        err.value = '加载详情失败';
    }
}
async function loadKbList() {
    loading.value = true;
    err.value = '';
    try {
        const res = await api.get('/kb/admin/page', { params: { pageNum: 1, pageSize: 100 } });
        kbList.value = res.data.records || [];
    }
    catch (e) {
        err.value = e.response?.data?.message || '加载失败';
    }
    finally {
        loading.value = false;
    }
}
async function loadCategories() {
    try {
        const res = await api.get('/kb/category/tree');
        const flat = [];
        const flatten = (items) => {
            items.forEach((item) => {
                flat.push({ id: item.id, name: item.name });
                if (item.children?.length)
                    flatten(item.children);
            });
        };
        flatten(res.data || []);
        categoryList.value = flat;
    }
    catch {
        categoryList.value = [];
    }
}
onMounted(async () => {
    await Promise.all([loadKbList(), loadCategories()]);
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['kb-card']} */ ;
/** @type {__VLS_StyleScopedClasses['detail-table']} */ ;
/** @type {__VLS_StyleScopedClasses['detail-table']} */ ;
/** @type {__VLS_StyleScopedClasses['detail-table']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-table']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-table']} */ ;
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
    ...{ class: "filter-bar" },
});
const __VLS_0 = {}.Search;
/** @type {[typeof __VLS_components.Search, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    size: (16),
    ...{ class: "search-icon" },
}));
const __VLS_2 = __VLS_1({
    size: (16),
    ...{ class: "search-icon" },
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ class: "anime-input" },
    placeholder: "搜索知识库名称、描述、创建者...",
    ...{ style: {} },
});
(__VLS_ctx.keyword);
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "filter-count" },
});
(__VLS_ctx.filteredList.length);
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.loadKbList) },
    ...{ class: "anime-btn ghost" },
    disabled: (__VLS_ctx.loading),
});
const __VLS_4 = {}.RefreshCw;
/** @type {[typeof __VLS_components.RefreshCw, ]} */ ;
// @ts-ignore
const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
    size: (16),
}));
const __VLS_6 = __VLS_5({
    size: (16),
}, ...__VLS_functionalComponentArgsRest(__VLS_5));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
if (__VLS_ctx.loading) {
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
else if (__VLS_ctx.filteredList.length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-empty" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-empty-icon" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-empty-text" },
    });
    (__VLS_ctx.keyword ? '未找到匹配的知识库' : '暂无知识库');
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "kb-grid" },
    });
    for (const [kb] of __VLS_getVForSourceType((__VLS_ctx.filteredList))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ onClick: (...[$event]) => {
                    if (!!(__VLS_ctx.loading))
                        return;
                    if (!!(__VLS_ctx.filteredList.length === 0))
                        return;
                    __VLS_ctx.openDetail(kb.id);
                } },
            key: (kb.id),
            ...{ class: "anime-card kb-card" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "kb-header" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "kb-name" },
        });
        (kb.name);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "kb-badges" },
        });
        if (kb.categoryId) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-badge purple" },
            });
            (__VLS_ctx.getCategoryName(kb.categoryId));
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-badge blue" },
        });
        (kb.docCount || 0);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "kb-desc" },
        });
        (kb.description || '无描述');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "kb-meta" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-badge" },
            ...{ class: (kb.status ? 'green' : 'pink') },
        });
        (kb.status ? '启用' : '禁用');
        if (kb.isPublic !== undefined) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-badge" },
                ...{ class: (kb.isPublic ? 'green' : 'gray') },
            });
            (kb.isPublic ? '公开' : '私有');
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-code" },
        });
        (kb.sourceType || '手动');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "kb-time" },
        });
        (kb.createBy || '-');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "kb-time" },
        });
        (kb.createTime);
    }
}
if (__VLS_ctx.showDetail && __VLS_ctx.detail) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showDetail && __VLS_ctx.detail))
                    return;
                __VLS_ctx.showDetail = false;
            } },
        ...{ class: "anime-modal-overlay" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal detail-modal" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-header" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "modal-title" },
    });
    (__VLS_ctx.detail.id);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showDetail && __VLS_ctx.detail))
                    return;
                __VLS_ctx.showDetail = false;
            } },
        ...{ class: "anime-modal-close" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-tabs" },
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showDetail && __VLS_ctx.detail))
                    return;
                __VLS_ctx.detailTab = 'info';
            } },
        ...{ class: "anime-tab" },
        ...{ class: ({ active: __VLS_ctx.detailTab === 'info' }) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.switchDocTab) },
        ...{ class: "anime-tab" },
        ...{ class: ({ active: __VLS_ctx.detailTab === 'docs' }) },
    });
    (__VLS_ctx.detail.docCount || 0);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-body" },
        ...{ style: {} },
    });
    if (__VLS_ctx.detailTab === 'info') {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.table, __VLS_intrinsicElements.table)({
            ...{ class: "detail-table" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-value" },
        });
        (__VLS_ctx.detail.name);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-value" },
        });
        (__VLS_ctx.detail.description || '无');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-value" },
        });
        (__VLS_ctx.getCategoryName(__VLS_ctx.detail.categoryId));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-value anime-code" },
        });
        (__VLS_ctx.detail.difyDatasetId || '-');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-value anime-code" },
        });
        (__VLS_ctx.maskKey(__VLS_ctx.detail.difyApiKey));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-value" },
        });
        (__VLS_ctx.detail.sourceType || '手动');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-value" },
        });
        (__VLS_ctx.detail.syncPlatform || '-');
        if (__VLS_ctx.detail.syncGroupIds) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
            __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
                ...{ class: "dt-label" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
                ...{ class: "dt-value anime-code" },
            });
            (__VLS_ctx.detail.syncGroupIds);
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-value" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-badge" },
            ...{ class: (__VLS_ctx.detail.autoSync ? 'green' : 'gray') },
        });
        (__VLS_ctx.detail.autoSync ? '开启' : '关闭');
        if (__VLS_ctx.detail.autoSync) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
            __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
                ...{ class: "dt-label" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
                ...{ class: "dt-value" },
            });
            (__VLS_ctx.detail.syncInterval || '-');
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-value" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-badge" },
            ...{ class: (__VLS_ctx.detail.isPublic ? 'green' : 'gray') },
        });
        (__VLS_ctx.detail.isPublic ? '是' : '否');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-value" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-badge" },
            ...{ class: (__VLS_ctx.detail.status ? 'green' : 'pink') },
        });
        (__VLS_ctx.detail.status ? '启用' : '禁用');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-value" },
        });
        (__VLS_ctx.detail.docCount || 0);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-value" },
        });
        (__VLS_ctx.detail.chunkCount || 0);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-value" },
        });
        (__VLS_ctx.detail.createBy || '-');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-value" },
        });
        (__VLS_ctx.detail.createTime || '-');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            ...{ class: "dt-value" },
        });
        (__VLS_ctx.detail.updateTime || '-');
    }
    else {
        if (__VLS_ctx.docLoading) {
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
        else if (__VLS_ctx.docList.length === 0) {
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
            __VLS_asFunctionalElement(__VLS_intrinsicElements.table, __VLS_intrinsicElements.table)({
                ...{ class: "doc-table" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.thead, __VLS_intrinsicElements.thead)({});
            __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
            __VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
            __VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
            __VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
            __VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
            __VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
            __VLS_asFunctionalElement(__VLS_intrinsicElements.tbody, __VLS_intrinsicElements.tbody)({});
            for (const [doc] of __VLS_getVForSourceType((__VLS_ctx.docList))) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({
                    key: (doc.id),
                });
                __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
                    ...{ class: "doc-title-cell" },
                });
                (doc.title);
                __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "anime-badge muted" },
                });
                (doc.fileType || '文本');
                __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
                    ...{ class: "anime-code" },
                });
                (__VLS_ctx.formatSize(doc.fileSize));
                __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "anime-badge" },
                    ...{ class: (__VLS_ctx.getSyncClass(doc)) },
                });
                (__VLS_ctx.getSyncLabel(doc));
                __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
                    ...{ class: "doc-time" },
                });
                (doc.createTime);
            }
        }
        if (__VLS_ctx.docTotal > __VLS_ctx.docList.length) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "doc-pagination" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!(__VLS_ctx.showDetail && __VLS_ctx.detail))
                            return;
                        if (!!(__VLS_ctx.detailTab === 'info'))
                            return;
                        if (!(__VLS_ctx.docTotal > __VLS_ctx.docList.length))
                            return;
                        __VLS_ctx.docPage--;
                        __VLS_ctx.loadDocuments();
                    } },
                ...{ class: "anime-btn ghost sm" },
                disabled: (__VLS_ctx.docPage <= 1 || __VLS_ctx.docLoading),
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-code" },
            });
            (__VLS_ctx.docPage);
            (__VLS_ctx.docTotalPages);
            (__VLS_ctx.docTotal);
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!(__VLS_ctx.showDetail && __VLS_ctx.detail))
                            return;
                        if (!!(__VLS_ctx.detailTab === 'info'))
                            return;
                        if (!(__VLS_ctx.docTotal > __VLS_ctx.docList.length))
                            return;
                        __VLS_ctx.docPage++;
                        __VLS_ctx.loadDocuments();
                    } },
                ...{ class: "anime-btn ghost sm" },
                disabled: (__VLS_ctx.docPage >= __VLS_ctx.docTotalPages || __VLS_ctx.docLoading),
            });
        }
    }
}
/** @type {__VLS_StyleScopedClasses['anime-page-shell']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-header']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-title']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-desc']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-error']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-body']} */ ;
/** @type {__VLS_StyleScopedClasses['filter-bar']} */ ;
/** @type {__VLS_StyleScopedClasses['search-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['filter-count']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-loader-spinner']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-card']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-header']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-name']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-badges']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['purple']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['blue']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-desc']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-code']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-time']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-time']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-overlay']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal']} */ ;
/** @type {__VLS_StyleScopedClasses['detail-modal']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-header']} */ ;
/** @type {__VLS_StyleScopedClasses['modal-title']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-close']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tabs']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tab']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tab']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-body']} */ ;
/** @type {__VLS_StyleScopedClasses['detail-table']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-value']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-value']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-value']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-value']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-code']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-value']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-code']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-value']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-value']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-value']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-code']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-value']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-value']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-value']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-value']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-value']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-value']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-value']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-value']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-label']} */ ;
/** @type {__VLS_StyleScopedClasses['dt-value']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-loader-spinner']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-table']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-title-cell']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-code']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-time']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-pagination']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-code']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            Search: Search,
            RefreshCw: RefreshCw,
            loading: loading,
            err: err,
            keyword: keyword,
            showDetail: showDetail,
            detail: detail,
            detailTab: detailTab,
            docList: docList,
            docLoading: docLoading,
            docPage: docPage,
            docTotal: docTotal,
            docTotalPages: docTotalPages,
            filteredList: filteredList,
            getCategoryName: getCategoryName,
            maskKey: maskKey,
            formatSize: formatSize,
            getSyncClass: getSyncClass,
            getSyncLabel: getSyncLabel,
            switchDocTab: switchDocTab,
            loadDocuments: loadDocuments,
            openDetail: openDetail,
            loadKbList: loadKbList,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
