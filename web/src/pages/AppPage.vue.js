/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref, computed, onMounted } from 'vue';
import { RouterLink } from 'vue-router';
import { Plus, RefreshCw, Edit3, Trash2, Star, Users, BookOpen, ExternalLink, CheckCircle, XCircle } from 'lucide-vue-next';
import { api } from '../api/client';
const difyConsoleUrl = 'https://cloud.dify.ai';
const appList = ref([]);
const categoryList = ref([]);
const kbList = ref([]);
const loading = ref(true);
const err = ref('');
const showCreateModal = ref(false);
const showGroupsModal = ref(false);
const showKbModal = ref(false);
const editingApp = ref(null);
const selectedApp = ref(null);
const selectedCategory = ref(null);
const boundGroups = ref([]);
const categoryKbList = ref([]);
const verifiedInfo = ref(null);
const verifyError = ref('');
const saving = ref(false);
const groupsTab = ref('qq');
const qqGroups = computed(() => boundGroups.value.filter((g) => g.platform === 'qq'));
const wxGroups = computed(() => boundGroups.value.filter((g) => g.platform === 'wx' || g.platform === 'wecom'));
const form = ref({
    name: '',
    description: '',
    icon: '',
    difyApiKey: '',
    categoryId: '',
    isPublic: true
});
const loadApps = async () => {
    loading.value = true;
    err.value = '';
    try {
        const res = await api.get('/kb/app/list');
        appList.value = res.data || [];
        for (const app of appList.value) {
            try {
                const groupsRes = await api.get(`/kb/app/${app.id}/groups`);
                const groups = groupsRes.data || [];
                app.qqGroups = groups.filter((g) => g.platform === 'qq').length;
                app.wxGroups = groups.filter((g) => g.platform === 'wx' || g.platform === 'wecom').length;
                app.boundGroupsCount = groups.length;
            }
            catch {
                app.qqGroups = 0;
                app.wxGroups = 0;
                app.boundGroupsCount = 0;
            }
        }
    }
    catch (e) {
        err.value = e.response?.data?.message || '加载失败';
    }
    finally {
        loading.value = false;
    }
};
const loadCategories = async () => {
    try {
        const res = await api.get('/kb/category/tree');
        const flatList = [];
        const flatten = (items) => {
            items.forEach(item => {
                flatList.push({ id: item.id, name: item.name, kbCount: item.kbCount || 0 });
                if (item.children?.length)
                    flatten(item.children);
            });
        };
        flatten(res.data || []);
        categoryList.value = flatList;
    }
    catch (e) {
        console.error('加载分类失败', e);
    }
};
const loadKbList = async () => {
    try {
        const res = await api.get('/kb/page', { params: { pageNum: 1, pageSize: 100 } });
        kbList.value = res.data.records || [];
    }
    catch (e) {
        console.error('加载知识库失败', e);
    }
};
const getCategoryName = (id) => {
    if (!id)
        return '未关联';
    const cat = categoryList.value.find(c => c.id === id);
    return cat?.name || '未知';
};
const getCategoryKbCount = (id) => {
    if (!id)
        return 0;
    const cat = categoryList.value.find(c => c.id === id);
    return cat?.kbCount || 0;
};
const getCategoryKbList = (id) => {
    return kbList.value.filter(kb => kb.categoryId === id);
};
const openCreateModal = () => {
    editingApp.value = null;
    verifiedInfo.value = null;
    verifyError.value = '';
    form.value = { name: '', description: '', icon: '', difyApiKey: '', categoryId: '', isPublic: true };
    showCreateModal.value = true;
};
const verifyApiKey = async () => {
    if (!form.value.difyApiKey) {
        verifyError.value = '请输入API Key';
        return;
    }
    verifyError.value = '';
    try {
        const res = await api.post('/kb/app/verify', { apiKey: form.value.difyApiKey });
        verifiedInfo.value = {
            difyAppName: res.data.difyAppName || '验证成功',
            difyAppMode: res.data.difyAppMode || 'unknown'
        };
    }
    catch (e) {
        verifyError.value = e.response?.data?.message || 'API Key验证失败';
        verifiedInfo.value = null;
    }
};
const verifyApp = async (app) => {
    try {
        await api.get(`/kb/app/${app.id}/info`);
        loadApps();
    }
    catch (e) {
        err.value = e.response?.data?.message || '验证失败';
    }
};
const showBoundGroups = async (app) => {
    selectedApp.value = app;
    try {
        const res = await api.get(`/kb/app/${app.id}/groups`);
        boundGroups.value = res.data || [];
        groupsTab.value = 'qq';
        showGroupsModal.value = true;
    }
    catch (e) {
        err.value = e.response?.data?.message || '加载群组失败';
    }
};
const showCategoryKb = (app) => {
    if (!app.categoryId)
        return;
    selectedApp.value = app;
    const cat = categoryList.value.find(c => c.id === app.categoryId);
    selectedCategory.value = cat || null;
    categoryKbList.value = getCategoryKbList(app.categoryId);
    showKbModal.value = true;
};
const setDefaultApp = async (app) => {
    try {
        await api.put(`/kb/app/${app.id}/default`);
        loadApps();
    }
    catch (e) {
        err.value = e.response?.data?.message || '设置失败';
    }
};
const editApp = (app) => {
    editingApp.value = app;
    form.value = {
        name: app.name,
        description: app.description || '',
        icon: app.icon || '',
        difyApiKey: app.difyApiKey,
        categoryId: app.categoryId ? String(app.categoryId) : '',
        isPublic: app.isPublic
    };
    verifiedInfo.value = app.difyAppName ? {
        difyAppName: app.difyAppName,
        difyAppMode: app.difyAppMode || ''
    } : null;
    verifyError.value = '';
    showCreateModal.value = true;
};
const deleteApp = async (app) => {
    if (!confirm(`确定删除应用 "${app.name}"？\n已绑定的群组将解除绑定。`))
        return;
    try {
        await api.delete(`/kb/app/${app.id}`);
        loadApps();
    }
    catch (e) {
        err.value = e.response?.data?.message || '删除失败';
    }
};
const saveApp = async () => {
    if (!form.value.name || !form.value.difyApiKey) {
        err.value = '请填写必填项';
        return;
    }
    saving.value = true;
    try {
        const payload = {
            name: form.value.name,
            description: form.value.description,
            icon: form.value.icon,
            difyApiKey: form.value.difyApiKey,
            categoryId: form.value.categoryId ? Number(form.value.categoryId) : null,
            isPublic: form.value.isPublic
        };
        if (editingApp.value) {
            await api.put('/kb/app', { ...payload, id: editingApp.value.id });
        }
        else {
            await api.post('/kb/app', payload);
        }
        closeModal();
        loadApps();
    }
    catch (e) {
        err.value = e.response?.data?.message || '保存失败';
    }
    finally {
        saving.value = false;
    }
};
const closeModal = () => {
    showCreateModal.value = false;
    editingApp.value = null;
    verifiedInfo.value = null;
    verifyError.value = '';
    form.value = { name: '', description: '', icon: '', difyApiKey: '', categoryId: '', isPublic: true };
    err.value = '';
};
onMounted(() => {
    loadApps();
    loadCategories();
    loadKbList();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['dify-link']} */ ;
/** @type {__VLS_StyleScopedClasses['step-item']} */ ;
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
    ...{ onClick: (__VLS_ctx.openCreateModal) },
    ...{ class: "anime-btn primary" },
});
const __VLS_0 = {}.Plus;
/** @type {[typeof __VLS_components.Plus, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    size: (18),
}));
const __VLS_2 = __VLS_1({
    size: (18),
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
else if (__VLS_ctx.appList.length === 0) {
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
    for (const [app] of __VLS_getVForSourceType((__VLS_ctx.appList))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            key: (app.id),
            ...{ class: "anime-card app-card" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "app-header" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "app-title-row" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "app-icon" },
        });
        (app.icon || '🤖');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "app-name" },
        });
        (app.name);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "app-badges" },
        });
        if (app.isDefault) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-badge yellow" },
            });
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-badge" },
            ...{ class: (app.isPublic ? 'green' : 'gray') },
        });
        (app.isPublic ? '公开' : '私有');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-badge" },
            ...{ class: (app.status ? 'green' : 'pink') },
        });
        (app.status ? '启用' : '禁用');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "app-desc" },
        });
        (app.description || '无描述');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "app-info-section" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "info-row" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "info-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-code" },
        });
        (app.difyAppName || '未验证');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-badge blue" },
        });
        (app.difyAppMode || '-');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.a, __VLS_intrinsicElements.a)({
            href: (__VLS_ctx.difyConsoleUrl),
            target: "_blank",
            ...{ class: "dify-link" },
        });
        const __VLS_4 = {}.ExternalLink;
        /** @type {[typeof __VLS_components.ExternalLink, ]} */ ;
        // @ts-ignore
        const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
            size: (14),
        }));
        const __VLS_6 = __VLS_5({
            size: (14),
        }, ...__VLS_functionalComponentArgsRest(__VLS_5));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "info-row" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "info-label" },
        });
        if (app.categoryId) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "info-value" },
            });
            (__VLS_ctx.getCategoryName(app.categoryId));
        }
        else {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "info-muted" },
            });
        }
        if (app.categoryId) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-badge purple" },
            });
            (__VLS_ctx.getCategoryKbCount(app.categoryId));
        }
        if (app.categoryId && __VLS_ctx.getCategoryKbList(app.categoryId).length > 0) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!!(__VLS_ctx.loading))
                            return;
                        if (!!(__VLS_ctx.appList.length === 0))
                            return;
                        if (!(app.categoryId && __VLS_ctx.getCategoryKbList(app.categoryId).length > 0))
                            return;
                        __VLS_ctx.showCategoryKb(app);
                    } },
                ...{ class: "anime-btn ghost sm" },
            });
            const __VLS_8 = {}.BookOpen;
            /** @type {[typeof __VLS_components.BookOpen, ]} */ ;
            // @ts-ignore
            const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
                size: (14),
            }));
            const __VLS_10 = __VLS_9({
                size: (14),
            }, ...__VLS_functionalComponentArgsRest(__VLS_9));
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "info-row" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "info-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-badge" },
            ...{ class: ((app.qqGroups || 0) > 0 ? 'green' : 'gray') },
        });
        (app.qqGroups || 0);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-badge" },
            ...{ class: ((app.wxGroups || 0) > 0 ? 'blue' : 'gray') },
        });
        (app.wxGroups || 0);
        if ((app.qqGroups || 0) > 0 || (app.wxGroups || 0) > 0) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!!(__VLS_ctx.loading))
                            return;
                        if (!!(__VLS_ctx.appList.length === 0))
                            return;
                        if (!((app.qqGroups || 0) > 0 || (app.wxGroups || 0) > 0))
                            return;
                        __VLS_ctx.showBoundGroups(app);
                    } },
                ...{ class: "anime-btn ghost sm" },
            });
            const __VLS_12 = {}.Users;
            /** @type {[typeof __VLS_components.Users, ]} */ ;
            // @ts-ignore
            const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
                size: (14),
            }));
            const __VLS_14 = __VLS_13({
                size: (14),
            }, ...__VLS_functionalComponentArgsRest(__VLS_13));
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "info-row" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "info-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "info-value" },
        });
        (app.createBy);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "app-actions" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!!(__VLS_ctx.loading))
                        return;
                    if (!!(__VLS_ctx.appList.length === 0))
                        return;
                    __VLS_ctx.verifyApp(app);
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
        if (!app.isDefault) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!!(__VLS_ctx.loading))
                            return;
                        if (!!(__VLS_ctx.appList.length === 0))
                            return;
                        if (!(!app.isDefault))
                            return;
                        __VLS_ctx.setDefaultApp(app);
                    } },
                ...{ class: "anime-btn primary" },
            });
            const __VLS_20 = {}.Star;
            /** @type {[typeof __VLS_components.Star, ]} */ ;
            // @ts-ignore
            const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
                size: (16),
            }));
            const __VLS_22 = __VLS_21({
                size: (16),
            }, ...__VLS_functionalComponentArgsRest(__VLS_21));
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!!(__VLS_ctx.loading))
                        return;
                    if (!!(__VLS_ctx.appList.length === 0))
                        return;
                    __VLS_ctx.editApp(app);
                } },
            ...{ class: "anime-btn ghost" },
        });
        const __VLS_24 = {}.Edit3;
        /** @type {[typeof __VLS_components.Edit3, ]} */ ;
        // @ts-ignore
        const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
            size: (16),
        }));
        const __VLS_26 = __VLS_25({
            size: (16),
        }, ...__VLS_functionalComponentArgsRest(__VLS_25));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        if (!app.isDefault) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!!(__VLS_ctx.loading))
                            return;
                        if (!!(__VLS_ctx.appList.length === 0))
                            return;
                        if (!(!app.isDefault))
                            return;
                        __VLS_ctx.deleteApp(app);
                    } },
                ...{ class: "anime-btn ghost danger" },
            });
            const __VLS_28 = {}.Trash2;
            /** @type {[typeof __VLS_components.Trash2, ]} */ ;
            // @ts-ignore
            const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
                size: (16),
            }));
            const __VLS_30 = __VLS_29({
                size: (16),
            }, ...__VLS_functionalComponentArgsRest(__VLS_29));
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        }
    }
}
if (__VLS_ctx.showCreateModal) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onClick: (__VLS_ctx.closeModal) },
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
    (__VLS_ctx.editingApp ? '编辑应用' : '创建应用');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.closeModal) },
        ...{ class: "anime-modal-close" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-body" },
    });
    if (!__VLS_ctx.editingApp) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "create-steps" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "step-item" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "step-num" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "step-content" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "step-title" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "step-desc" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.a, __VLS_intrinsicElements.a)({
            href: (__VLS_ctx.difyConsoleUrl),
            target: "_blank",
            ...{ class: "anime-btn blue step-btn" },
        });
        const __VLS_32 = {}.ExternalLink;
        /** @type {[typeof __VLS_components.ExternalLink, ]} */ ;
        // @ts-ignore
        const __VLS_33 = __VLS_asFunctionalComponent(__VLS_32, new __VLS_32({
            size: (16),
        }));
        const __VLS_34 = __VLS_33({
            size: (16),
        }, ...__VLS_functionalComponentArgsRest(__VLS_33));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "step-item" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "step-num" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "step-content" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "step-title" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "step-desc" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "step-hint" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.img)({
            src: "",
            alt: "API Key位置示意",
            ...{ class: "step-image-placeholder" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "step-item" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "step-num" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "step-content" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "step-title" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "step-desc" },
        });
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        ...{ class: "anime-input" },
        placeholder: "如：医疗助手、群聊助手",
    });
    (__VLS_ctx.form.name);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
        value: (__VLS_ctx.form.description),
        ...{ class: "anime-input" },
        rows: "2",
        placeholder: "应用用途说明",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        ...{ class: "anime-input" },
        placeholder: "如：🏥、💬、🤖、📚",
    });
    (__VLS_ctx.form.icon);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        ...{ class: "anime-input" },
        placeholder: "从Dify控制台获取，格式：app-xxxxx",
    });
    (__VLS_ctx.form.difyApiKey);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "api-key-actions" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.verifyApiKey) },
        ...{ class: "anime-btn ghost" },
    });
    const __VLS_36 = {}.RefreshCw;
    /** @type {[typeof __VLS_components.RefreshCw, ]} */ ;
    // @ts-ignore
    const __VLS_37 = __VLS_asFunctionalComponent(__VLS_36, new __VLS_36({
        size: (16),
    }));
    const __VLS_38 = __VLS_37({
        size: (16),
    }, ...__VLS_functionalComponentArgsRest(__VLS_37));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.a, __VLS_intrinsicElements.a)({
        href: (__VLS_ctx.difyConsoleUrl),
        target: "_blank",
        ...{ class: "anime-btn blue" },
    });
    const __VLS_40 = {}.ExternalLink;
    /** @type {[typeof __VLS_components.ExternalLink, ]} */ ;
    // @ts-ignore
    const __VLS_41 = __VLS_asFunctionalComponent(__VLS_40, new __VLS_40({
        size: (16),
    }));
    const __VLS_42 = __VLS_41({
        size: (16),
    }, ...__VLS_functionalComponentArgsRest(__VLS_41));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    if (__VLS_ctx.verifiedInfo) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "verified-info" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "verified-success" },
        });
        const __VLS_44 = {}.CheckCircle;
        /** @type {[typeof __VLS_components.CheckCircle, ]} */ ;
        // @ts-ignore
        const __VLS_45 = __VLS_asFunctionalComponent(__VLS_44, new __VLS_44({
            size: (16),
        }));
        const __VLS_46 = __VLS_45({
            size: (16),
        }, ...__VLS_functionalComponentArgsRest(__VLS_45));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
        (__VLS_ctx.verifiedInfo.difyAppName);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
        (__VLS_ctx.verifiedInfo.difyAppMode);
    }
    if (__VLS_ctx.verifyError) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "verified-error" },
        });
        const __VLS_48 = {}.XCircle;
        /** @type {[typeof __VLS_components.XCircle, ]} */ ;
        // @ts-ignore
        const __VLS_49 = __VLS_asFunctionalComponent(__VLS_48, new __VLS_48({
            size: (16),
        }));
        const __VLS_50 = __VLS_49({
            size: (16),
        }, ...__VLS_functionalComponentArgsRest(__VLS_49));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.verifyError);
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
        value: (__VLS_ctx.form.categoryId),
        ...{ class: "anime-input" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        value: "",
    });
    for (const [cat] of __VLS_getVForSourceType((__VLS_ctx.categoryList))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
            key: (cat.id),
            value: (cat.id),
        });
        (cat.name);
        (cat.kbCount || 0);
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "form-hint" },
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
    (__VLS_ctx.form.isPublic);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-footer" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.closeModal) },
        ...{ class: "anime-btn ghost" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.saveApp) },
        ...{ class: "anime-btn primary" },
        disabled: (__VLS_ctx.saving || !__VLS_ctx.form.difyApiKey),
    });
    if (__VLS_ctx.saving) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    }
    else {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.editingApp ? '更新' : '创建');
    }
}
if (__VLS_ctx.showGroupsModal) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showGroupsModal))
                    return;
                __VLS_ctx.showGroupsModal = false;
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
    (__VLS_ctx.selectedApp?.name);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showGroupsModal))
                    return;
                __VLS_ctx.showGroupsModal = false;
            } },
        ...{ class: "anime-modal-close" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-body" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "groups-tabs" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showGroupsModal))
                    return;
                __VLS_ctx.groupsTab = 'qq';
            } },
        ...{ class: "anime-tab" },
        ...{ class: ({ active: __VLS_ctx.groupsTab === 'qq' }) },
    });
    (__VLS_ctx.qqGroups.length);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showGroupsModal))
                    return;
                __VLS_ctx.groupsTab = 'wx';
            } },
        ...{ class: "anime-tab" },
        ...{ class: ({ active: __VLS_ctx.groupsTab === 'wx' }) },
    });
    (__VLS_ctx.wxGroups.length);
    if (__VLS_ctx.groupsTab === 'qq') {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "groups-list" },
        });
        if (__VLS_ctx.qqGroups.length === 0) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "anime-empty" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-empty-text" },
            });
        }
        else {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
            for (const [g] of __VLS_getVForSourceType((__VLS_ctx.qqGroups))) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                    key: (g.id),
                    ...{ class: "group-item" },
                });
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "anime-badge green" },
                });
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "group-name" },
                });
                (g.groupName || g.groupId);
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "group-members" },
                });
                (g.memberCount || 0);
                const __VLS_52 = {}.RouterLink;
                /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
                // @ts-ignore
                const __VLS_53 = __VLS_asFunctionalComponent(__VLS_52, new __VLS_52({
                    to: "/console/im",
                    ...{ class: "anime-btn ghost sm" },
                }));
                const __VLS_54 = __VLS_53({
                    to: "/console/im",
                    ...{ class: "anime-btn ghost sm" },
                }, ...__VLS_functionalComponentArgsRest(__VLS_53));
                __VLS_55.slots.default;
                var __VLS_55;
            }
        }
    }
    if (__VLS_ctx.groupsTab === 'wx') {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "groups-list" },
        });
        if (__VLS_ctx.wxGroups.length === 0) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "anime-empty" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-empty-text" },
            });
        }
        else {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
            for (const [g] of __VLS_getVForSourceType((__VLS_ctx.wxGroups))) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                    key: (g.id),
                    ...{ class: "group-item" },
                });
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "anime-badge blue" },
                });
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "group-name" },
                });
                (g.groupName || g.groupId);
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "group-members" },
                });
                (g.memberCount || 0);
                const __VLS_56 = {}.RouterLink;
                /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
                // @ts-ignore
                const __VLS_57 = __VLS_asFunctionalComponent(__VLS_56, new __VLS_56({
                    to: "/console/im",
                    ...{ class: "anime-btn ghost sm" },
                }));
                const __VLS_58 = __VLS_57({
                    to: "/console/im",
                    ...{ class: "anime-btn ghost sm" },
                }, ...__VLS_functionalComponentArgsRest(__VLS_57));
                __VLS_59.slots.default;
                var __VLS_59;
            }
        }
    }
}
if (__VLS_ctx.showKbModal) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showKbModal))
                    return;
                __VLS_ctx.showKbModal = false;
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
    (__VLS_ctx.selectedApp?.name);
    (__VLS_ctx.selectedCategory?.name);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showKbModal))
                    return;
                __VLS_ctx.showKbModal = false;
            } },
        ...{ class: "anime-modal-close" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-body" },
    });
    if (__VLS_ctx.categoryKbList.length === 0) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "anime-empty" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-empty-text" },
        });
    }
    else {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "kb-list-modal" },
        });
        for (const [kb] of __VLS_getVForSourceType((__VLS_ctx.categoryKbList))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                key: (kb.id),
                ...{ class: "kb-item" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "kb-title" },
            });
            (kb.name);
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-badge blue" },
            });
            (kb.docCount || 0);
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-badge" },
                ...{ class: (kb.status ? 'green' : 'pink') },
            });
            (kb.status ? '启用' : '禁用');
        }
    }
}
/** @type {__VLS_StyleScopedClasses['anime-page-shell']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-header']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-title']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-desc']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-error']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-body']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-loader-spinner']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card']} */ ;
/** @type {__VLS_StyleScopedClasses['app-card']} */ ;
/** @type {__VLS_StyleScopedClasses['app-header']} */ ;
/** @type {__VLS_StyleScopedClasses['app-title-row']} */ ;
/** @type {__VLS_StyleScopedClasses['app-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['app-name']} */ ;
/** @type {__VLS_StyleScopedClasses['app-badges']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['yellow']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['app-desc']} */ ;
/** @type {__VLS_StyleScopedClasses['app-info-section']} */ ;
/** @type {__VLS_StyleScopedClasses['info-row']} */ ;
/** @type {__VLS_StyleScopedClasses['info-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-code']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['blue']} */ ;
/** @type {__VLS_StyleScopedClasses['dify-link']} */ ;
/** @type {__VLS_StyleScopedClasses['info-row']} */ ;
/** @type {__VLS_StyleScopedClasses['info-label']} */ ;
/** @type {__VLS_StyleScopedClasses['info-value']} */ ;
/** @type {__VLS_StyleScopedClasses['info-muted']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['purple']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['info-row']} */ ;
/** @type {__VLS_StyleScopedClasses['info-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['info-row']} */ ;
/** @type {__VLS_StyleScopedClasses['info-label']} */ ;
/** @type {__VLS_StyleScopedClasses['info-value']} */ ;
/** @type {__VLS_StyleScopedClasses['app-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['blue']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['danger']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-overlay']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-header']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-close']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-body']} */ ;
/** @type {__VLS_StyleScopedClasses['create-steps']} */ ;
/** @type {__VLS_StyleScopedClasses['step-item']} */ ;
/** @type {__VLS_StyleScopedClasses['step-num']} */ ;
/** @type {__VLS_StyleScopedClasses['step-content']} */ ;
/** @type {__VLS_StyleScopedClasses['step-title']} */ ;
/** @type {__VLS_StyleScopedClasses['step-desc']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['blue']} */ ;
/** @type {__VLS_StyleScopedClasses['step-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['step-item']} */ ;
/** @type {__VLS_StyleScopedClasses['step-num']} */ ;
/** @type {__VLS_StyleScopedClasses['step-content']} */ ;
/** @type {__VLS_StyleScopedClasses['step-title']} */ ;
/** @type {__VLS_StyleScopedClasses['step-desc']} */ ;
/** @type {__VLS_StyleScopedClasses['step-hint']} */ ;
/** @type {__VLS_StyleScopedClasses['step-image-placeholder']} */ ;
/** @type {__VLS_StyleScopedClasses['step-item']} */ ;
/** @type {__VLS_StyleScopedClasses['step-num']} */ ;
/** @type {__VLS_StyleScopedClasses['step-content']} */ ;
/** @type {__VLS_StyleScopedClasses['step-title']} */ ;
/** @type {__VLS_StyleScopedClasses['step-desc']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['api-key-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['blue']} */ ;
/** @type {__VLS_StyleScopedClasses['verified-info']} */ ;
/** @type {__VLS_StyleScopedClasses['verified-success']} */ ;
/** @type {__VLS_StyleScopedClasses['verified-error']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['form-hint']} */ ;
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
/** @type {__VLS_StyleScopedClasses['groups-tabs']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tab']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-tab']} */ ;
/** @type {__VLS_StyleScopedClasses['groups-list']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['group-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['green']} */ ;
/** @type {__VLS_StyleScopedClasses['group-name']} */ ;
/** @type {__VLS_StyleScopedClasses['group-members']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['groups-list']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['group-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['blue']} */ ;
/** @type {__VLS_StyleScopedClasses['group-name']} */ ;
/** @type {__VLS_StyleScopedClasses['group-members']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-overlay']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-header']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-close']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-body']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-list-modal']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-item']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-title']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['blue']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            RouterLink: RouterLink,
            Plus: Plus,
            RefreshCw: RefreshCw,
            Edit3: Edit3,
            Trash2: Trash2,
            Star: Star,
            Users: Users,
            BookOpen: BookOpen,
            ExternalLink: ExternalLink,
            CheckCircle: CheckCircle,
            XCircle: XCircle,
            difyConsoleUrl: difyConsoleUrl,
            appList: appList,
            categoryList: categoryList,
            loading: loading,
            err: err,
            showCreateModal: showCreateModal,
            showGroupsModal: showGroupsModal,
            showKbModal: showKbModal,
            editingApp: editingApp,
            selectedApp: selectedApp,
            selectedCategory: selectedCategory,
            categoryKbList: categoryKbList,
            verifiedInfo: verifiedInfo,
            verifyError: verifyError,
            saving: saving,
            groupsTab: groupsTab,
            qqGroups: qqGroups,
            wxGroups: wxGroups,
            form: form,
            getCategoryName: getCategoryName,
            getCategoryKbCount: getCategoryKbCount,
            getCategoryKbList: getCategoryKbList,
            openCreateModal: openCreateModal,
            verifyApiKey: verifyApiKey,
            verifyApp: verifyApp,
            showBoundGroups: showBoundGroups,
            showCategoryKb: showCategoryKb,
            setDefaultApp: setDefaultApp,
            editApp: editApp,
            deleteApp: deleteApp,
            saveApp: saveApp,
            closeModal: closeModal,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
