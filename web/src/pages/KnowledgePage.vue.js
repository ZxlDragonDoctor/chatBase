/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref, onMounted, onUnmounted } from 'vue';
import { Plus, BookOpen, RefreshCw, Edit3, Trash2, Upload, Download, Search } from 'lucide-vue-next';
import { api } from '../api/client';
import { syncFromDify } from '../api/kb';
import { subscribeUploadProgress } from '../api/progress';
import { getOrCreateUserId } from '../lib/user';
const userId = getOrCreateUserId();
const tabs = [{ key: 'kb', label: '知识库' }, { key: 'doc', label: '文档' }, { key: 'faq', label: 'FAQ' }];
const activeTab = ref('kb');
const loading = ref(false);
const syncing = ref(false);
const err = ref(null);
const kbList = ref([]);
const docList = ref([]);
const faqList = ref([]);
const selectedKb = ref(null);
const docLoading = ref(false);
const docSearchKeyword = ref('');
const docSearchTimer = ref(null);
const showCreateKb = ref(false);
const showCreateDoc = ref(false);
const showUploadModal = ref(false);
const formKb = ref({ name: '', description: '' });
const formDoc = ref({ title: '', content: '' });
const uploadFiles = ref([]);
const uploadLoading = ref(false);
const uploadResult = ref(null);
const uploadKbId = ref(null);
const uploadInputRef = ref(null);
const uploadProgress = ref(null);
const uploadEventSource = ref(null);
async function loadKbList() {
    loading.value = true;
    err.value = null;
    try {
        const res = await api.get('/kb/page', { params: { pageNum: 1, pageSize: 100 } });
        kbList.value = res.data.records || [];
    }
    catch (e) {
        err.value = e?.message || '加载失败';
    }
    finally {
        loading.value = false;
    }
}
async function doSyncFromDify() {
    syncing.value = true;
    err.value = null;
    try {
        const result = await syncFromDify();
        if (result.success) {
            await loadKbList();
        }
        else {
            err.value = result.message;
        }
    }
    catch (e) {
        err.value = e?.message || '同步失败';
    }
    finally {
        syncing.value = false;
    }
}
async function loadDocList(kbId) {
    docLoading.value = true;
    try {
        const keyword = docSearchKeyword.value.trim();
        const res = await api.get(`/kb/${kbId}/document/page`, {
            params: {
                pageNum: 1,
                pageSize: 100,
                title: keyword || undefined
            }
        });
        docList.value = res.data.records || [];
    }
    finally {
        docLoading.value = false;
    }
}
function handleDocSearch() {
    if (docSearchTimer.value) {
        clearTimeout(docSearchTimer.value);
    }
    docSearchTimer.value = window.setTimeout(() => {
        if (selectedKb.value) {
            loadDocList(selectedKb.value.id);
        }
    }, 300);
}
function clearDocSearch() {
    docSearchKeyword.value = '';
    if (selectedKb.value) {
        loadDocList(selectedKb.value.id);
    }
}
async function loadFaqList() {
    try {
        const res = await api.get('/kb/conversation/faq/page', { params: { pageNum: 1, pageSize: 100 } });
        faqList.value = res.data.records || [];
    }
    catch {
        faqList.value = [];
    }
}
function loadTabData() {
    if (activeTab.value === 'kb' && kbList.value.length === 0)
        loadKbList();
    else if (activeTab.value === 'faq')
        loadFaqList();
}
async function createKbSubmit() {
    try {
        await api.post('/kb', formKb.value);
        showCreateKb.value = false;
        formKb.value = { name: '', description: '' };
        loadKbList();
    }
    catch (e) {
        err.value = e?.message || '创建失败';
    }
}
async function createDocSubmit() {
    if (!selectedKb.value)
        return;
    try {
        await api.post('/kb/document', { ...formDoc.value, knowledgeBaseId: selectedKb.value.id });
        showCreateDoc.value = false;
        formDoc.value = { title: '', content: '' };
        loadDocList(selectedKb.value.id);
    }
    catch (e) {
        err.value = e?.message || '创建失败';
    }
}
async function deleteKb(kb) {
    if (!confirm(`确定删除知识库 "${kb.name}" 吗？`))
        return;
    try {
        await api.delete(`/kb/${kb.id}`);
        loadKbList();
    }
    catch (e) {
        err.value = e?.message || '删除失败';
    }
}
function viewDocs(kb) {
    selectedKb.value = kb;
    docSearchKeyword.value = '';
    activeTab.value = 'doc';
    loadDocList(kb.id);
}
async function syncKb(kb) {
    try {
        const res = await api.post(`/kb/${kb.id}/sync`);
        if (res.data?.success)
            alert('同步成功');
        else
            alert(res.data?.message || '同步失败');
    }
    catch {
        alert('同步失败');
    }
}
function editKb(kb) { alert('编辑功能开发中'); }
async function deleteDoc(doc) {
    if (!confirm(`确定删除文档 "${doc.title}" 吗？`))
        return;
    try {
        await api.delete(`/kb/document/${doc.id}`);
        if (selectedKb.value)
            loadDocList(selectedKb.value.id);
    }
    catch (e) {
        err.value = e?.message || '删除失败';
    }
}
async function syncDoc(doc) {
    try {
        const res = await api.post(`/kb/document/${doc.id}/sync`);
        if (res.data?.success)
            alert('同步成功');
        else
            alert(res.data?.message || '同步失败');
    }
    catch {
        alert('同步失败');
    }
}
async function deleteFaq(faq) {
    if (!confirm('确定删除FAQ吗？'))
        return;
    try {
        await api.delete(`/kb/conversation/faq/${faq.id}`);
        loadFaqList();
    }
    catch (e) {
        err.value = e?.message || '删除失败';
    }
}
function getSyncColor(status) { if (status === 1)
    return 'green'; if (status === 2)
    return 'pink'; return 'muted'; }
function getSyncText(status) { if (status === 1)
    return '已同步'; if (status === 2)
    return '失败'; return '未同步'; }
function openUploadModal(kb) {
    uploadKbId.value = kb.id;
    uploadFiles.value = [];
    uploadResult.value = null;
    uploadProgress.value = null;
    showUploadModal.value = true;
}
function triggerUploadInput() {
    uploadInputRef.value?.click();
}
function handleUploadFileSelect(e) {
    const el = e.target;
    const files = el.files;
    if (files && files.length > 0) {
        uploadFiles.value = [...uploadFiles.value, ...Array.from(files)];
    }
    el.value = '';
}
function removeUploadFile(idx) {
    uploadFiles.value.splice(idx, 1);
}
async function doBatchUpload() {
    if (uploadFiles.value.length === 0 || !uploadKbId.value)
        return;
    uploadLoading.value = true;
    uploadResult.value = null;
    uploadProgress.value = null;
    const form = new FormData();
    uploadFiles.value.forEach(f => form.append('files', f));
    form.append('user', userId);
    try {
        const resp = await api.post(`/kb/${uploadKbId.value}/batch-upload`, form, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
        if (!resp.data.success || !resp.data.taskId) {
            err.value = resp.data.message || '创建上传任务失败';
            uploadLoading.value = false;
            return;
        }
        const taskId = resp.data.taskId;
        uploadProgress.value = {
            taskId,
            totalCount: uploadFiles.value.length,
            completedCount: 0,
            successCount: 0,
            failedCount: 0,
            currentFile: '准备上传...',
            status: 'pending',
            fileProgresses: [],
            progressPercent: 0,
            createTime: new Date().toISOString(),
            updateTime: new Date().toISOString()
        };
        uploadEventSource.value = subscribeUploadProgress(taskId, (progress) => {
            uploadProgress.value = progress;
        }, (progress) => {
            uploadProgress.value = progress;
            uploadLoading.value = false;
            uploadResult.value = {
                totalCount: progress.totalCount,
                successCount: progress.successCount,
                failedCount: progress.failedCount,
                results: progress.fileProgresses.map(fp => ({
                    fileName: fp.fileName,
                    success: fp.status === 'success',
                    message: fp.message,
                    difyFileId: fp.difyFileId || undefined
                }))
            };
            uploadFiles.value = [];
            if (selectedKb.value)
                loadDocList(selectedKb.value.id);
        }, (errorMsg) => {
            err.value = errorMsg;
            uploadLoading.value = false;
        });
    }
    catch (e) {
        err.value = e?.message || '上传失败';
        uploadLoading.value = false;
    }
}
function formatSize(bytes) {
    if (bytes < 1024)
        return `${bytes} B`;
    if (bytes < 1024 * 1024)
        return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}
onMounted(async () => {
    await loadKbList();
});
onUnmounted(() => {
    if (uploadEventSource.value) {
        uploadEventSource.value.close();
    }
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['doc-search-wrapper']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-search-input']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-search-clear']} */ ;
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
    ...{ onClick: (__VLS_ctx.doSyncFromDify) },
    ...{ class: "anime-btn blue" },
    disabled: (__VLS_ctx.syncing),
});
const __VLS_0 = {}.Download;
/** @type {[typeof __VLS_components.Download, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    size: (18),
}));
const __VLS_2 = __VLS_1({
    size: (18),
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
if (__VLS_ctx.syncing) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.showCreateKb = true;
        } },
    ...{ class: "anime-btn primary" },
});
const __VLS_4 = {}.Plus;
/** @type {[typeof __VLS_components.Plus, ]} */ ;
// @ts-ignore
const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
    size: (18),
}));
const __VLS_6 = __VLS_5({
    size: (18),
}, ...__VLS_functionalComponentArgsRest(__VLS_5));
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
for (const [t] of __VLS_getVForSourceType((__VLS_ctx.tabs))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.activeTab = t.key;
                __VLS_ctx.loadTabData();
            } },
        key: (t.key),
        ...{ class: "anime-tab" },
        ...{ class: ({ active: __VLS_ctx.activeTab === t.key }) },
    });
    (t.label);
}
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
else if (__VLS_ctx.activeTab === 'kb') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "kb-list" },
    });
    if (__VLS_ctx.kbList.length === 0) {
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
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "kb-grid" },
    });
    for (const [kb] of __VLS_getVForSourceType((__VLS_ctx.kbList))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            key: (kb.id),
            ...{ class: "anime-card" },
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ style: {} },
        });
        (kb.name);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-badge blue" },
        });
        (kb.docCount || 0);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        (kb.description || '无描述');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-badge" },
            ...{ class: (kb.status ? 'green' : 'pink') },
        });
        (kb.status ? '启用' : '禁用');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-code" },
        });
        (kb.sourceType);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        (kb.createTime);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!!(__VLS_ctx.loading))
                        return;
                    if (!(__VLS_ctx.activeTab === 'kb'))
                        return;
                    __VLS_ctx.viewDocs(kb);
                } },
            ...{ class: "anime-btn ghost" },
        });
        const __VLS_8 = {}.BookOpen;
        /** @type {[typeof __VLS_components.BookOpen, ]} */ ;
        // @ts-ignore
        const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
            size: (16),
        }));
        const __VLS_10 = __VLS_9({
            size: (16),
        }, ...__VLS_functionalComponentArgsRest(__VLS_9));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!!(__VLS_ctx.loading))
                        return;
                    if (!(__VLS_ctx.activeTab === 'kb'))
                        return;
                    __VLS_ctx.openUploadModal(kb);
                } },
            ...{ class: "anime-btn primary" },
        });
        const __VLS_12 = {}.Upload;
        /** @type {[typeof __VLS_components.Upload, ]} */ ;
        // @ts-ignore
        const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
            size: (16),
        }));
        const __VLS_14 = __VLS_13({
            size: (16),
        }, ...__VLS_functionalComponentArgsRest(__VLS_13));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!!(__VLS_ctx.loading))
                        return;
                    if (!(__VLS_ctx.activeTab === 'kb'))
                        return;
                    __VLS_ctx.syncKb(kb);
                } },
            ...{ class: "anime-btn blue" },
        });
        const __VLS_16 = {}.RefreshCw;
        /** @type {[typeof __VLS_components.RefreshCw, ]} */ ;
        // @ts-ignore
        const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
            size: (16),
        }));
        const __VLS_18 = __VLS_17({
            size: (16),
        }, ...__VLS_functionalComponentArgsRest(__VLS_17));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!!(__VLS_ctx.loading))
                        return;
                    if (!(__VLS_ctx.activeTab === 'kb'))
                        return;
                    __VLS_ctx.editKb(kb);
                } },
            ...{ class: "anime-btn ghost" },
        });
        const __VLS_20 = {}.Edit3;
        /** @type {[typeof __VLS_components.Edit3, ]} */ ;
        // @ts-ignore
        const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
            size: (16),
        }));
        const __VLS_22 = __VLS_21({
            size: (16),
        }, ...__VLS_functionalComponentArgsRest(__VLS_21));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!!(__VLS_ctx.loading))
                        return;
                    if (!(__VLS_ctx.activeTab === 'kb'))
                        return;
                    __VLS_ctx.deleteKb(kb);
                } },
            ...{ class: "anime-btn ghost" },
            ...{ style: {} },
        });
        const __VLS_24 = {}.Trash2;
        /** @type {[typeof __VLS_components.Trash2, ]} */ ;
        // @ts-ignore
        const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
            size: (16),
        }));
        const __VLS_26 = __VLS_25({
            size: (16),
        }, ...__VLS_functionalComponentArgsRest(__VLS_25));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    }
}
else if (__VLS_ctx.activeTab === 'doc') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "doc-section" },
    });
    if (!__VLS_ctx.selectedKb) {
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
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-badge green" },
        });
        (__VLS_ctx.selectedKb.name);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "doc-search-wrapper" },
        });
        const __VLS_28 = {}.Search;
        /** @type {[typeof __VLS_components.Search, ]} */ ;
        // @ts-ignore
        const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
            size: (16),
            ...{ class: "doc-search-icon" },
        }));
        const __VLS_30 = __VLS_29({
            size: (16),
            ...{ class: "doc-search-icon" },
        }, ...__VLS_functionalComponentArgsRest(__VLS_29));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
            ...{ onInput: (__VLS_ctx.handleDocSearch) },
            ...{ class: "doc-search-input" },
            placeholder: "搜索文档...",
        });
        (__VLS_ctx.docSearchKeyword);
        if (__VLS_ctx.docSearchKeyword) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (__VLS_ctx.clearDocSearch) },
                ...{ class: "doc-search-clear" },
            });
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!!(__VLS_ctx.loading))
                        return;
                    if (!!(__VLS_ctx.activeTab === 'kb'))
                        return;
                    if (!(__VLS_ctx.activeTab === 'doc'))
                        return;
                    if (!!(!__VLS_ctx.selectedKb))
                        return;
                    __VLS_ctx.showCreateDoc = true;
                } },
            ...{ class: "anime-btn primary" },
        });
        const __VLS_32 = {}.Plus;
        /** @type {[typeof __VLS_components.Plus, ]} */ ;
        // @ts-ignore
        const __VLS_33 = __VLS_asFunctionalComponent(__VLS_32, new __VLS_32({
            size: (18),
        }));
        const __VLS_34 = __VLS_33({
            size: (18),
        }, ...__VLS_functionalComponentArgsRest(__VLS_33));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
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
                ...{ class: "anime-table" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.thead, __VLS_intrinsicElements.thead)({});
            __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
            __VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
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
                __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
                (doc.title);
                __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "anime-badge" },
                    ...{ class: (doc.status ? 'green' : 'pink') },
                });
                (doc.status ? '启用' : '禁用');
                __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
                    ...{ class: "anime-code" },
                });
                (doc.source);
                __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "anime-badge" },
                    ...{ class: (__VLS_ctx.getSyncColor(doc.syncStatus)) },
                });
                (__VLS_ctx.getSyncText(doc.syncStatus));
                __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
                (doc.createTime);
                __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
                    ...{ style: {} },
                });
                __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                    ...{ onClick: (...[$event]) => {
                            if (!!(__VLS_ctx.loading))
                                return;
                            if (!!(__VLS_ctx.activeTab === 'kb'))
                                return;
                            if (!(__VLS_ctx.activeTab === 'doc'))
                                return;
                            if (!!(!__VLS_ctx.selectedKb))
                                return;
                            if (!!(__VLS_ctx.docLoading))
                                return;
                            if (!!(__VLS_ctx.docList.length === 0))
                                return;
                            __VLS_ctx.syncDoc(doc);
                        } },
                    ...{ class: "anime-btn blue" },
                    ...{ style: {} },
                });
                __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                    ...{ onClick: (...[$event]) => {
                            if (!!(__VLS_ctx.loading))
                                return;
                            if (!!(__VLS_ctx.activeTab === 'kb'))
                                return;
                            if (!(__VLS_ctx.activeTab === 'doc'))
                                return;
                            if (!!(!__VLS_ctx.selectedKb))
                                return;
                            if (!!(__VLS_ctx.docLoading))
                                return;
                            if (!!(__VLS_ctx.docList.length === 0))
                                return;
                            __VLS_ctx.deleteDoc(doc);
                        } },
                    ...{ class: "anime-btn ghost" },
                    ...{ style: {} },
                });
            }
        }
    }
}
else if (__VLS_ctx.activeTab === 'faq') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "faq-section" },
    });
    if (__VLS_ctx.faqList.length === 0) {
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
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "faq-grid" },
    });
    for (const [faq] of __VLS_getVForSourceType((__VLS_ctx.faqList))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            key: (faq.id),
            ...{ class: "anime-card" },
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        (faq.question);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        (faq.answer);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-badge green" },
        });
        (faq.hitCount || 0);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!!(__VLS_ctx.loading))
                        return;
                    if (!!(__VLS_ctx.activeTab === 'kb'))
                        return;
                    if (!!(__VLS_ctx.activeTab === 'doc'))
                        return;
                    if (!(__VLS_ctx.activeTab === 'faq'))
                        return;
                    __VLS_ctx.deleteFaq(faq);
                } },
            ...{ class: "anime-btn ghost" },
            ...{ style: {} },
        });
    }
}
if (__VLS_ctx.showCreateKb) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showCreateKb))
                    return;
                __VLS_ctx.showCreateKb = false;
            } },
        ...{ class: "anime-modal-overlay" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-header" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "anime-modal-title" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showCreateKb))
                    return;
                __VLS_ctx.showCreateKb = false;
            } },
        ...{ class: "anime-modal-close" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-body" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
        ...{ class: "form-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        ...{ class: "anime-input" },
        placeholder: "知识库名称",
    });
    (__VLS_ctx.formKb.name);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
        ...{ class: "form-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.textarea)({
        value: (__VLS_ctx.formKb.description),
        ...{ class: "anime-textarea" },
        placeholder: "知识库描述",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-footer" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.createKbSubmit) },
        ...{ class: "anime-btn primary" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showCreateKb))
                    return;
                __VLS_ctx.showCreateKb = false;
            } },
        ...{ class: "anime-btn ghost" },
    });
}
if (__VLS_ctx.showCreateDoc) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showCreateDoc))
                    return;
                __VLS_ctx.showCreateDoc = false;
            } },
        ...{ class: "anime-modal-overlay" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-header" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "anime-modal-title" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showCreateDoc))
                    return;
                __VLS_ctx.showCreateDoc = false;
            } },
        ...{ class: "anime-modal-close" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-body" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
        ...{ class: "form-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        ...{ class: "anime-input" },
        placeholder: "文档标题",
    });
    (__VLS_ctx.formDoc.title);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
        ...{ class: "form-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.textarea)({
        value: (__VLS_ctx.formDoc.content),
        ...{ class: "anime-textarea" },
        placeholder: "文档内容",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-footer" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.createDocSubmit) },
        ...{ class: "anime-btn primary" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showCreateDoc))
                    return;
                __VLS_ctx.showCreateDoc = false;
            } },
        ...{ class: "anime-btn ghost" },
    });
}
if (__VLS_ctx.showUploadModal) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showUploadModal))
                    return;
                __VLS_ctx.showUploadModal = false;
            } },
        ...{ class: "anime-modal-overlay" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal" },
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-header" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "anime-modal-title" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showUploadModal))
                    return;
                __VLS_ctx.showUploadModal = false;
            } },
        ...{ class: "anime-modal-close" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-body" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        ...{ onChange: (__VLS_ctx.handleUploadFileSelect) },
        ref: "uploadInputRef",
        type: "file",
        multiple: true,
        ...{ style: {} },
        disabled: (__VLS_ctx.uploadLoading),
    });
    /** @type {typeof __VLS_ctx.uploadInputRef} */ ;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.triggerUploadInput) },
        ...{ class: "anime-btn primary" },
    });
    const __VLS_36 = {}.Upload;
    /** @type {[typeof __VLS_components.Upload, ]} */ ;
    // @ts-ignore
    const __VLS_37 = __VLS_asFunctionalComponent(__VLS_36, new __VLS_36({
        size: (18),
    }));
    const __VLS_38 = __VLS_37({
        size: (18),
    }, ...__VLS_functionalComponentArgsRest(__VLS_37));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ style: {} },
    });
    if (__VLS_ctx.uploadFiles.length > 0) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        (__VLS_ctx.uploadFiles.length);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        for (const [f, i] of __VLS_getVForSourceType((__VLS_ctx.uploadFiles))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                key: (i),
                ...{ style: {} },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ style: {} },
            });
            (f.name);
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-code" },
                ...{ style: {} },
            });
            (__VLS_ctx.formatSize(f.size));
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!(__VLS_ctx.showUploadModal))
                            return;
                        if (!(__VLS_ctx.uploadFiles.length > 0))
                            return;
                        __VLS_ctx.removeUploadFile(i);
                    } },
                ...{ class: "anime-btn ghost" },
                ...{ style: {} },
            });
        }
    }
    if (__VLS_ctx.uploadProgress && __VLS_ctx.uploadLoading) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-badge pink" },
        });
        (__VLS_ctx.uploadProgress.status === 'completed' ? '完成' : '上传中');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ style: {} },
        });
        (__VLS_ctx.uploadProgress.completedCount);
        (__VLS_ctx.uploadProgress.totalCount);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "anime-progress" },
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "anime-progress-bar" },
            ...{ style: ({ width: __VLS_ctx.uploadProgress.progressPercent + '%' }) },
        });
        if (__VLS_ctx.uploadProgress.currentFile) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ style: {} },
            });
            (__VLS_ctx.uploadProgress.currentFile);
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        for (const [fp, i] of __VLS_getVForSourceType((__VLS_ctx.uploadProgress.fileProgresses))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                key: (i),
                ...{ style: {} },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ style: {} },
            });
            (fp.fileName);
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ style: ({ color: fp.status === 'success' ? 'var(--anime-green)' : 'var(--anime-pink)' }) },
            });
            (fp.status === 'success' ? '✓' : '✗');
        }
    }
    if (__VLS_ctx.uploadResult && !__VLS_ctx.uploadLoading) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        (__VLS_ctx.uploadResult.successCount);
        (__VLS_ctx.uploadResult.totalCount);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        for (const [r, i] of __VLS_getVForSourceType((__VLS_ctx.uploadResult.results))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                key: (i),
                ...{ style: {} },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
            (r.fileName);
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ style: ({ color: r.success ? 'var(--anime-green)' : 'var(--anime-pink)' }) },
            });
            (r.message);
        }
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-footer" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.doBatchUpload) },
        ...{ class: "anime-btn primary" },
        disabled: (__VLS_ctx.uploadLoading || __VLS_ctx.uploadFiles.length === 0),
    });
    const __VLS_40 = {}.Upload;
    /** @type {[typeof __VLS_components.Upload, ]} */ ;
    // @ts-ignore
    const __VLS_41 = __VLS_asFunctionalComponent(__VLS_40, new __VLS_40({
        size: (18),
    }));
    const __VLS_42 = __VLS_41({
        size: (18),
    }, ...__VLS_functionalComponentArgsRest(__VLS_41));
    if (__VLS_ctx.uploadLoading) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    }
    else {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showUploadModal))
                    return;
                __VLS_ctx.showUploadModal = false;
            } },
        ...{ class: "anime-btn ghost" },
    });
}
/** @type {__VLS_StyleScopedClasses['anime-page-shell']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-header']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-title']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-desc']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['blue']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-error']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-body']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tabs']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tab']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-loader-spinner']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-list']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['blue']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-code']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['blue']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-section']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['green']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-search-wrapper']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-search-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-search-input']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-search-clear']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-loader-spinner']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-table']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-code']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['blue']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['faq-section']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['faq-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['green']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-overlay']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-header']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-title']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-close']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-body']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-textarea']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-footer']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-overlay']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-header']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-title']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-close']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-body']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-textarea']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-footer']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-overlay']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-header']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-title']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-close']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-body']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-code']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['pink']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-progress']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-progress-bar']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-footer']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            Plus: Plus,
            BookOpen: BookOpen,
            RefreshCw: RefreshCw,
            Edit3: Edit3,
            Trash2: Trash2,
            Upload: Upload,
            Download: Download,
            Search: Search,
            tabs: tabs,
            activeTab: activeTab,
            loading: loading,
            syncing: syncing,
            err: err,
            kbList: kbList,
            docList: docList,
            faqList: faqList,
            selectedKb: selectedKb,
            docLoading: docLoading,
            docSearchKeyword: docSearchKeyword,
            showCreateKb: showCreateKb,
            showCreateDoc: showCreateDoc,
            showUploadModal: showUploadModal,
            formKb: formKb,
            formDoc: formDoc,
            uploadFiles: uploadFiles,
            uploadLoading: uploadLoading,
            uploadResult: uploadResult,
            uploadInputRef: uploadInputRef,
            uploadProgress: uploadProgress,
            doSyncFromDify: doSyncFromDify,
            handleDocSearch: handleDocSearch,
            clearDocSearch: clearDocSearch,
            loadTabData: loadTabData,
            createKbSubmit: createKbSubmit,
            createDocSubmit: createDocSubmit,
            deleteKb: deleteKb,
            viewDocs: viewDocs,
            syncKb: syncKb,
            editKb: editKb,
            deleteDoc: deleteDoc,
            syncDoc: syncDoc,
            deleteFaq: deleteFaq,
            getSyncColor: getSyncColor,
            getSyncText: getSyncText,
            openUploadModal: openUploadModal,
            triggerUploadInput: triggerUploadInput,
            handleUploadFileSelect: handleUploadFileSelect,
            removeUploadFile: removeUploadFile,
            doBatchUpload: doBatchUpload,
            formatSize: formatSize,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
