/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref, computed, onMounted } from 'vue';
import { RouterLink } from 'vue-router';
import { Plus, Trash2, Paperclip, Link, Send, ThumbsUp, ThumbsDown, MessageSquare, ChevronDown, ChevronUp, Brain } from 'lucide-vue-next';
import { webChatWithSession } from '../api/chat';
import { submitFeedback as submitFeedbackApi, getFeedbackStatus } from '../api/feedback';
import { getOrCreateUserId } from '../lib/user';
import { uploadFile } from '../api/upload';
import { createSession, listSessions, getSessionMessages, deleteSession as deleteSessionApi } from '../api/session';
import { renderMessage } from '../lib/markdown';
import { api } from '../api/client';
const userId = getOrCreateUserId();
const input = ref('');
const urlInput = ref('');
const loading = ref(false);
const error = ref(null);
const sessions = ref([]);
const currentSession = ref(null);
const messages = ref([]);
const pendingFiles = ref([]);
const showAttachMenu = ref(false);
const showUrlInput = ref(false);
const fileInputRef = ref(null);
const appList = ref([]);
const selectedAppId = ref(null);
function truncate(s, max) { return s && s.length > max ? `${s.slice(0, max)}...` : s; }
function getThinkingHtml(text) {
    const { thinkingHtml } = renderMessage(text);
    return thinkingHtml;
}
function getContentHtml(text, role) {
    if (role === 'user') {
        return `<p style="white-space: pre-wrap; line-height: 1.7;">${text}</p>`;
    }
    const { contentHtml } = renderMessage(text);
    return contentHtml;
}
function toggleThinking(idx) {
    messages.value[idx].showThinking = !messages.value[idx].showThinking;
}
function getDateKey(t) {
    if (!t)
        return 'unknown';
    const d = new Date(t);
    return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`;
}
function formatDateTitle(t) {
    if (!t)
        return '未知日期';
    const d = new Date(t);
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);
    if (d.getFullYear() === today.getFullYear() && d.getMonth() === today.getMonth() && d.getDate() === today.getDate()) {
        return '今天';
    }
    if (d.getFullYear() === yesterday.getFullYear() && d.getMonth() === yesterday.getMonth() && d.getDate() === yesterday.getDate()) {
        return '昨天';
    }
    return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()}`;
}
function formatTime(t) {
    if (!t)
        return '';
    const d = new Date(t);
    const now = new Date();
    const diff = now.getTime() - d.getTime();
    if (diff < 60000)
        return '刚刚';
    if (diff < 3600000)
        return `${Math.floor(diff / 60000)}分钟前`;
    if (diff < 86400000)
        return `${Math.floor(diff / 3600000)}小时前`;
    return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
}
const groupedSessions = computed(() => {
    const groups = [];
    const groupMap = new Map();
    for (const s of sessions.value) {
        const key = getDateKey(s.lastMessageTime || s.createTime);
        if (!groupMap.has(key)) {
            groupMap.set(key, []);
        }
        groupMap.get(key).push(s);
    }
    const sortedKeys = Array.from(groupMap.keys()).sort((a, b) => {
        const [ay, am, ad] = a.split('-').map(Number);
        const [by, bm, bd] = b.split('-').map(Number);
        return (by * 10000 + bm * 100 + bd) - (ay * 10000 + am * 100 + ad);
    });
    for (const key of sortedKeys) {
        const sessionList = groupMap.get(key);
        const sampleTime = sessionList[0]?.lastMessageTime || sessionList[0]?.createTime;
        groups.push({
            dateKey: key,
            dateTitle: formatDateTitle(sampleTime),
            sessions: sessionList
        });
    }
    return groups;
});
async function loadSessions() {
    try {
        const resp = await listSessions(userId, 'web', 1, 50);
        sessions.value = resp.records || [];
        if (sessions.value.length > 0 && !currentSession.value) {
            await switchSession(sessions.value[0]);
        }
    }
    catch (e) {
        error.value = e?.message || '加载会话列表失败';
    }
}
async function loadApps() {
    try {
        const resp = await api.get('/kb/app/list');
        appList.value = resp.data || [];
        const defaultApp = appList.value.find(a => a.isDefault);
        selectedAppId.value = defaultApp?.id || (appList.value.length > 0 ? appList.value[0].id : null);
    }
    catch (e) {
        console.error('加载应用列表失败', e);
    }
}
async function createNewSession() {
    try {
        const session = await createSession(userId, 'web');
        sessions.value.unshift(session);
        await switchSession(session);
    }
    catch (e) {
        error.value = e?.message || '创建会话失败';
    }
}
async function switchSession(session) {
    currentSession.value = session;
    messages.value = [];
    error.value = null;
    pendingFiles.value = [];
    try {
        const msgs = await getSessionMessages(session.sessionId);
        const feedbackMap = await getFeedbackStatus(session.sessionId);
        for (const m of msgs) {
            if (m.query) {
                messages.value.push({ role: 'user', text: m.query });
            }
            if (m.answer) {
                const idx = messages.value.length;
                const feedback = feedbackMap[idx] !== undefined ? feedbackMap[idx] : undefined;
                const { thinkingHtml, contentHtml } = renderMessage(m.answer);
                messages.value.push({
                    role: 'assistant',
                    text: m.answer,
                    feedback,
                    thinkingHtml,
                    contentHtml,
                    showThinking: false
                });
            }
        }
    }
    catch (e) {
        error.value = e?.message || '加载消息失败';
    }
}
async function deleteSession(session) {
    if (!confirm('确定删除此对话吗？'))
        return;
    try {
        await deleteSessionApi(session.sessionId);
        sessions.value = sessions.value.filter(s => s.sessionId !== session.sessionId);
        if (currentSession.value?.sessionId === session.sessionId) {
            if (sessions.value.length > 0) {
                await switchSession(sessions.value[0]);
            }
            else {
                currentSession.value = null;
                messages.value = [];
            }
        }
    }
    catch (e) {
        error.value = e?.message || '删除失败';
    }
}
function toggleAttachMenu() {
    showAttachMenu.value = !showAttachMenu.value;
    if (showUrlInput.value)
        showUrlInput.value = false;
}
function pickLocalFile() {
    showAttachMenu.value = false;
    fileInputRef.value?.click();
}
async function handleFileUpload(e) {
    const el = e.target;
    const file = el.files?.[0];
    if (!file)
        return;
    loading.value = true;
    error.value = null;
    try {
        const resp = await uploadFile(file, userId);
        if (resp.id) {
            pendingFiles.value.push({ type: guessFileType(file.name), transfer_method: 'local_file', upload_file_id: resp.id });
        }
    }
    catch (err) {
        error.value = err?.message || '上传失败';
    }
    finally {
        loading.value = false;
        el.value = '';
    }
}
function addUrlFile() {
    const u = urlInput.value.trim();
    if (!u)
        return;
    pendingFiles.value.push({ type: guessFileType(u), transfer_method: 'remote_url', url: u });
    urlInput.value = '';
    showUrlInput.value = false;
}
function guessFileType(name) {
    const n = name.toLowerCase();
    if (/\.(png|jpg|jpeg|gif|webp|bmp)$/.test(n))
        return 'image';
    if (/\.(mp3|wav|m4a|aac|ogg)$/.test(n))
        return 'audio';
    if (/\.(mp4|mov|mkv|webm)$/.test(n))
        return 'video';
    return 'document';
}
async function send() {
    const text = input.value.trim();
    if (!text || loading.value)
        return;
    if (!currentSession.value) {
        await createNewSession();
        if (!currentSession.value)
            return;
    }
    error.value = null;
    messages.value.push({ role: 'user', text });
    input.value = '';
    loading.value = true;
    try {
        const files = [...pendingFiles.value];
        const resp = await webChatWithSession(currentSession.value.sessionId, text, userId, files, selectedAppId.value ?? undefined);
        const { thinkingHtml, contentHtml } = renderMessage(resp.answer || '（无返回）');
        messages.value.push({
            role: 'assistant',
            text: resp.answer || '（无返回）',
            sources: resp.retrieverResources || [],
            thinkingHtml,
            contentHtml,
            showThinking: false
        });
        pendingFiles.value = [];
        currentSession.value.messageCount = (currentSession.value.messageCount || 0) + 2;
        currentSession.value.lastMessageTime = new Date().toISOString();
        if (!currentSession.value.title) {
            currentSession.value.title = text.length > 50 ? text.substring(0, 50) + '...' : text;
        }
    }
    catch (e) {
        error.value = e?.message || '请求失败';
        messages.value.push({ role: 'assistant', text: '【系统错误】请求失败，请稍后再试' });
    }
    finally {
        loading.value = false;
    }
}
async function submitFeedback(msgIdx, rating) {
    if (!currentSession.value)
        return;
    try {
        const result = await submitFeedbackApi(currentSession.value.sessionId, msgIdx, rating);
        if (result.success) {
            messages.value[msgIdx].feedback = rating;
        }
        else {
            error.value = result.message;
        }
    }
    catch {
        error.value = '反馈提交失败';
    }
}
onMounted(() => {
    loadSessions();
    loadApps();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['anime-chat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-session-date']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-session-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-session-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-session-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-session-delete']} */ ;
/** @type {__VLS_StyleScopedClasses['thinking-toggle']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-app-select']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-page-shell" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "anime-card anime-chat-card" },
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
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.selectedAppId),
    ...{ class: "anime-app-select" },
    disabled: (__VLS_ctx.loading),
});
for (const [app] of __VLS_getVForSourceType((__VLS_ctx.appList))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        key: (app.id),
        value: (app.id),
    });
    (app.name);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.createNewSession) },
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
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-chat-layout" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
    ...{ class: "anime-chat-sidebar" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-session-list" },
});
if (__VLS_ctx.sessions.length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-empty" },
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-empty-icon" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-empty-text" },
    });
}
for (const [group] of __VLS_getVForSourceType((__VLS_ctx.groupedSessions))) {
    (group.dateKey);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-session-date" },
    });
    (group.dateTitle);
    for (const [s] of __VLS_getVForSourceType((group.sessions))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ onClick: (...[$event]) => {
                    __VLS_ctx.switchSession(s);
                } },
            key: (s.sessionId),
            ...{ class: "anime-session-item" },
            ...{ class: ({ active: __VLS_ctx.currentSession?.sessionId === s.sessionId }) },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "anime-session-title" },
        });
        (s.title || '新对话');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "anime-session-meta" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (s.messageCount || 0);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.formatTime(s.lastMessageTime));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    __VLS_ctx.deleteSession(s);
                } },
            ...{ class: "anime-session-delete" },
        });
        const __VLS_4 = {}.Trash2;
        /** @type {[typeof __VLS_components.Trash2, ]} */ ;
        // @ts-ignore
        const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
            size: (14),
        }));
        const __VLS_6 = __VLS_5({
            size: (14),
        }, ...__VLS_functionalComponentArgsRest(__VLS_5));
    }
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-chat-main" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-chat-messages" },
});
if (__VLS_ctx.messages.length === 0) {
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
for (const [m, idx] of __VLS_getVForSourceType((__VLS_ctx.messages))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        key: (idx),
        ...{ class: "anime-chat-message anime-fade-in" },
        ...{ class: (m.role) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-chat-avatar" },
    });
    (m.role === 'user' ? 'U' : 'AI');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-chat-bubble" },
    });
    if (m.role === 'assistant' && (m.thinkingHtml || __VLS_ctx.getThinkingHtml(m.text))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "thinking-section" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(m.role === 'assistant' && (m.thinkingHtml || __VLS_ctx.getThinkingHtml(m.text))))
                        return;
                    __VLS_ctx.toggleThinking(idx);
                } },
            ...{ class: "thinking-toggle" },
        });
        const __VLS_8 = {}.Brain;
        /** @type {[typeof __VLS_components.Brain, ]} */ ;
        // @ts-ignore
        const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
            size: (14),
        }));
        const __VLS_10 = __VLS_9({
            size: (14),
        }, ...__VLS_functionalComponentArgsRest(__VLS_9));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        if (!m.showThinking) {
            const __VLS_12 = {}.ChevronDown;
            /** @type {[typeof __VLS_components.ChevronDown, ]} */ ;
            // @ts-ignore
            const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
                size: (14),
            }));
            const __VLS_14 = __VLS_13({
                size: (14),
            }, ...__VLS_functionalComponentArgsRest(__VLS_13));
        }
        else {
            const __VLS_16 = {}.ChevronUp;
            /** @type {[typeof __VLS_components.ChevronUp, ]} */ ;
            // @ts-ignore
            const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
                size: (14),
            }));
            const __VLS_18 = __VLS_17({
                size: (14),
            }, ...__VLS_functionalComponentArgsRest(__VLS_17));
        }
        if (m.showThinking) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "thinking-content" },
            });
            __VLS_asFunctionalDirective(__VLS_directives.vHtml)(null, { ...__VLS_directiveBindingRestFields, value: (m.thinkingHtml || __VLS_ctx.getThinkingHtml(m.text)) }, null, null);
        }
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "message-content" },
    });
    __VLS_asFunctionalDirective(__VLS_directives.vHtml)(null, { ...__VLS_directiveBindingRestFields, value: (m.contentHtml || __VLS_ctx.getContentHtml(m.text, m.role)) }, null, null);
    if (m.sources?.length) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-badge blue" },
        });
        for (const [s, i] of __VLS_getVForSourceType((m.sources))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                key: (i),
                ...{ style: {} },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ style: {} },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-badge muted" },
            });
            (s.datasetName || s.datasetId || 'KB');
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-badge pink" },
            });
            (s.documentName || s.documentId || 'DOC');
            if (typeof s.score === 'number') {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "anime-code" },
                });
                (s.score.toFixed(3));
            }
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ style: {} },
            });
            (s.content);
        }
    }
    if (m.role === 'assistant' && m.feedback === undefined) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(m.role === 'assistant' && m.feedback === undefined))
                        return;
                    __VLS_ctx.submitFeedback(idx, 1);
                } },
            ...{ class: "anime-btn ghost" },
            ...{ style: {} },
        });
        const __VLS_20 = {}.ThumbsUp;
        /** @type {[typeof __VLS_components.ThumbsUp, ]} */ ;
        // @ts-ignore
        const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
            size: (16),
        }));
        const __VLS_22 = __VLS_21({
            size: (16),
        }, ...__VLS_functionalComponentArgsRest(__VLS_21));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(m.role === 'assistant' && m.feedback === undefined))
                        return;
                    __VLS_ctx.submitFeedback(idx, 0);
                } },
            ...{ class: "anime-btn ghost" },
            ...{ style: {} },
        });
        const __VLS_24 = {}.ThumbsDown;
        /** @type {[typeof __VLS_components.ThumbsDown, ]} */ ;
        // @ts-ignore
        const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
            size: (16),
        }));
        const __VLS_26 = __VLS_25({
            size: (16),
        }, ...__VLS_functionalComponentArgsRest(__VLS_25));
        const __VLS_28 = {}.RouterLink;
        /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
        // @ts-ignore
        const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
            to: "/feedback",
            ...{ class: "anime-btn ghost" },
            ...{ style: {} },
        }));
        const __VLS_30 = __VLS_29({
            to: "/feedback",
            ...{ class: "anime-btn ghost" },
            ...{ style: {} },
        }, ...__VLS_functionalComponentArgsRest(__VLS_29));
        __VLS_31.slots.default;
        const __VLS_32 = {}.MessageSquare;
        /** @type {[typeof __VLS_components.MessageSquare, ]} */ ;
        // @ts-ignore
        const __VLS_33 = __VLS_asFunctionalComponent(__VLS_32, new __VLS_32({
            size: (16),
        }));
        const __VLS_34 = __VLS_33({
            size: (16),
        }, ...__VLS_functionalComponentArgsRest(__VLS_33));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ style: {} },
        });
        var __VLS_31;
    }
    if (m.role === 'assistant' && m.feedback !== undefined) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        if (m.feedback === 1) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-badge green" },
                ...{ style: {} },
            });
            const __VLS_36 = {}.ThumbsUp;
            /** @type {[typeof __VLS_components.ThumbsUp, ]} */ ;
            // @ts-ignore
            const __VLS_37 = __VLS_asFunctionalComponent(__VLS_36, new __VLS_36({
                size: (14),
            }));
            const __VLS_38 = __VLS_37({
                size: (14),
            }, ...__VLS_functionalComponentArgsRest(__VLS_37));
        }
        if (m.feedback === 0) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-badge pink" },
                ...{ style: {} },
            });
            const __VLS_40 = {}.ThumbsDown;
            /** @type {[typeof __VLS_components.ThumbsDown, ]} */ ;
            // @ts-ignore
            const __VLS_41 = __VLS_asFunctionalComponent(__VLS_40, new __VLS_40({
                size: (14),
            }));
            const __VLS_42 = __VLS_41({
                size: (14),
            }, ...__VLS_functionalComponentArgsRest(__VLS_41));
        }
    }
}
if (__VLS_ctx.loading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-chat-message assistant anime-fade-in" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-chat-avatar" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-chat-bubble" },
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "anime-loader-spinner" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ style: {} },
    });
}
if (__VLS_ctx.pendingFiles.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "anime-badge pink" },
    });
    (__VLS_ctx.pendingFiles.length);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    for (const [f, i] of __VLS_getVForSourceType((__VLS_ctx.pendingFiles))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            key: (i),
            ...{ class: "anime-pill" },
        });
        (f.transfer_method === 'local_file' ? `本地:${f.upload_file_id?.slice(0, 8)}...` : `链接:${__VLS_ctx.truncate(f.url || '', 20)}`);
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.pendingFiles.length))
                    return;
                __VLS_ctx.pendingFiles = [];
            } },
        ...{ class: "anime-btn ghost" },
        ...{ style: {} },
    });
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-chat-composer" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.toggleAttachMenu) },
    ...{ class: "anime-btn ghost" },
    type: "button",
});
const __VLS_44 = {}.Plus;
/** @type {[typeof __VLS_components.Plus, ]} */ ;
// @ts-ignore
const __VLS_45 = __VLS_asFunctionalComponent(__VLS_44, new __VLS_44({
    size: (18),
}));
const __VLS_46 = __VLS_45({
    size: (18),
}, ...__VLS_functionalComponentArgsRest(__VLS_45));
if (__VLS_ctx.showAttachMenu) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.pickLocalFile) },
        ...{ class: "anime-btn ghost" },
        type: "button",
    });
    const __VLS_48 = {}.Paperclip;
    /** @type {[typeof __VLS_components.Paperclip, ]} */ ;
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
                if (!(__VLS_ctx.showAttachMenu))
                    return;
                __VLS_ctx.showUrlInput = true;
                __VLS_ctx.showAttachMenu = false;
            } },
        ...{ class: "anime-btn ghost" },
        type: "button",
    });
    const __VLS_52 = {}.Link;
    /** @type {[typeof __VLS_components.Link, ]} */ ;
    // @ts-ignore
    const __VLS_53 = __VLS_asFunctionalComponent(__VLS_52, new __VLS_52({
        size: (16),
    }));
    const __VLS_54 = __VLS_53({
        size: (16),
    }, ...__VLS_functionalComponentArgsRest(__VLS_53));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ onChange: (__VLS_ctx.handleFileUpload) },
    ref: "fileInputRef",
    type: "file",
    ...{ style: {} },
    disabled: (__VLS_ctx.loading),
});
/** @type {typeof __VLS_ctx.fileInputRef} */ ;
if (__VLS_ctx.showUrlInput) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        ...{ onKeyup: (__VLS_ctx.addUrlFile) },
        ...{ class: "anime-input anime-chat-input" },
        placeholder: "输入文件URL (https://...)",
        disabled: (__VLS_ctx.loading),
    });
    (__VLS_ctx.urlInput);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.addUrlFile) },
        ...{ class: "anime-btn blue" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showUrlInput))
                    return;
                __VLS_ctx.showUrlInput = false;
                __VLS_ctx.urlInput = '';
            } },
        ...{ class: "anime-btn ghost" },
    });
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-chat-input" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        ...{ onKeyup: (__VLS_ctx.send) },
        ...{ class: "anime-input" },
        placeholder: "输入问题，按Enter发送...",
        disabled: (__VLS_ctx.loading),
    });
    (__VLS_ctx.input);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.send) },
    ...{ class: "anime-btn primary" },
    disabled: (__VLS_ctx.loading || !__VLS_ctx.input.trim()),
});
const __VLS_56 = {}.Send;
/** @type {[typeof __VLS_components.Send, ]} */ ;
// @ts-ignore
const __VLS_57 = __VLS_asFunctionalComponent(__VLS_56, new __VLS_56({
    size: (18),
}));
const __VLS_58 = __VLS_57({
    size: (18),
}, ...__VLS_functionalComponentArgsRest(__VLS_57));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.loading ? '发送中...' : '发送');
if (__VLS_ctx.error) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-error" },
        ...{ style: {} },
    });
    (__VLS_ctx.error);
}
/** @type {__VLS_StyleScopedClasses['anime-page-shell']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-chat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-header']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-title']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-desc']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-app-select']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-chat-layout']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-chat-sidebar']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-session-list']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-session-date']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-session-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-session-title']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-session-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-session-delete']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-chat-main']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-chat-messages']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-chat-message']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-fade-in']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-chat-avatar']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-chat-bubble']} */ ;
/** @type {__VLS_StyleScopedClasses['thinking-section']} */ ;
/** @type {__VLS_StyleScopedClasses['thinking-toggle']} */ ;
/** @type {__VLS_StyleScopedClasses['thinking-content']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['blue']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['pink']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-code']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['green']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['pink']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-chat-message']} */ ;
/** @type {__VLS_StyleScopedClasses['assistant']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-fade-in']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-chat-avatar']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-chat-bubble']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-loader-spinner']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['pink']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-pill']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-chat-composer']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-chat-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['blue']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-chat-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-error']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            RouterLink: RouterLink,
            Plus: Plus,
            Trash2: Trash2,
            Paperclip: Paperclip,
            Link: Link,
            Send: Send,
            ThumbsUp: ThumbsUp,
            ThumbsDown: ThumbsDown,
            MessageSquare: MessageSquare,
            ChevronDown: ChevronDown,
            ChevronUp: ChevronUp,
            Brain: Brain,
            input: input,
            urlInput: urlInput,
            loading: loading,
            error: error,
            sessions: sessions,
            currentSession: currentSession,
            messages: messages,
            pendingFiles: pendingFiles,
            showAttachMenu: showAttachMenu,
            showUrlInput: showUrlInput,
            fileInputRef: fileInputRef,
            appList: appList,
            selectedAppId: selectedAppId,
            truncate: truncate,
            getThinkingHtml: getThinkingHtml,
            getContentHtml: getContentHtml,
            toggleThinking: toggleThinking,
            formatTime: formatTime,
            groupedSessions: groupedSessions,
            createNewSession: createNewSession,
            switchSession: switchSession,
            deleteSession: deleteSession,
            toggleAttachMenu: toggleAttachMenu,
            pickLocalFile: pickLocalFile,
            handleFileUpload: handleFileUpload,
            addUrlFile: addUrlFile,
            send: send,
            submitFeedback: submitFeedback,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
