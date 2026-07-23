/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { Plus, BookOpen, RefreshCw, Edit3, Trash2, Upload, Download, Search } from 'lucide-vue-next';
import { api } from '../api/client';
import { syncFromDify } from '../api/kb';
import { subscribeUploadProgress } from '../api/progress';
import { getOrCreateUserId } from '../lib/user';
const userId = getOrCreateUserId();
const currentUserName = computed(() => localStorage.getItem('chatbase_original_username') || localStorage.getItem('chatbase_user') || '');
const kbTab = ref('mine');
const tabs = [
    { key: 'category', label: '分类' },
    { key: 'kb', label: '知识库' },
    { key: 'doc', label: '文档' },
    { key: 'faq', label: 'FAQ' }
];
const activeTab = ref('category');
const loading = ref(false);
const syncing = ref(false);
const err = ref(null);
const categoryTree = ref([]);
const flatCategories = computed(() => {
    const flat = [];
    const flatten = (items) => {
        items.forEach(item => {
            flat.push(item);
            if (item.children?.length)
                flatten(item.children);
        });
    };
    flatten(categoryTree.value);
    return flat;
});
const kbList = ref([]);
const filterCategoryId = ref('');
const filteredKbList = computed(() => {
    let list = kbList.value;
    if (kbTab.value === 'mine') {
        list = list.filter(kb => kb.createBy === currentUserName.value);
    }
    else if (kbTab.value === 'all') {
        list = list.filter(kb => kb.isPublic);
    }
    if (filterCategoryId.value) {
        list = list.filter(kb => kb.categoryId === Number(filterCategoryId.value));
    }
    return list;
});
const docList = ref([]);
const faqList = ref([]);
const selectedKb = ref(null);
const docLoading = ref(false);
const docSearchKeyword = ref('');
const docSearchTimer = ref(null);
const showCreateCategory = ref(false);
const showCreateKb = ref(false);
const showCreateDoc = ref(false);
const showUploadModal = ref(false);
const showAddKbToCategoryModal = ref(false);
const selectedCategory = ref(null);
const addKbSearchKeyword = ref('');
const selectedKbIds = ref([]);
const addingKb = ref(false);
const availableKbList = ref([]);
const filteredAvailableKbList = computed(() => {
    const keyword = addKbSearchKeyword.value.toLowerCase().trim();
    if (!keyword)
        return availableKbList.value;
    return availableKbList.value.filter(kb => kb.name.toLowerCase().includes(keyword));
});
const editingCategory = ref(null);
const editingKb = ref(null);
const formCategory = ref({ name: '', icon: '', parentId: '', sortOrder: 0, description: '' });
const formKb = ref({ name: '', categoryId: '', description: '', isPublic: true });
const formDoc = ref({ title: '', content: '' });
const savingCategory = ref(false);
const savingKb = ref(false);
const uploadFiles = ref([]);
const uploadLoading = ref(false);
const uploadResult = ref(null);
const uploadKbId = ref(null);
const uploadInputRef = ref(null);
const uploadProgress = ref(null);
const uploadEventSource = ref(null);
async function loadCategoryTree() {
    try {
        const res = await api.get('/kb/category/tree');
        categoryTree.value = res.data || [];
    }
    catch (e) {
        console.error('加载分类失败', e);
        categoryTree.value = [];
    }
}
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
function filterKbList() { }
function getCategoryName(id) {
    if (!id)
        return '无分类';
    const cat = flatCategories.value.find(c => c.id === id);
    return cat?.name || '未知';
}
function getCategoryKbList(catId) {
    return kbList.value.filter(kb => kb.categoryId === catId);
}
async function doSyncFromDify() {
    syncing.value = true;
    err.value = null;
    try {
        const result = await syncFromDify();
        if (result.success) {
            await Promise.all([loadKbList(), loadCategoryTree()]);
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
            params: { pageNum: 1, pageSize: 100, title: keyword || undefined }
        });
        docList.value = res.data.records || [];
    }
    finally {
        docLoading.value = false;
    }
}
function handleDocSearch() {
    if (docSearchTimer.value)
        clearTimeout(docSearchTimer.value);
    docSearchTimer.value = window.setTimeout(() => {
        if (selectedKb.value)
            loadDocList(selectedKb.value.id);
    }, 300);
}
function clearDocSearch() {
    docSearchKeyword.value = '';
    if (selectedKb.value)
        loadDocList(selectedKb.value.id);
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
    if (activeTab.value === 'category') {
        if (categoryTree.value.length === 0)
            loadCategoryTree();
        if (kbList.value.length === 0)
            loadKbList();
    }
    else if (activeTab.value === 'kb' && kbList.value.length === 0)
        loadKbList();
    else if (activeTab.value === 'faq')
        loadFaqList();
}
async function saveCategory() {
    if (!formCategory.value.name) {
        err.value = '请输入分类名称';
        return;
    }
    savingCategory.value = true;
    try {
        const payload = {
            name: formCategory.value.name,
            icon: formCategory.value.icon,
            parentId: formCategory.value.parentId ? Number(formCategory.value.parentId) : null,
            sortOrder: formCategory.value.sortOrder || 0,
            description: formCategory.value.description
        };
        if (editingCategory.value) {
            await api.put('/kb/category', { ...payload, id: editingCategory.value.id });
        }
        else {
            await api.post('/kb/category', payload);
        }
        closeCategoryModal();
        loadCategoryTree();
    }
    catch (e) {
        err.value = e.response?.data?.message || '保存失败';
    }
    finally {
        savingCategory.value = false;
    }
}
function editCategory(cat) {
    editingCategory.value = cat;
    formCategory.value = {
        name: cat.name,
        icon: cat.icon || '',
        parentId: cat.parentId ? String(cat.parentId) : '',
        sortOrder: cat.sortOrder || 0,
        description: cat.description || ''
    };
    showCreateCategory.value = true;
}
async function deleteCategory(cat) {
    if (cat.kbCount && cat.kbCount > 0) {
        alert('该分类下有知识库，无法删除');
        return;
    }
    if (!confirm(`确定删除分类 "${cat.name}"？`))
        return;
    try {
        const res = await api.delete(`/kb/category/${cat.id}`);
        if (res.data && res.data.success === false) {
            err.value = res.data.message || '删除失败';
            return;
        }
        loadCategoryTree();
    }
    catch (e) {
        err.value = e.response?.data?.message || '删除失败';
    }
}
function closeCategoryModal() {
    showCreateCategory.value = false;
    editingCategory.value = null;
    formCategory.value = { name: '', icon: '', parentId: '', sortOrder: 0, description: '' };
    err.value = '';
}
function viewCategoryKb(cat) {
    filterCategoryId.value = String(cat.id);
    activeTab.value = 'kb';
}
function addKbToCategory(cat) {
    selectedCategory.value = cat;
    addKbSearchKeyword.value = '';
    selectedKbIds.value = [];
    availableKbList.value = kbList.value;
    showAddKbToCategoryModal.value = true;
}
function toggleKbSelection(kbId) {
    const idx = selectedKbIds.value.indexOf(kbId);
    if (idx >= 0) {
        selectedKbIds.value.splice(idx, 1);
    }
    else {
        selectedKbIds.value.push(kbId);
    }
}
async function confirmAddKbToCategory() {
    if (selectedKbIds.value.length === 0 || !selectedCategory.value)
        return;
    addingKb.value = true;
    try {
        for (const kbId of selectedKbIds.value) {
            await api.put('/kb', { id: kbId, categoryId: selectedCategory.value.id });
        }
        showAddKbToCategoryModal.value = false;
        await Promise.all([loadKbList(), loadCategoryTree()]);
    }
    catch (e) {
        err.value = e.response?.data?.message || '添加失败';
    }
    finally {
        addingKb.value = false;
    }
}
async function saveKb() {
    if (!formKb.value.name) {
        err.value = '请输入知识库名称';
        return;
    }
    savingKb.value = true;
    try {
        const payload = {
            name: formKb.value.name,
            categoryId: formKb.value.categoryId ? Number(formKb.value.categoryId) : null,
            description: formKb.value.description,
            isPublic: formKb.value.isPublic
        };
        if (editingKb.value) {
            await api.put('/kb', { ...payload, id: editingKb.value.id });
        }
        else {
            await api.post('/kb', payload);
        }
        closeKbModal();
        loadKbList();
    }
    catch (e) {
        err.value = e.response?.data?.message || '保存失败';
    }
    finally {
        savingKb.value = false;
    }
}
function editKb(kb) {
    editingKb.value = kb;
    formKb.value = {
        name: kb.name,
        categoryId: kb.categoryId ? String(kb.categoryId) : '',
        description: kb.description || '',
        isPublic: kb.isPublic !== undefined ? kb.isPublic : true
    };
    showCreateKb.value = true;
}
async function deleteKb(kb) {
    if (!confirm(`确定删除知识库 "${kb.name}"？`))
        return;
    try {
        await api.delete(`/kb/${kb.id}`);
        loadKbList();
    }
    catch (e) {
        err.value = e.response?.data?.message || '删除失败';
    }
}
function closeKbModal() {
    showCreateKb.value = false;
    editingKb.value = null;
    formKb.value = { name: '', categoryId: '', description: '', isPublic: true };
    err.value = '';
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
async function createDocSubmit() {
    if (!selectedKb.value || !formDoc.value.title)
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
async function deleteDoc(doc) {
    if (!confirm(`确定删除文档 "${doc.title}"？`))
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
    if (!confirm('确定删除FAQ？'))
        return;
    try {
        await api.delete(`/kb/conversation/faq/${faq.id}`);
        loadFaqList();
    }
    catch (e) {
        err.value = e?.message || '删除失败';
    }
}
function getSyncColor(status) {
    if (status === 1)
        return 'green';
    if (status === 2)
        return 'pink';
    return 'muted';
}
function getSyncText(status) {
    if (status === 1)
        return '已同步';
    if (status === 2)
        return '失败';
    return '未同步';
}
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
        uploadEventSource.value = subscribeUploadProgress(taskId, (progress) => { uploadProgress.value = progress; }, (progress) => {
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
    await Promise.all([loadCategoryTree(), loadKbList()]);
});
onUnmounted(() => {
    if (uploadEventSource.value)
        uploadEventSource.value.close();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['category-item']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-mini-card']} */ ;
/** @type {__VLS_StyleScopedClasses['add-kb-item']} */ ;
/** @type {__VLS_StyleScopedClasses['add-kb-item']} */ ;
/** @type {__VLS_StyleScopedClasses['filter-select']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-search-wrapper']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-search-input']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-search-clear']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
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
if (__VLS_ctx.activeTab === 'category') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.activeTab === 'category'))
                    return;
                __VLS_ctx.showCreateCategory = true;
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
}
if (__VLS_ctx.activeTab === 'kb') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.activeTab === 'kb'))
                    return;
                __VLS_ctx.showCreateKb = true;
            } },
        ...{ class: "anime-btn primary" },
    });
    const __VLS_8 = {}.Plus;
    /** @type {[typeof __VLS_components.Plus, ]} */ ;
    // @ts-ignore
    const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
        size: (18),
    }));
    const __VLS_10 = __VLS_9({
        size: (18),
    }, ...__VLS_functionalComponentArgsRest(__VLS_9));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
}
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
else if (__VLS_ctx.activeTab === 'category') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "category-section" },
    });
    if (__VLS_ctx.categoryTree.length === 0) {
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
            ...{ class: "category-tree" },
        });
        for (const [cat] of __VLS_getVForSourceType((__VLS_ctx.categoryTree))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                key: (cat.id),
                ...{ class: "category-item" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "category-header" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "category-info" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "category-icon" },
            });
            (cat.icon || '📁');
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "category-name" },
            });
            (cat.name);
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-badge blue" },
            });
            (cat.kbCount || 0);
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "category-actions" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!!(__VLS_ctx.loading))
                            return;
                        if (!(__VLS_ctx.activeTab === 'category'))
                            return;
                        if (!!(__VLS_ctx.categoryTree.length === 0))
                            return;
                        __VLS_ctx.addKbToCategory(cat);
                    } },
                ...{ class: "anime-btn primary sm" },
            });
            const __VLS_12 = {}.Plus;
            /** @type {[typeof __VLS_components.Plus, ]} */ ;
            // @ts-ignore
            const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
                size: (14),
            }));
            const __VLS_14 = __VLS_13({
                size: (14),
            }, ...__VLS_functionalComponentArgsRest(__VLS_13));
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
            if (cat.createBy === __VLS_ctx.currentUserName) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                    ...{ onClick: (...[$event]) => {
                            if (!!(__VLS_ctx.loading))
                                return;
                            if (!(__VLS_ctx.activeTab === 'category'))
                                return;
                            if (!!(__VLS_ctx.categoryTree.length === 0))
                                return;
                            if (!(cat.createBy === __VLS_ctx.currentUserName))
                                return;
                            __VLS_ctx.editCategory(cat);
                        } },
                    ...{ class: "anime-btn ghost sm" },
                });
                const __VLS_16 = {}.Edit3;
                /** @type {[typeof __VLS_components.Edit3, ]} */ ;
                // @ts-ignore
                const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
                    size: (14),
                }));
                const __VLS_18 = __VLS_17({
                    size: (14),
                }, ...__VLS_functionalComponentArgsRest(__VLS_17));
            }
            if (cat.createBy === __VLS_ctx.currentUserName) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                    ...{ onClick: (...[$event]) => {
                            if (!!(__VLS_ctx.loading))
                                return;
                            if (!(__VLS_ctx.activeTab === 'category'))
                                return;
                            if (!!(__VLS_ctx.categoryTree.length === 0))
                                return;
                            if (!(cat.createBy === __VLS_ctx.currentUserName))
                                return;
                            __VLS_ctx.deleteCategory(cat);
                        } },
                    ...{ class: "anime-btn ghost sm danger" },
                });
                const __VLS_20 = {}.Trash2;
                /** @type {[typeof __VLS_components.Trash2, ]} */ ;
                // @ts-ignore
                const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
                    size: (14),
                }));
                const __VLS_22 = __VLS_21({
                    size: (14),
                }, ...__VLS_functionalComponentArgsRest(__VLS_21));
            }
            if (__VLS_ctx.getCategoryKbList(cat.id).length > 0) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                    ...{ class: "category-kb-list" },
                });
                for (const [kb] of __VLS_getVForSourceType((__VLS_ctx.getCategoryKbList(cat.id)))) {
                    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                        ...{ onClick: (...[$event]) => {
                                if (!!(__VLS_ctx.loading))
                                    return;
                                if (!(__VLS_ctx.activeTab === 'category'))
                                    return;
                                if (!!(__VLS_ctx.categoryTree.length === 0))
                                    return;
                                if (!(__VLS_ctx.getCategoryKbList(cat.id).length > 0))
                                    return;
                                __VLS_ctx.viewDocs(kb);
                            } },
                        key: (kb.id),
                        ...{ class: "kb-mini-card" },
                    });
                    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                        ...{ class: "kb-mini-name" },
                    });
                    (kb.name);
                    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                        ...{ class: "anime-badge muted" },
                    });
                    (kb.docCount || 0);
                    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                        ...{ onClick: (...[$event]) => {
                                if (!!(__VLS_ctx.loading))
                                    return;
                                if (!(__VLS_ctx.activeTab === 'category'))
                                    return;
                                if (!!(__VLS_ctx.categoryTree.length === 0))
                                    return;
                                if (!(__VLS_ctx.getCategoryKbList(cat.id).length > 0))
                                    return;
                                __VLS_ctx.editKb(kb);
                            } },
                        ...{ class: "anime-btn ghost xs" },
                    });
                    const __VLS_24 = {}.Edit3;
                    /** @type {[typeof __VLS_components.Edit3, ]} */ ;
                    // @ts-ignore
                    const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
                        size: (12),
                    }));
                    const __VLS_26 = __VLS_25({
                        size: (12),
                    }, ...__VLS_functionalComponentArgsRest(__VLS_25));
                }
            }
            else {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                    ...{ class: "category-kb-empty" },
                });
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ style: {} },
                });
                __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                    ...{ onClick: (...[$event]) => {
                            if (!!(__VLS_ctx.loading))
                                return;
                            if (!(__VLS_ctx.activeTab === 'category'))
                                return;
                            if (!!(__VLS_ctx.categoryTree.length === 0))
                                return;
                            if (!!(__VLS_ctx.getCategoryKbList(cat.id).length > 0))
                                return;
                            __VLS_ctx.addKbToCategory(cat);
                        } },
                    ...{ class: "anime-btn ghost sm" },
                });
                const __VLS_28 = {}.Plus;
                /** @type {[typeof __VLS_components.Plus, ]} */ ;
                // @ts-ignore
                const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
                    size: (14),
                }));
                const __VLS_30 = __VLS_29({
                    size: (14),
                }, ...__VLS_functionalComponentArgsRest(__VLS_29));
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
            }
            if (cat.children && cat.children.length > 0) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                    ...{ class: "category-children" },
                });
                for (const [child] of __VLS_getVForSourceType((cat.children))) {
                    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                        key: (child.id),
                        ...{ class: "category-item child" },
                    });
                    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                        ...{ class: "category-header" },
                    });
                    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                        ...{ class: "category-info" },
                    });
                    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                        ...{ class: "category-icon" },
                    });
                    (child.icon || '📂');
                    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                        ...{ class: "category-name" },
                    });
                    (child.name);
                    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                        ...{ class: "anime-badge purple" },
                    });
                    (child.kbCount || 0);
                    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                        ...{ class: "category-actions" },
                    });
                    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                        ...{ onClick: (...[$event]) => {
                                if (!!(__VLS_ctx.loading))
                                    return;
                                if (!(__VLS_ctx.activeTab === 'category'))
                                    return;
                                if (!!(__VLS_ctx.categoryTree.length === 0))
                                    return;
                                if (!(cat.children && cat.children.length > 0))
                                    return;
                                __VLS_ctx.addKbToCategory(child);
                            } },
                        ...{ class: "anime-btn primary sm" },
                    });
                    const __VLS_32 = {}.Plus;
                    /** @type {[typeof __VLS_components.Plus, ]} */ ;
                    // @ts-ignore
                    const __VLS_33 = __VLS_asFunctionalComponent(__VLS_32, new __VLS_32({
                        size: (14),
                    }));
                    const __VLS_34 = __VLS_33({
                        size: (14),
                    }, ...__VLS_functionalComponentArgsRest(__VLS_33));
                    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
                    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                        ...{ onClick: (...[$event]) => {
                                if (!!(__VLS_ctx.loading))
                                    return;
                                if (!(__VLS_ctx.activeTab === 'category'))
                                    return;
                                if (!!(__VLS_ctx.categoryTree.length === 0))
                                    return;
                                if (!(cat.children && cat.children.length > 0))
                                    return;
                                __VLS_ctx.editCategory(child);
                            } },
                        ...{ class: "anime-btn ghost sm" },
                    });
                    const __VLS_36 = {}.Edit3;
                    /** @type {[typeof __VLS_components.Edit3, ]} */ ;
                    // @ts-ignore
                    const __VLS_37 = __VLS_asFunctionalComponent(__VLS_36, new __VLS_36({
                        size: (14),
                    }));
                    const __VLS_38 = __VLS_37({
                        size: (14),
                    }, ...__VLS_functionalComponentArgsRest(__VLS_37));
                    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                        ...{ onClick: (...[$event]) => {
                                if (!!(__VLS_ctx.loading))
                                    return;
                                if (!(__VLS_ctx.activeTab === 'category'))
                                    return;
                                if (!!(__VLS_ctx.categoryTree.length === 0))
                                    return;
                                if (!(cat.children && cat.children.length > 0))
                                    return;
                                __VLS_ctx.deleteCategory(child);
                            } },
                        ...{ class: "anime-btn ghost sm danger" },
                    });
                    const __VLS_40 = {}.Trash2;
                    /** @type {[typeof __VLS_components.Trash2, ]} */ ;
                    // @ts-ignore
                    const __VLS_41 = __VLS_asFunctionalComponent(__VLS_40, new __VLS_40({
                        size: (14),
                    }));
                    const __VLS_42 = __VLS_41({
                        size: (14),
                    }, ...__VLS_functionalComponentArgsRest(__VLS_41));
                    if (__VLS_ctx.getCategoryKbList(child.id).length > 0) {
                        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                            ...{ class: "category-kb-list" },
                        });
                        for (const [kb] of __VLS_getVForSourceType((__VLS_ctx.getCategoryKbList(child.id)))) {
                            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                                ...{ onClick: (...[$event]) => {
                                        if (!!(__VLS_ctx.loading))
                                            return;
                                        if (!(__VLS_ctx.activeTab === 'category'))
                                            return;
                                        if (!!(__VLS_ctx.categoryTree.length === 0))
                                            return;
                                        if (!(cat.children && cat.children.length > 0))
                                            return;
                                        if (!(__VLS_ctx.getCategoryKbList(child.id).length > 0))
                                            return;
                                        __VLS_ctx.viewDocs(kb);
                                    } },
                                key: (kb.id),
                                ...{ class: "kb-mini-card" },
                            });
                            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                                ...{ class: "kb-mini-name" },
                            });
                            (kb.name);
                            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                                ...{ class: "anime-badge muted" },
                            });
                            (kb.docCount || 0);
                        }
                    }
                }
            }
        }
    }
}
else if (__VLS_ctx.activeTab === 'kb') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "kb-list" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "kb-sub-tabs" },
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!!(__VLS_ctx.loading))
                    return;
                if (!!(__VLS_ctx.activeTab === 'category'))
                    return;
                if (!(__VLS_ctx.activeTab === 'kb'))
                    return;
                __VLS_ctx.kbTab = 'mine';
            } },
        ...{ class: "anime-tab" },
        ...{ class: ({ active: __VLS_ctx.kbTab === 'mine' }) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!!(__VLS_ctx.loading))
                    return;
                if (!!(__VLS_ctx.activeTab === 'category'))
                    return;
                if (!(__VLS_ctx.activeTab === 'kb'))
                    return;
                __VLS_ctx.kbTab = 'all';
            } },
        ...{ class: "anime-tab" },
        ...{ class: ({ active: __VLS_ctx.kbTab === 'all' }) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "kb-filter-bar" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "filter-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
        ...{ onChange: (__VLS_ctx.filterKbList) },
        value: (__VLS_ctx.filterCategoryId),
        ...{ class: "filter-select" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        value: "",
    });
    for (const [cat] of __VLS_getVForSourceType((__VLS_ctx.flatCategories))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
            key: (cat.id),
            value: (cat.id),
        });
        (cat.name);
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "kb-count-info" },
    });
    (__VLS_ctx.filteredKbList.length);
    if (__VLS_ctx.filteredKbList.length === 0) {
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
            ...{ class: "kb-grid" },
        });
        for (const [kb] of __VLS_getVForSourceType((__VLS_ctx.filteredKbList))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
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
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "kb-actions" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!!(__VLS_ctx.loading))
                            return;
                        if (!!(__VLS_ctx.activeTab === 'category'))
                            return;
                        if (!(__VLS_ctx.activeTab === 'kb'))
                            return;
                        if (!!(__VLS_ctx.filteredKbList.length === 0))
                            return;
                        __VLS_ctx.viewDocs(kb);
                    } },
                ...{ class: "anime-btn ghost" },
            });
            const __VLS_44 = {}.BookOpen;
            /** @type {[typeof __VLS_components.BookOpen, ]} */ ;
            // @ts-ignore
            const __VLS_45 = __VLS_asFunctionalComponent(__VLS_44, new __VLS_44({
                size: (16),
            }));
            const __VLS_46 = __VLS_45({
                size: (16),
            }, ...__VLS_functionalComponentArgsRest(__VLS_45));
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!!(__VLS_ctx.loading))
                            return;
                        if (!!(__VLS_ctx.activeTab === 'category'))
                            return;
                        if (!(__VLS_ctx.activeTab === 'kb'))
                            return;
                        if (!!(__VLS_ctx.filteredKbList.length === 0))
                            return;
                        __VLS_ctx.openUploadModal(kb);
                    } },
                ...{ class: "anime-btn primary" },
            });
            const __VLS_48 = {}.Upload;
            /** @type {[typeof __VLS_components.Upload, ]} */ ;
            // @ts-ignore
            const __VLS_49 = __VLS_asFunctionalComponent(__VLS_48, new __VLS_48({
                size: (16),
            }));
            const __VLS_50 = __VLS_49({
                size: (16),
            }, ...__VLS_functionalComponentArgsRest(__VLS_49));
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!!(__VLS_ctx.loading))
                            return;
                        if (!!(__VLS_ctx.activeTab === 'category'))
                            return;
                        if (!(__VLS_ctx.activeTab === 'kb'))
                            return;
                        if (!!(__VLS_ctx.filteredKbList.length === 0))
                            return;
                        __VLS_ctx.syncKb(kb);
                    } },
                ...{ class: "anime-btn blue" },
            });
            const __VLS_52 = {}.RefreshCw;
            /** @type {[typeof __VLS_components.RefreshCw, ]} */ ;
            // @ts-ignore
            const __VLS_53 = __VLS_asFunctionalComponent(__VLS_52, new __VLS_52({
                size: (16),
            }));
            const __VLS_54 = __VLS_53({
                size: (16),
            }, ...__VLS_functionalComponentArgsRest(__VLS_53));
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
            if (kb.createBy === __VLS_ctx.currentUserName) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                    ...{ onClick: (...[$event]) => {
                            if (!!(__VLS_ctx.loading))
                                return;
                            if (!!(__VLS_ctx.activeTab === 'category'))
                                return;
                            if (!(__VLS_ctx.activeTab === 'kb'))
                                return;
                            if (!!(__VLS_ctx.filteredKbList.length === 0))
                                return;
                            if (!(kb.createBy === __VLS_ctx.currentUserName))
                                return;
                            __VLS_ctx.editKb(kb);
                        } },
                    ...{ class: "anime-btn ghost" },
                });
                const __VLS_56 = {}.Edit3;
                /** @type {[typeof __VLS_components.Edit3, ]} */ ;
                // @ts-ignore
                const __VLS_57 = __VLS_asFunctionalComponent(__VLS_56, new __VLS_56({
                    size: (16),
                }));
                const __VLS_58 = __VLS_57({
                    size: (16),
                }, ...__VLS_functionalComponentArgsRest(__VLS_57));
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
            }
            if (kb.createBy === __VLS_ctx.currentUserName) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                    ...{ onClick: (...[$event]) => {
                            if (!!(__VLS_ctx.loading))
                                return;
                            if (!!(__VLS_ctx.activeTab === 'category'))
                                return;
                            if (!(__VLS_ctx.activeTab === 'kb'))
                                return;
                            if (!!(__VLS_ctx.filteredKbList.length === 0))
                                return;
                            if (!(kb.createBy === __VLS_ctx.currentUserName))
                                return;
                            __VLS_ctx.deleteKb(kb);
                        } },
                    ...{ class: "anime-btn ghost danger" },
                });
                const __VLS_60 = {}.Trash2;
                /** @type {[typeof __VLS_components.Trash2, ]} */ ;
                // @ts-ignore
                const __VLS_61 = __VLS_asFunctionalComponent(__VLS_60, new __VLS_60({
                    size: (16),
                }));
                const __VLS_62 = __VLS_61({
                    size: (16),
                }, ...__VLS_functionalComponentArgsRest(__VLS_61));
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
            }
        }
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
            ...{ class: "doc-header" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "doc-title-row" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-badge green" },
        });
        (__VLS_ctx.selectedKb.name);
        if (__VLS_ctx.selectedKb.categoryId) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-badge purple" },
            });
            (__VLS_ctx.getCategoryName(__VLS_ctx.selectedKb.categoryId));
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "doc-actions" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "doc-search-wrapper" },
        });
        const __VLS_64 = {}.Search;
        /** @type {[typeof __VLS_components.Search, ]} */ ;
        // @ts-ignore
        const __VLS_65 = __VLS_asFunctionalComponent(__VLS_64, new __VLS_64({
            size: (16),
            ...{ class: "doc-search-icon" },
        }));
        const __VLS_66 = __VLS_65({
            size: (16),
            ...{ class: "doc-search-icon" },
        }, ...__VLS_functionalComponentArgsRest(__VLS_65));
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
                    if (!!(__VLS_ctx.activeTab === 'category'))
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
        const __VLS_68 = {}.Plus;
        /** @type {[typeof __VLS_components.Plus, ]} */ ;
        // @ts-ignore
        const __VLS_69 = __VLS_asFunctionalComponent(__VLS_68, new __VLS_68({
            size: (18),
        }));
        const __VLS_70 = __VLS_69({
            size: (18),
        }, ...__VLS_functionalComponentArgsRest(__VLS_69));
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
                            if (!!(__VLS_ctx.activeTab === 'category'))
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
                    ...{ class: "anime-btn blue sm" },
                });
                __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                    ...{ onClick: (...[$event]) => {
                            if (!!(__VLS_ctx.loading))
                                return;
                            if (!!(__VLS_ctx.activeTab === 'category'))
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
                    ...{ class: "anime-btn ghost sm danger" },
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
    else {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "faq-grid" },
        });
        for (const [faq] of __VLS_getVForSourceType((__VLS_ctx.faqList))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                key: (faq.id),
                ...{ class: "anime-card faq-card" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "faq-question" },
            });
            (faq.question);
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "faq-answer" },
            });
            (faq.answer);
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "faq-footer" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-badge green" },
            });
            (faq.hitCount || 0);
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!!(__VLS_ctx.loading))
                            return;
                        if (!!(__VLS_ctx.activeTab === 'category'))
                            return;
                        if (!!(__VLS_ctx.activeTab === 'kb'))
                            return;
                        if (!!(__VLS_ctx.activeTab === 'doc'))
                            return;
                        if (!(__VLS_ctx.activeTab === 'faq'))
                            return;
                        if (!!(__VLS_ctx.faqList.length === 0))
                            return;
                        __VLS_ctx.deleteFaq(faq);
                    } },
                ...{ class: "anime-btn ghost sm danger" },
            });
        }
    }
}
if (__VLS_ctx.showCreateCategory) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showCreateCategory))
                    return;
                __VLS_ctx.showCreateCategory = false;
            } },
        ...{ class: "anime-modal-overlay" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-header" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.editingCategory ? '编辑分类' : '新建分类');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.closeCategoryModal) },
        ...{ class: "anime-modal-close" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-body" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        ...{ class: "anime-input" },
        placeholder: "如：医疗知识",
    });
    (__VLS_ctx.formCategory.name);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        ...{ class: "anime-input" },
        placeholder: "如：🏥、📚、💼",
    });
    (__VLS_ctx.formCategory.icon);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
        value: (__VLS_ctx.formCategory.parentId),
        ...{ class: "anime-input" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        value: "",
    });
    for (const [cat] of __VLS_getVForSourceType((__VLS_ctx.flatCategories))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
            key: (cat.id),
            value: (cat.id),
        });
        (cat.name);
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        ...{ class: "anime-input" },
        type: "number",
        placeholder: "数字越小越靠前",
    });
    (__VLS_ctx.formCategory.sortOrder);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
        value: (__VLS_ctx.formCategory.description),
        ...{ class: "anime-input" },
        rows: "2",
        placeholder: "分类描述",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-footer" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.closeCategoryModal) },
        ...{ class: "anime-btn ghost" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.saveCategory) },
        ...{ class: "anime-btn primary" },
        disabled: (__VLS_ctx.savingCategory),
    });
    if (__VLS_ctx.savingCategory) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    }
    else {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.editingCategory ? '更新' : '创建');
    }
}
if (__VLS_ctx.showAddKbToCategoryModal) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showAddKbToCategoryModal))
                    return;
                __VLS_ctx.showAddKbToCategoryModal = false;
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
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.selectedCategory?.name);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showAddKbToCategoryModal))
                    return;
                __VLS_ctx.showAddKbToCategoryModal = false;
            } },
        ...{ class: "anime-modal-close" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-body" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "add-kb-search" },
    });
    const __VLS_72 = {}.Search;
    /** @type {[typeof __VLS_components.Search, ]} */ ;
    // @ts-ignore
    const __VLS_73 = __VLS_asFunctionalComponent(__VLS_72, new __VLS_72({
        size: (16),
    }));
    const __VLS_74 = __VLS_73({
        size: (16),
    }, ...__VLS_functionalComponentArgsRest(__VLS_73));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        ...{ class: "anime-input" },
        placeholder: "搜索知识库...",
    });
    (__VLS_ctx.addKbSearchKeyword);
    if (__VLS_ctx.availableKbList.length === 0) {
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
            ...{ class: "add-kb-list" },
        });
        for (const [kb] of __VLS_getVForSourceType((__VLS_ctx.filteredAvailableKbList))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ onClick: (...[$event]) => {
                        if (!(__VLS_ctx.showAddKbToCategoryModal))
                            return;
                        if (!!(__VLS_ctx.availableKbList.length === 0))
                            return;
                        __VLS_ctx.toggleKbSelection(kb.id);
                    } },
                key: (kb.id),
                ...{ class: "add-kb-item" },
                ...{ class: ({ selected: __VLS_ctx.selectedKbIds.includes(kb.id) }) },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "add-kb-info" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "add-kb-name" },
            });
            (kb.name);
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-badge blue" },
            });
            (kb.docCount || 0);
            if (kb.categoryId) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "anime-badge purple" },
                });
                (__VLS_ctx.getCategoryName(kb.categoryId));
            }
            else {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "anime-badge muted" },
                });
            }
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "add-kb-check" },
            });
            if (__VLS_ctx.selectedKbIds.includes(kb.id)) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "check-icon" },
                });
            }
        }
    }
    if (__VLS_ctx.selectedKbIds.length > 0) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "selection-info" },
        });
        (__VLS_ctx.selectedKbIds.length);
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-footer" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showAddKbToCategoryModal))
                    return;
                __VLS_ctx.showAddKbToCategoryModal = false;
            } },
        ...{ class: "anime-btn ghost" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.confirmAddKbToCategory) },
        ...{ class: "anime-btn primary" },
        disabled: (__VLS_ctx.selectedKbIds.length === 0 || __VLS_ctx.addingKb),
    });
    if (__VLS_ctx.addingKb) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    }
    else {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
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
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.editingKb ? '编辑知识库' : '新建知识库');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.closeKbModal) },
        ...{ class: "anime-modal-close" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-body" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        ...{ class: "anime-input" },
        placeholder: "知识库名称",
    });
    (__VLS_ctx.formKb.name);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
        value: (__VLS_ctx.formKb.categoryId),
        ...{ class: "anime-input" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        value: "",
    });
    for (const [cat] of __VLS_getVForSourceType((__VLS_ctx.flatCategories))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
            key: (cat.id),
            value: (cat.id),
        });
        (cat.name);
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
        value: (__VLS_ctx.formKb.description),
        ...{ class: "anime-input" },
        rows: "2",
        placeholder: "知识库描述",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        type: "checkbox",
    });
    (__VLS_ctx.formKb.isPublic);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-footer" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.closeKbModal) },
        ...{ class: "anime-btn ghost" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.saveKb) },
        ...{ class: "anime-btn primary" },
        disabled: (__VLS_ctx.savingKb),
    });
    if (__VLS_ctx.savingKb) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    }
    else {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.editingKb ? '更新' : '创建');
    }
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
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
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
        ...{ class: "anime-form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        ...{ class: "anime-input" },
        placeholder: "文档标题",
    });
    (__VLS_ctx.formDoc.title);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
        value: (__VLS_ctx.formDoc.content),
        ...{ class: "anime-input" },
        rows: "4",
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
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
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
    const __VLS_76 = {}.Upload;
    /** @type {[typeof __VLS_components.Upload, ]} */ ;
    // @ts-ignore
    const __VLS_77 = __VLS_asFunctionalComponent(__VLS_76, new __VLS_76({
        size: (18),
    }));
    const __VLS_78 = __VLS_77({
        size: (18),
    }, ...__VLS_functionalComponentArgsRest(__VLS_77));
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
                ...{ class: "anime-btn ghost sm" },
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
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-footer" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.doBatchUpload) },
        ...{ class: "anime-btn primary" },
        disabled: (__VLS_ctx.uploadLoading || __VLS_ctx.uploadFiles.length === 0),
    });
    const __VLS_80 = {}.Upload;
    /** @type {[typeof __VLS_components.Upload, ]} */ ;
    // @ts-ignore
    const __VLS_81 = __VLS_asFunctionalComponent(__VLS_80, new __VLS_80({
        size: (18),
    }));
    const __VLS_82 = __VLS_81({
        size: (18),
    }, ...__VLS_functionalComponentArgsRest(__VLS_81));
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
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-error']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-body']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tabs']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tab']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-loader-spinner']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['category-section']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['category-tree']} */ ;
/** @type {__VLS_StyleScopedClasses['category-item']} */ ;
/** @type {__VLS_StyleScopedClasses['category-header']} */ ;
/** @type {__VLS_StyleScopedClasses['category-info']} */ ;
/** @type {__VLS_StyleScopedClasses['category-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['category-name']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['blue']} */ ;
/** @type {__VLS_StyleScopedClasses['category-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['danger']} */ ;
/** @type {__VLS_StyleScopedClasses['category-kb-list']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-mini-card']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-mini-name']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['xs']} */ ;
/** @type {__VLS_StyleScopedClasses['category-kb-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['category-children']} */ ;
/** @type {__VLS_StyleScopedClasses['category-item']} */ ;
/** @type {__VLS_StyleScopedClasses['child']} */ ;
/** @type {__VLS_StyleScopedClasses['category-header']} */ ;
/** @type {__VLS_StyleScopedClasses['category-info']} */ ;
/** @type {__VLS_StyleScopedClasses['category-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['category-name']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['purple']} */ ;
/** @type {__VLS_StyleScopedClasses['category-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['danger']} */ ;
/** @type {__VLS_StyleScopedClasses['category-kb-list']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-mini-card']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-mini-name']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-list']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-sub-tabs']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tab']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tab']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-filter-bar']} */ ;
/** @type {__VLS_StyleScopedClasses['filter-group']} */ ;
/** @type {__VLS_StyleScopedClasses['filter-select']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-count-info']} */ ;
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
/** @type {__VLS_StyleScopedClasses['kb-actions']} */ ;
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
/** @type {__VLS_StyleScopedClasses['danger']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-section']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-header']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-title-row']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['green']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['purple']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-actions']} */ ;
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
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['danger']} */ ;
/** @type {__VLS_StyleScopedClasses['faq-section']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['faq-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card']} */ ;
/** @type {__VLS_StyleScopedClasses['faq-card']} */ ;
/** @type {__VLS_StyleScopedClasses['faq-question']} */ ;
/** @type {__VLS_StyleScopedClasses['faq-answer']} */ ;
/** @type {__VLS_StyleScopedClasses['faq-footer']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['green']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['danger']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-overlay']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-header']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-close']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-body']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-footer']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-overlay']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-header']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-close']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-body']} */ ;
/** @type {__VLS_StyleScopedClasses['add-kb-search']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['add-kb-list']} */ ;
/** @type {__VLS_StyleScopedClasses['add-kb-item']} */ ;
/** @type {__VLS_StyleScopedClasses['add-kb-info']} */ ;
/** @type {__VLS_StyleScopedClasses['add-kb-name']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['blue']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['purple']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['add-kb-check']} */ ;
/** @type {__VLS_StyleScopedClasses['check-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['selection-info']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-footer']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-overlay']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-header']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-close']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-body']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-footer']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-overlay']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-header']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-close']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-body']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-footer']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-overlay']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-header']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-close']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-body']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-code']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
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
            currentUserName: currentUserName,
            kbTab: kbTab,
            tabs: tabs,
            activeTab: activeTab,
            loading: loading,
            syncing: syncing,
            err: err,
            categoryTree: categoryTree,
            flatCategories: flatCategories,
            filterCategoryId: filterCategoryId,
            filteredKbList: filteredKbList,
            docList: docList,
            faqList: faqList,
            selectedKb: selectedKb,
            docLoading: docLoading,
            docSearchKeyword: docSearchKeyword,
            showCreateCategory: showCreateCategory,
            showCreateKb: showCreateKb,
            showCreateDoc: showCreateDoc,
            showUploadModal: showUploadModal,
            showAddKbToCategoryModal: showAddKbToCategoryModal,
            selectedCategory: selectedCategory,
            addKbSearchKeyword: addKbSearchKeyword,
            selectedKbIds: selectedKbIds,
            addingKb: addingKb,
            availableKbList: availableKbList,
            filteredAvailableKbList: filteredAvailableKbList,
            editingCategory: editingCategory,
            editingKb: editingKb,
            formCategory: formCategory,
            formKb: formKb,
            formDoc: formDoc,
            savingCategory: savingCategory,
            savingKb: savingKb,
            uploadFiles: uploadFiles,
            uploadLoading: uploadLoading,
            uploadResult: uploadResult,
            uploadInputRef: uploadInputRef,
            uploadProgress: uploadProgress,
            filterKbList: filterKbList,
            getCategoryName: getCategoryName,
            getCategoryKbList: getCategoryKbList,
            doSyncFromDify: doSyncFromDify,
            handleDocSearch: handleDocSearch,
            clearDocSearch: clearDocSearch,
            loadTabData: loadTabData,
            saveCategory: saveCategory,
            editCategory: editCategory,
            deleteCategory: deleteCategory,
            closeCategoryModal: closeCategoryModal,
            addKbToCategory: addKbToCategory,
            toggleKbSelection: toggleKbSelection,
            confirmAddKbToCategory: confirmAddKbToCategory,
            saveKb: saveKb,
            editKb: editKb,
            deleteKb: deleteKb,
            closeKbModal: closeKbModal,
            viewDocs: viewDocs,
            syncKb: syncKb,
            createDocSubmit: createDocSubmit,
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
