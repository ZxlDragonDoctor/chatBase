/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref } from 'vue';
import { webChat } from '../api/chat';
import { getOrCreateUserId } from '../lib/user';
import { uploadFile } from '../api/upload';
const userId = getOrCreateUserId();
const input = ref('');
const urlInput = ref('');
const loading = ref(false);
const error = ref(null);
const messages = ref([]);
const pendingFiles = ref([]);
const menuOpen = ref(false);
const urlPanelOpen = ref(false);
const fileInputRef = ref(null);
function truncate(s, max) {
    if (!s)
        return '';
    return s.length > max ? `${s.slice(0, max)}…` : s;
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
function reset() {
    messages.value = [];
    error.value = null;
    input.value = '';
    urlInput.value = '';
    pendingFiles.value = [];
    menuOpen.value = false;
    urlPanelOpen.value = false;
}
async function send() {
    const text = input.value.trim();
    if (!text)
        return;
    error.value = null;
    messages.value.push({ role: 'user', text });
    input.value = '';
    loading.value = true;
    try {
        const files = [...pendingFiles.value];
        const resp = await webChat(text, userId, files);
        messages.value.push({
            role: 'assistant',
            text: resp.answer || '（无返回）',
            sources: resp.retrieverResources || [],
        });
        pendingFiles.value = [];
    }
    catch (e) {
        error.value = e?.message || '请求失败';
        messages.value.push({ role: 'assistant', text: '【系统错误】请求失败，请稍后再试' });
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
        const resp = await uploadFile(file, userId);
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
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "pageShell" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "card" },
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
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "pill" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.code, __VLS_intrinsicElements.code)({});
(__VLS_ctx.userId);
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.reset) },
    ...{ class: "btn btnGhost" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "chat" },
});
if (__VLS_ctx.messages.length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "empty" },
    });
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
    disabled: (__VLS_ctx.loading || !__VLS_ctx.input.trim()),
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
/** @type {__VLS_StyleScopedClasses['pageShell']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['cardHeader']} */ ;
/** @type {__VLS_StyleScopedClasses['h1']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['right']} */ ;
/** @type {__VLS_StyleScopedClasses['pill']} */ ;
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
            userId: userId,
            input: input,
            urlInput: urlInput,
            loading: loading,
            error: error,
            messages: messages,
            pendingFiles: pendingFiles,
            menuOpen: menuOpen,
            urlPanelOpen: urlPanelOpen,
            fileInputRef: fileInputRef,
            truncate: truncate,
            togglePlusMenu: togglePlusMenu,
            closePlusMenu: closePlusMenu,
            pickLocalFile: pickLocalFile,
            openUrlPanelFromMenu: openUrlPanelFromMenu,
            confirmUrlAndClose: confirmUrlAndClose,
            reset: reset,
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
