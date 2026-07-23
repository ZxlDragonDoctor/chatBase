/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { login, register } from '../api/user';
const router = useRouter();
const username = ref('');
const password = ref('');
const loading = ref(false);
const error = ref(null);
const illLoaded = ref(false);
const showRegister = ref(false);
const regUsername = ref('');
const regPassword = ref('');
const regNickname = ref('');
const regLoading = ref(false);
const regError = ref(null);
const animeBgUrl = ref('https://t.alcy.cc/moe?' + Date.now());
function refreshBg() {
    animeBgUrl.value = 'https://t.alcy.cc/moe?' + Date.now();
    illLoaded.value = false;
}
async function handleLogin() {
    if (!username.value.trim() || !password.value.trim())
        return;
    loading.value = true;
    error.value = null;
    try {
        const resp = await login(username.value.trim(), password.value);
        if (resp.success && resp.token) {
            localStorage.setItem('chatbase_token', resp.token);
            localStorage.setItem('chatbase_user', resp.user?.nickname || resp.user?.username || username.value.trim());
            localStorage.setItem('chatbase_original_username', resp.user?.username || username.value.trim());
            if (resp.user?.role)
                localStorage.setItem('chatbase_role', resp.user.role);
            if (resp.user?.id)
                localStorage.setItem('chatbase_admin_id', resp.user.id.toString());
            router.push('/console/dashboard');
        }
        else {
            error.value = resp.message || '用户名或密码错误';
        }
    }
    catch (e) {
        error.value = e?.message || '登录失败';
    }
    finally {
        loading.value = false;
    }
}
async function handleRegister() {
    if (!regUsername.value.trim() || !regPassword.value.trim())
        return;
    regLoading.value = true;
    regError.value = null;
    try {
        const resp = await register(regUsername.value.trim(), regPassword.value, regNickname.value.trim());
        if (resp.success) {
            showRegister.value = false;
            username.value = regUsername.value;
            password.value = regPassword.value;
            regUsername.value = '';
            regPassword.value = '';
            regNickname.value = '';
        }
        else {
            regError.value = resp.message || '注册失败';
        }
    }
    catch (e) {
        regError.value = e?.message || '注册失败';
    }
    finally {
        regLoading.value = false;
    }
}
onMounted(() => {
    setInterval(refreshBg, 60000);
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['login-ill-img']} */ ;
/** @type {__VLS_StyleScopedClasses['ill-feature']} */ ;
/** @type {__VLS_StyleScopedClasses['ill-feature']} */ ;
/** @type {__VLS_StyleScopedClasses['ill-feature']} */ ;
/** @type {__VLS_StyleScopedClasses['ill-feature']} */ ;
/** @type {__VLS_StyleScopedClasses['login-input']} */ ;
/** @type {__VLS_StyleScopedClasses['login-input']} */ ;
/** @type {__VLS_StyleScopedClasses['login-input']} */ ;
/** @type {__VLS_StyleScopedClasses['login-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['login-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['login-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['login-link-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['login-illustration']} */ ;
/** @type {__VLS_StyleScopedClasses['login-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['login-card']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-bg-decor" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "sakura-container" },
});
for (const [i] of __VLS_getVForSourceType((6))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        key: (i),
        ...{ class: "sakura-petal" },
        ...{ style: ({
                left: Math.random() * 100 + '%',
                animationDelay: (i * 1.2) + 's',
                animationDuration: (8 + Math.random() * 6) + 's',
                width: (10 + Math.random() * 8) + 'px',
                height: (10 + Math.random() * 8) + 'px',
                opacity: 0.3 + Math.random() * 0.3
            }) },
    });
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-layout" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-illustration" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-ill-bg" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.img)({
    ...{ onLoad: (...[$event]) => {
            __VLS_ctx.illLoaded = true;
        } },
    src: (__VLS_ctx.animeBgUrl),
    alt: "",
    ...{ class: "login-ill-img" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-ill-overlay" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-ill-content" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "ill-badge" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "ill-badge-dot" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "ill-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "ill-desc" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "ill-features" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "ill-feature" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "ill-feature-icon" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "ill-feature" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "ill-feature-icon" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "ill-feature" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "ill-feature-icon" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "ill-feature" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "ill-feature-icon" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "ill-footer" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-card-inner" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-logo" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-logo-icon" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.img)({
    src: "/logo.png",
    alt: "logo",
    ...{ class: "login-logo-img" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({
    ...{ class: "login-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "login-subtitle" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-form" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "login-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-input-wrap" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "login-input-icon" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.svg, __VLS_intrinsicElements.svg)({
    width: "16",
    height: "16",
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    'stroke-width': "2",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.circle)({
    cx: "12",
    cy: "7",
    r: "4",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ class: "login-input" },
    placeholder: "请输入用户名",
    disabled: (__VLS_ctx.loading),
});
(__VLS_ctx.username);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "login-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-input-wrap" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "login-input-icon" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.svg, __VLS_intrinsicElements.svg)({
    width: "16",
    height: "16",
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    'stroke-width': "2",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.rect)({
    x: "3",
    y: "11",
    width: "18",
    height: "11",
    rx: "2",
    ry: "2",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M7 11V7a5 5 0 0 1 10 0v4",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ onKeyup: (__VLS_ctx.handleLogin) },
    ...{ class: "login-input" },
    type: "password",
    placeholder: "请输入密码",
    disabled: (__VLS_ctx.loading),
});
(__VLS_ctx.password);
if (__VLS_ctx.error) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "login-error" },
    });
    (__VLS_ctx.error);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.handleLogin) },
    ...{ class: "login-btn" },
    disabled: (__VLS_ctx.loading || !__VLS_ctx.username.trim() || !__VLS_ctx.password.trim()),
});
if (__VLS_ctx.loading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "login-btn-loader" },
    });
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-register-link" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.showRegister = true;
        } },
    ...{ class: "login-link-btn" },
});
if (__VLS_ctx.showRegister) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showRegister))
                    return;
                __VLS_ctx.showRegister = false;
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
                if (!(__VLS_ctx.showRegister))
                    return;
                __VLS_ctx.showRegister = false;
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
        placeholder: "请输入用户名",
        disabled: (__VLS_ctx.regLoading),
    });
    (__VLS_ctx.regUsername);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        ...{ class: "anime-input" },
        type: "password",
        placeholder: "请输入密码",
        disabled: (__VLS_ctx.regLoading),
    });
    (__VLS_ctx.regPassword);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        ...{ class: "anime-input" },
        placeholder: "请输入昵称",
        disabled: (__VLS_ctx.regLoading),
    });
    (__VLS_ctx.regNickname);
    if (__VLS_ctx.regError) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "anime-error" },
            ...{ style: {} },
        });
        (__VLS_ctx.regError);
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-modal-footer" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showRegister))
                    return;
                __VLS_ctx.showRegister = false;
            } },
        ...{ class: "anime-btn ghost" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.handleRegister) },
        ...{ class: "anime-btn primary" },
        disabled: (__VLS_ctx.regLoading || !__VLS_ctx.regUsername.trim() || !__VLS_ctx.regPassword.trim()),
    });
    if (__VLS_ctx.regLoading) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-loader-spinner" },
        });
    }
    else {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    }
}
/** @type {__VLS_StyleScopedClasses['login-page']} */ ;
/** @type {__VLS_StyleScopedClasses['login-bg-decor']} */ ;
/** @type {__VLS_StyleScopedClasses['sakura-container']} */ ;
/** @type {__VLS_StyleScopedClasses['sakura-petal']} */ ;
/** @type {__VLS_StyleScopedClasses['login-layout']} */ ;
/** @type {__VLS_StyleScopedClasses['login-illustration']} */ ;
/** @type {__VLS_StyleScopedClasses['login-ill-bg']} */ ;
/** @type {__VLS_StyleScopedClasses['login-ill-img']} */ ;
/** @type {__VLS_StyleScopedClasses['login-ill-overlay']} */ ;
/** @type {__VLS_StyleScopedClasses['login-ill-content']} */ ;
/** @type {__VLS_StyleScopedClasses['ill-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['ill-badge-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['ill-title']} */ ;
/** @type {__VLS_StyleScopedClasses['ill-desc']} */ ;
/** @type {__VLS_StyleScopedClasses['ill-features']} */ ;
/** @type {__VLS_StyleScopedClasses['ill-feature']} */ ;
/** @type {__VLS_StyleScopedClasses['ill-feature-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['ill-feature']} */ ;
/** @type {__VLS_StyleScopedClasses['ill-feature-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['ill-feature']} */ ;
/** @type {__VLS_StyleScopedClasses['ill-feature-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['ill-feature']} */ ;
/** @type {__VLS_StyleScopedClasses['ill-feature-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['ill-footer']} */ ;
/** @type {__VLS_StyleScopedClasses['login-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['login-card']} */ ;
/** @type {__VLS_StyleScopedClasses['login-card-inner']} */ ;
/** @type {__VLS_StyleScopedClasses['login-logo']} */ ;
/** @type {__VLS_StyleScopedClasses['login-logo-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['login-logo-img']} */ ;
/** @type {__VLS_StyleScopedClasses['login-title']} */ ;
/** @type {__VLS_StyleScopedClasses['login-subtitle']} */ ;
/** @type {__VLS_StyleScopedClasses['login-form']} */ ;
/** @type {__VLS_StyleScopedClasses['login-field']} */ ;
/** @type {__VLS_StyleScopedClasses['login-label']} */ ;
/** @type {__VLS_StyleScopedClasses['login-input-wrap']} */ ;
/** @type {__VLS_StyleScopedClasses['login-input-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['login-input']} */ ;
/** @type {__VLS_StyleScopedClasses['login-field']} */ ;
/** @type {__VLS_StyleScopedClasses['login-label']} */ ;
/** @type {__VLS_StyleScopedClasses['login-input-wrap']} */ ;
/** @type {__VLS_StyleScopedClasses['login-input-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['login-input']} */ ;
/** @type {__VLS_StyleScopedClasses['login-error']} */ ;
/** @type {__VLS_StyleScopedClasses['login-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['login-btn-loader']} */ ;
/** @type {__VLS_StyleScopedClasses['login-register-link']} */ ;
/** @type {__VLS_StyleScopedClasses['login-link-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-overlay']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-header']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-title']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-close']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-body']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-error']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-modal-footer']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-loader-spinner']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            username: username,
            password: password,
            loading: loading,
            error: error,
            illLoaded: illLoaded,
            showRegister: showRegister,
            regUsername: regUsername,
            regPassword: regPassword,
            regNickname: regNickname,
            regLoading: regLoading,
            regError: regError,
            animeBgUrl: animeBgUrl,
            handleLogin: handleLogin,
            handleRegister: handleRegister,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
