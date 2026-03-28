/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref, watch, onMounted } from 'vue';
import { RouterLink } from 'vue-router';
import { useAuth } from '../composables/useAuth';
import { webChat, listWebSessions, registerWebSession } from '../api/chat';
import { uploadFile } from '../api/upload';
import { loadLocalMessages, saveLocalMessages, loadActiveSessionId, saveActiveSessionId, } from '../lib/chatMessages';
const retentionHint = 30;
const { auth } = useAuth();
const input = ref('');
const urlInput = ref('');
const loading = ref(false);
const error = ref(null);
const messages = ref([]);
const pendingFiles = ref([]);
const menuOpen = ref(false);
const urlPanelOpen = ref(false);
const fileInputRef = ref(null);
const sessions = ref([]);
const activeSessionId = ref('');
function truncate(s, max) {
    if (!s)
        return '';
    return s.length > max ? `${s.slice(0, max)}…` : s;
}
function formatTime(ms) {
    if (!ms)
        return '';
    return new Date(ms).toLocaleString();
}
function togglePlusMenu() {
    menuOpen.value = !menuOpen.value;
}
function closePlusMenu() {
    menuOpen.value = false;
}
function pickLocalFile() {
    closePlusMenu();
    fileInputRef.value?.click();
}
function openUrlPanelFromMenu() {
    closePlusMenu();
    urlPanelOpen.value = true;
}
function confirmUrlAndClose() {
    addUrlFile();
    urlPanelOpen.value = false;
}
function persistCurrentMessages() {
    if (!activeSessionId.value)
        return;
    saveLocalMessages(auth.value.userId, activeSessionId.value, messages.value);
}
async function refreshSessions() {
    try {
        const list = await listWebSessions(auth.value.userId);
        sessions.value = [...list].sort((a, b) => b.updatedAt - a.updatedAt);
    }
    catch {
        sessions.value = [];
    }
}
async function newSession() {
    persistCurrentMessages();
    const id = crypto.randomUUID();
    activeSessionId.value = id;
    messages.value = [];
    saveActiveSessionId(auth.value.userId, id);
    try {
        await registerWebSession(auth.value.userId, id);
    }
    catch {
        /* 离线时仍允许本地聊，首条消息会再次同步 */
    }
    await refreshSessions();
    if (!sessions.value.some((s) => s.sessionId === id)) {
        sessions.value = [{ sessionId: id, title: '新对话', updatedAt: Date.now() }, ...sessions.value];
    }
}
async function selectSession(id) {
    if (id === activeSessionId.value)
        return;
    persistCurrentMessages();
    activeSessionId.value = id;
    saveActiveSessionId(auth.value.userId, id);
    messages.value = loadLocalMessages(auth.value.userId, id);
}
async function bootstrapSessions() {
    await refreshSessions();
    let sid = loadActiveSessionId(auth.value.userId);
    if (sid) {
        activeSessionId.value = sid;
        messages.value = loadLocalMessages(auth.value.userId, sid);
        if (!sessions.value.some((s) => s.sessionId === sid)) {
            sessions.value = [{ sessionId: sid, title: '新对话', updatedAt: Date.now() }, ...sessions.value];
        }
        return;
    }
    if (sessions.value.length > 0) {
        sid = sessions.value[0].sessionId;
        activeSessionId.value = sid;
        saveActiveSessionId(auth.value.userId, sid);
        messages.value = loadLocalMessages(auth.value.userId, sid);
        return;
    }
    await newSession();
}
function resetThread() {
    messages.value = [];
    error.value = null;
    input.value = '';
    urlInput.value = '';
    pendingFiles.value = [];
    menuOpen.value = false;
    urlPanelOpen.value = false;
    if (activeSessionId.value) {
        saveLocalMessages(auth.value.userId, activeSessionId.value, []);
    }
}
async function send() {
    const text = input.value.trim();
    if (!text)
        return;
    if (!activeSessionId.value)
        await newSession();
    error.value = null;
    messages.value.push({ role: 'user', text });
    input.value = '';
    loading.value = true;
    persistCurrentMessages();
    try {
        const files = [...pendingFiles.value];
        const sid = activeSessionId.value;
        const resp = await webChat(text, auth.value.userId, sid, files);
        messages.value.push({
            role: 'assistant',
            text: resp.answer || '（无返回）',
            sources: resp.retrieverResources || [],
        });
        pendingFiles.value = [];
        persistCurrentMessages();
        await refreshSessions();
    }
    catch (e) {
        error.value = e?.message || '请求失败';
        messages.value.push({ role: 'assistant', text: '【系统错误】请求失败，请稍后再试' });
        persistCurrentMessages();
    }
    finally {
        loading.value = false;
    }
}
function addUrlFile() {
    const u = urlInput.value.trim();
    if (!u)
        return;
    pendingFiles.value.push({
        type: guessFileTypeFromUrl(u),
        transferMethod: 'remote_url',
        url: u,
    });
    urlInput.value = '';
}
async function uploadLocalFile(e) {
    const el = e.target;
    const file = el.files?.[0];
    if (!file)
        return;
    loading.value = true;
    error.value = null;
    try {
        const resp = await uploadFile(file, auth.value.userId);
        if (resp.id) {
            pendingFiles.value.push({
                type: guessFileTypeFromName(file.name),
                transferMethod: 'local_file',
                uploadFileId: resp.id,
            });
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
function guessFileTypeFromName(name) {
    const n = name.toLowerCase();
    if (/\.(png|jpg|jpeg|gif|webp|bmp)$/.test(n))
        return 'image';
    if (/\.(mp3|wav|m4a|aac|ogg)$/.test(n))
        return 'audio';
    if (/\.(mp4|mov|mkv|webm)$/.test(n))
        return 'video';
    return 'document';
}
function guessFileTypeFromUrl(url) {
    try {
        const path = new URL(url).pathname;
        return guessFileTypeFromName(path);
    }
    catch {
        return 'document';
    }
}
onMounted(() => {
    bootstrapSessions();
});
watch(() => auth.value.userId, () => {
    bootstrapSessions();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "chatShell" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
    ...{ class: "sessionSidebar" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.newSession) },
    type: "button",
    ...{ class: "btn btnNewChat" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "sessionList" },
});
for (const [s] of __VLS_getVForSourceType((__VLS_ctx.sessions))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.selectSession(s.sessionId);
            } },
        key: (s.sessionId),
        type: "button",
        ...{ class: "sessionItem" },
        ...{ class: ({ active: s.sessionId === __VLS_ctx.activeSessionId }) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "sessionTitle" },
    });
    (s.title);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "sessionTime" },
    });
    (__VLS_ctx.formatTime(s.updatedAt));
}
if (__VLS_ctx.sessions.length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "sessionEmpty muted" },
    });
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "sidebarHint muted" },
});
(__VLS_ctx.retentionHint);
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "card chatCard" },
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
(__VLS_ctx.auth.displayName);
__VLS_asFunctionalElement(__VLS_intrinsicElements.code, __VLS_intrinsicElements.code)({});
(__VLS_ctx.auth.userId);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "right" },
});
const __VLS_0 = {}.RouterLink;
/** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    ...{ class: "btn btnGhost" },
    to: "/login",
}));
const __VLS_2 = __VLS_1({
    ...{ class: "btn btnGhost" },
    to: "/login",
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_3.slots.default;
var __VLS_3;
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.resetThread) },
    ...{ class: "btn btnGhost" },
    type: "button",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "chat" },
});
if (__VLS_ctx.messages.length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "empty" },
    });
    (__VLS_ctx.activeSessionId ? '请输入问题开始对话。' : '正在准备会话…');
}
for (const [m, idx] of __VLS_getVForSourceType((__VLS_ctx.messages))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        key: (idx),
        ...{ class: "msgRow" },
        ...{ class: (m.role) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "avatar" },
    });
    (m.role === 'user' ? '我' : 'AI');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "bubble" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "content" },
    });
    (m.text);
    if (m.sources?.length) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "sources" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "sourcesTitle" },
        });
        for (const [s, i] of __VLS_getVForSourceType((m.sources))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                key: (i),
                ...{ class: "sourceItem" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "sourceMeta" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "badge" },
            });
            (s.datasetName || s.datasetId || 'dataset');
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "badge" },
            });
            (s.documentName || s.documentId || 'doc');
            if (typeof s.score === 'number') {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "badge" },
                });
                (s.score.toFixed(3));
            }
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "sourceText" },
            });
            (s.content);
        }
    }
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.form, __VLS_intrinsicElements.form)({
    ...{ onSubmit: (__VLS_ctx.send) },
    ...{ class: "composer" },
});
if (__VLS_ctx.menuOpen) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div)({
        ...{ onClick: (__VLS_ctx.closePlusMenu) },
        ...{ class: "menuOverlay" },
        'aria-hidden': "true",
    });
}
if (__VLS_ctx.pendingFiles.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "fileTags" },
    });
    for (const [f, i] of __VLS_getVForSourceType((__VLS_ctx.pendingFiles))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            key: (i),
            ...{ class: "badge" },
        });
        (f.transferMethod === 'local_file' ? `本地:${f.uploadFileId?.slice(0, 8)}…` : `链接:${__VLS_ctx.truncate(f.url, 24)}`);
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.pendingFiles.length))
                    return;
                __VLS_ctx.pendingFiles = [];
            } },
        ...{ class: "btn btnGhost" },
        type: "button",
    });
}
if (__VLS_ctx.urlPanelOpen) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "urlPanel" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        ...{ onKeyup: (__VLS_ctx.confirmUrlAndClose) },
        ...{ class: "input urlPanelInput" },
        placeholder: "粘贴文件或图片的直链 URL（https://…）",
        disabled: (__VLS_ctx.loading),
    });
    (__VLS_ctx.urlInput);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.confirmUrlAndClose) },
        ...{ class: "btn btnGhost" },
        type: "button",
        disabled: (!__VLS_ctx.urlInput.trim() || __VLS_ctx.loading),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.urlPanelOpen))
                    return;
                __VLS_ctx.urlPanelOpen = false;
            } },
        ...{ class: "btn btnGhost" },
        type: "button",
    });
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "composerRow" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "plusWrap" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.togglePlusMenu) },
    type: "button",
    ...{ class: "plusBtn" },
    'aria-expanded': (__VLS_ctx.menuOpen),
    title: "添加附件",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "plusMenu" },
    role: "menu",
});
__VLS_asFunctionalDirective(__VLS_directives.vShow)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.menuOpen) }, null, null);
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.pickLocalFile) },
    type: "button",
    ...{ class: "plusMenuItem" },
    role: "menuitem",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "plusMenuIco" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.openUrlPanelFromMenu) },
    type: "button",
    ...{ class: "plusMenuItem" },
    role: "menuitem",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "plusMenuIco" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ onChange: (__VLS_ctx.uploadLocalFile) },
    ref: "fileInputRef",
    type: "file",
    ...{ class: "fileHidden" },
    disabled: (__VLS_ctx.loading),
});
/** @type {typeof __VLS_ctx.fileInputRef} */ ;
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ class: "input inputMain" },
    placeholder: "尽管问，带图也行",
    disabled: (__VLS_ctx.loading),
});
(__VLS_ctx.input);
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ class: "btn" },
    type: "submit",
    disabled: (__VLS_ctx.loading || !__VLS_ctx.input.trim() || !__VLS_ctx.activeSessionId),
});
(__VLS_ctx.loading ? '发送中…' : '发送');
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "attachHint" },
});
if (__VLS_ctx.error) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "error" },
    });
    (__VLS_ctx.error);
}
/** @type {__VLS_StyleScopedClasses['chatShell']} */ ;
/** @type {__VLS_StyleScopedClasses['sessionSidebar']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btnNewChat']} */ ;
/** @type {__VLS_StyleScopedClasses['sessionList']} */ ;
/** @type {__VLS_StyleScopedClasses['sessionItem']} */ ;
/** @type {__VLS_StyleScopedClasses['sessionTitle']} */ ;
/** @type {__VLS_StyleScopedClasses['sessionTime']} */ ;
/** @type {__VLS_StyleScopedClasses['sessionEmpty']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['sidebarHint']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['chatCard']} */ ;
/** @type {__VLS_StyleScopedClasses['cardHeader']} */ ;
/** @type {__VLS_StyleScopedClasses['h1']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['right']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btnGhost']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btnGhost']} */ ;
/** @type {__VLS_StyleScopedClasses['chat']} */ ;
/** @type {__VLS_StyleScopedClasses['empty']} */ ;
/** @type {__VLS_StyleScopedClasses['msgRow']} */ ;
/** @type {__VLS_StyleScopedClasses['avatar']} */ ;
/** @type {__VLS_StyleScopedClasses['bubble']} */ ;
/** @type {__VLS_StyleScopedClasses['content']} */ ;
/** @type {__VLS_StyleScopedClasses['sources']} */ ;
/** @type {__VLS_StyleScopedClasses['sourcesTitle']} */ ;
/** @type {__VLS_StyleScopedClasses['sourceItem']} */ ;
/** @type {__VLS_StyleScopedClasses['sourceMeta']} */ ;
/** @type {__VLS_StyleScopedClasses['badge']} */ ;
/** @type {__VLS_StyleScopedClasses['badge']} */ ;
/** @type {__VLS_StyleScopedClasses['badge']} */ ;
/** @type {__VLS_StyleScopedClasses['sourceText']} */ ;
/** @type {__VLS_StyleScopedClasses['composer']} */ ;
/** @type {__VLS_StyleScopedClasses['menuOverlay']} */ ;
/** @type {__VLS_StyleScopedClasses['fileTags']} */ ;
/** @type {__VLS_StyleScopedClasses['badge']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btnGhost']} */ ;
/** @type {__VLS_StyleScopedClasses['urlPanel']} */ ;
/** @type {__VLS_StyleScopedClasses['input']} */ ;
/** @type {__VLS_StyleScopedClasses['urlPanelInput']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btnGhost']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btnGhost']} */ ;
/** @type {__VLS_StyleScopedClasses['composerRow']} */ ;
/** @type {__VLS_StyleScopedClasses['plusWrap']} */ ;
/** @type {__VLS_StyleScopedClasses['plusBtn']} */ ;
/** @type {__VLS_StyleScopedClasses['plusMenu']} */ ;
/** @type {__VLS_StyleScopedClasses['plusMenuItem']} */ ;
/** @type {__VLS_StyleScopedClasses['plusMenuIco']} */ ;
/** @type {__VLS_StyleScopedClasses['plusMenuItem']} */ ;
/** @type {__VLS_StyleScopedClasses['plusMenuIco']} */ ;
/** @type {__VLS_StyleScopedClasses['fileHidden']} */ ;
/** @type {__VLS_StyleScopedClasses['input']} */ ;
/** @type {__VLS_StyleScopedClasses['inputMain']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['attachHint']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            RouterLink: RouterLink,
            retentionHint: retentionHint,
            auth: auth,
            input: input,
            urlInput: urlInput,
            loading: loading,
            error: error,
            messages: messages,
            pendingFiles: pendingFiles,
            menuOpen: menuOpen,
            urlPanelOpen: urlPanelOpen,
            fileInputRef: fileInputRef,
            sessions: sessions,
            activeSessionId: activeSessionId,
            truncate: truncate,
            formatTime: formatTime,
            togglePlusMenu: togglePlusMenu,
            closePlusMenu: closePlusMenu,
            pickLocalFile: pickLocalFile,
            openUrlPanelFromMenu: openUrlPanelFromMenu,
            confirmUrlAndClose: confirmUrlAndClose,
            newSession: newSession,
            selectSession: selectSession,
            resetThread: resetThread,
            send: send,
            uploadLocalFile: uploadLocalFile,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
