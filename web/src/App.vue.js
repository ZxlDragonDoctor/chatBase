/// <reference types="../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref, computed, onMounted, watch } from 'vue';
import { RouterLink, RouterView, useRouter, useRoute } from 'vue-router';
import { Home, BarChart3, Users, BookOpen, MessageCircle, Mail, HelpCircle, LogOut, Bot, ClipboardList, Cpu } from 'lucide-vue-next';
import UserProfile from './components/UserProfile.vue';
import { getCurrentUser as fetchUserProfile } from './api/user';
const router = useRouter();
const route = useRoute();
const isLoginPage = computed(() => route.path === '/login');
const currentUser = ref('');
const isAdmin = ref(false);
const role = ref('');
function syncAuthState() {
    currentUser.value = localStorage.getItem('chatbase_user') || '';
    isAdmin.value = localStorage.getItem('chatbase_role') === 'admin';
    role.value = localStorage.getItem('chatbase_role') || '';
}
function getOriginalUsername() {
    return localStorage.getItem('chatbase_original_username') || localStorage.getItem('chatbase_user') || '';
}
const showUserProfile = ref(false);
const userProfile = ref(null);
async function openUserProfile() {
    showUserProfile.value = true;
    await loadUserProfile();
}
const roleLabel = computed(() => {
    switch (role.value) {
        case 'admin': return '管理员';
        case 'user': return '用户';
        default: return '访客';
    }
});
async function loadUserProfile() {
    if (!localStorage.getItem('chatbase_token'))
        return;
    const username = getOriginalUsername();
    if (!username)
        return;
    try {
        const user = await fetchUserProfile(username);
        if (user) {
            userProfile.value = user;
            localStorage.setItem('chatbase_original_username', user.username);
        }
    }
    catch (e) {
        console.error('加载用户信息失败', e);
    }
}
function handleUserUpdated(user) {
    userProfile.value = user;
    const displayName = user.nickname || user.username;
    if (displayName)
        localStorage.setItem('chatbase_user', displayName);
    localStorage.setItem('chatbase_original_username', user.username);
    if (user.role)
        localStorage.setItem('chatbase_role', user.role);
    syncAuthState();
}
function getAvatarUrl(path) {
    if (!path)
        return '';
    if (path.startsWith('http'))
        return path;
    return `/api${path}`;
}
function onAvatarError(e) {
    const img = e.target;
    img.style.display = 'none';
}
function handleLogout() {
    localStorage.removeItem('chatbase_token');
    localStorage.removeItem('chatbase_user');
    localStorage.removeItem('chatbase_original_username');
    localStorage.removeItem('chatbase_role');
    localStorage.removeItem('chatbase_admin_id');
    router.push('/login');
}
watch(() => route.path, () => {
    syncAuthState();
    if (localStorage.getItem('chatbase_token'))
        loadUserProfile();
});
onMounted(() => {
    syncAuthState();
    loadUserProfile();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-bg-decor" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-floating-stars" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "anime-star" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "anime-star" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "anime-star" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "anime-star" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-sakura-container" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "anime-sakura-petal" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "anime-sakura-petal" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "anime-sakura-petal" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "anime-sakura-petal" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "anime-sakura-petal" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "anime-sakura-petal" },
    ...{ style: {} },
});
if (__VLS_ctx.isLoginPage) {
    const __VLS_0 = {}.RouterView;
    /** @type {[typeof __VLS_components.RouterView, typeof __VLS_components.RouterView, ]} */ ;
    // @ts-ignore
    const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({}));
    const __VLS_2 = __VLS_1({}, ...__VLS_functionalComponentArgsRest(__VLS_1));
    {
        const { default: __VLS_thisSlot } = __VLS_3.slots;
        const [{ Component }] = __VLS_getSlotParams(__VLS_thisSlot);
        const __VLS_4 = {}.transition;
        /** @type {[typeof __VLS_components.Transition, typeof __VLS_components.transition, typeof __VLS_components.Transition, typeof __VLS_components.transition, ]} */ ;
        // @ts-ignore
        const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
            name: "anime-fade",
            mode: "out-in",
        }));
        const __VLS_6 = __VLS_5({
            name: "anime-fade",
            mode: "out-in",
        }, ...__VLS_functionalComponentArgsRest(__VLS_5));
        __VLS_7.slots.default;
        const __VLS_8 = ((Component));
        // @ts-ignore
        const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({}));
        const __VLS_10 = __VLS_9({}, ...__VLS_functionalComponentArgsRest(__VLS_9));
        var __VLS_7;
        __VLS_3.slots['' /* empty slot name completion */];
    }
    var __VLS_3;
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-app" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
        ...{ class: "anime-nav" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-nav-brand" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-logo" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.img)({
        src: "/logo.png",
        alt: "logo",
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-brand-title" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-brand-sub" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.nav, __VLS_intrinsicElements.nav)({
        ...{ class: "anime-nav-list" },
    });
    const __VLS_12 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
        ...{ class: "anime-nav-item" },
        to: "/console/dashboard",
        activeClass: "active",
    }));
    const __VLS_14 = __VLS_13({
        ...{ class: "anime-nav-item" },
        to: "/console/dashboard",
        activeClass: "active",
    }, ...__VLS_functionalComponentArgsRest(__VLS_13));
    __VLS_15.slots.default;
    const __VLS_16 = {}.Home;
    /** @type {[typeof __VLS_components.Home, ]} */ ;
    // @ts-ignore
    const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
        ...{ class: "anime-nav-icon" },
        size: (20),
    }));
    const __VLS_18 = __VLS_17({
        ...{ class: "anime-nav-icon" },
        size: (20),
    }, ...__VLS_functionalComponentArgsRest(__VLS_17));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    var __VLS_15;
    const __VLS_20 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
        ...{ class: "anime-nav-item" },
        to: "/console/statistics",
        activeClass: "active",
    }));
    const __VLS_22 = __VLS_21({
        ...{ class: "anime-nav-item" },
        to: "/console/statistics",
        activeClass: "active",
    }, ...__VLS_functionalComponentArgsRest(__VLS_21));
    __VLS_23.slots.default;
    const __VLS_24 = {}.BarChart3;
    /** @type {[typeof __VLS_components.BarChart3, ]} */ ;
    // @ts-ignore
    const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
        ...{ class: "anime-nav-icon" },
        size: (20),
    }));
    const __VLS_26 = __VLS_25({
        ...{ class: "anime-nav-icon" },
        size: (20),
    }, ...__VLS_functionalComponentArgsRest(__VLS_25));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    var __VLS_23;
    const __VLS_28 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
        ...{ class: "anime-nav-item" },
        to: "/console/im",
        activeClass: "active",
    }));
    const __VLS_30 = __VLS_29({
        ...{ class: "anime-nav-item" },
        to: "/console/im",
        activeClass: "active",
    }, ...__VLS_functionalComponentArgsRest(__VLS_29));
    __VLS_31.slots.default;
    const __VLS_32 = {}.Users;
    /** @type {[typeof __VLS_components.Users, ]} */ ;
    // @ts-ignore
    const __VLS_33 = __VLS_asFunctionalComponent(__VLS_32, new __VLS_32({
        ...{ class: "anime-nav-icon" },
        size: (20),
    }));
    const __VLS_34 = __VLS_33({
        ...{ class: "anime-nav-icon" },
        size: (20),
    }, ...__VLS_functionalComponentArgsRest(__VLS_33));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    var __VLS_31;
    const __VLS_36 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_37 = __VLS_asFunctionalComponent(__VLS_36, new __VLS_36({
        ...{ class: "anime-nav-item" },
        to: "/console/knowledge",
        activeClass: "active",
    }));
    const __VLS_38 = __VLS_37({
        ...{ class: "anime-nav-item" },
        to: "/console/knowledge",
        activeClass: "active",
    }, ...__VLS_functionalComponentArgsRest(__VLS_37));
    __VLS_39.slots.default;
    const __VLS_40 = {}.BookOpen;
    /** @type {[typeof __VLS_components.BookOpen, ]} */ ;
    // @ts-ignore
    const __VLS_41 = __VLS_asFunctionalComponent(__VLS_40, new __VLS_40({
        ...{ class: "anime-nav-icon" },
        size: (20),
    }));
    const __VLS_42 = __VLS_41({
        ...{ class: "anime-nav-icon" },
        size: (20),
    }, ...__VLS_functionalComponentArgsRest(__VLS_41));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    var __VLS_39;
    const __VLS_44 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_45 = __VLS_asFunctionalComponent(__VLS_44, new __VLS_44({
        ...{ class: "anime-nav-item" },
        to: "/console/app",
        activeClass: "active",
    }));
    const __VLS_46 = __VLS_45({
        ...{ class: "anime-nav-item" },
        to: "/console/app",
        activeClass: "active",
    }, ...__VLS_functionalComponentArgsRest(__VLS_45));
    __VLS_47.slots.default;
    const __VLS_48 = {}.Bot;
    /** @type {[typeof __VLS_components.Bot, ]} */ ;
    // @ts-ignore
    const __VLS_49 = __VLS_asFunctionalComponent(__VLS_48, new __VLS_48({
        ...{ class: "anime-nav-icon" },
        size: (20),
    }));
    const __VLS_50 = __VLS_49({
        ...{ class: "anime-nav-icon" },
        size: (20),
    }, ...__VLS_functionalComponentArgsRest(__VLS_49));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    var __VLS_47;
    const __VLS_52 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_53 = __VLS_asFunctionalComponent(__VLS_52, new __VLS_52({
        ...{ class: "anime-nav-item" },
        to: "/console/bots",
        activeClass: "active",
    }));
    const __VLS_54 = __VLS_53({
        ...{ class: "anime-nav-item" },
        to: "/console/bots",
        activeClass: "active",
    }, ...__VLS_functionalComponentArgsRest(__VLS_53));
    __VLS_55.slots.default;
    const __VLS_56 = {}.Cpu;
    /** @type {[typeof __VLS_components.Cpu, ]} */ ;
    // @ts-ignore
    const __VLS_57 = __VLS_asFunctionalComponent(__VLS_56, new __VLS_56({
        ...{ class: "anime-nav-icon" },
        size: (20),
    }));
    const __VLS_58 = __VLS_57({
        ...{ class: "anime-nav-icon" },
        size: (20),
    }, ...__VLS_functionalComponentArgsRest(__VLS_57));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    var __VLS_55;
    const __VLS_60 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_61 = __VLS_asFunctionalComponent(__VLS_60, new __VLS_60({
        ...{ class: "anime-nav-item" },
        to: "/console/faq",
        activeClass: "active",
    }));
    const __VLS_62 = __VLS_61({
        ...{ class: "anime-nav-item" },
        to: "/console/faq",
        activeClass: "active",
    }, ...__VLS_functionalComponentArgsRest(__VLS_61));
    __VLS_63.slots.default;
    const __VLS_64 = {}.HelpCircle;
    /** @type {[typeof __VLS_components.HelpCircle, ]} */ ;
    // @ts-ignore
    const __VLS_65 = __VLS_asFunctionalComponent(__VLS_64, new __VLS_64({
        ...{ class: "anime-nav-icon" },
        size: (20),
    }));
    const __VLS_66 = __VLS_65({
        ...{ class: "anime-nav-icon" },
        size: (20),
    }, ...__VLS_functionalComponentArgsRest(__VLS_65));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    var __VLS_63;
    const __VLS_68 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_69 = __VLS_asFunctionalComponent(__VLS_68, new __VLS_68({
        ...{ class: "anime-nav-item" },
        to: "/chat",
        activeClass: "active",
    }));
    const __VLS_70 = __VLS_69({
        ...{ class: "anime-nav-item" },
        to: "/chat",
        activeClass: "active",
    }, ...__VLS_functionalComponentArgsRest(__VLS_69));
    __VLS_71.slots.default;
    const __VLS_72 = {}.MessageCircle;
    /** @type {[typeof __VLS_components.MessageCircle, ]} */ ;
    // @ts-ignore
    const __VLS_73 = __VLS_asFunctionalComponent(__VLS_72, new __VLS_72({
        ...{ class: "anime-nav-icon" },
        size: (20),
    }));
    const __VLS_74 = __VLS_73({
        ...{ class: "anime-nav-icon" },
        size: (20),
    }, ...__VLS_functionalComponentArgsRest(__VLS_73));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    var __VLS_71;
    const __VLS_76 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_77 = __VLS_asFunctionalComponent(__VLS_76, new __VLS_76({
        ...{ class: "anime-nav-item" },
        to: "/feedback",
        activeClass: "active",
    }));
    const __VLS_78 = __VLS_77({
        ...{ class: "anime-nav-item" },
        to: "/feedback",
        activeClass: "active",
    }, ...__VLS_functionalComponentArgsRest(__VLS_77));
    __VLS_79.slots.default;
    const __VLS_80 = {}.Mail;
    /** @type {[typeof __VLS_components.Mail, ]} */ ;
    // @ts-ignore
    const __VLS_81 = __VLS_asFunctionalComponent(__VLS_80, new __VLS_80({
        ...{ class: "anime-nav-icon" },
        size: (20),
    }));
    const __VLS_82 = __VLS_81({
        ...{ class: "anime-nav-icon" },
        size: (20),
    }, ...__VLS_functionalComponentArgsRest(__VLS_81));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    var __VLS_79;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    if (__VLS_ctx.isAdmin) {
        const __VLS_84 = {}.RouterLink;
        /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
        // @ts-ignore
        const __VLS_85 = __VLS_asFunctionalComponent(__VLS_84, new __VLS_84({
            ...{ class: "anime-nav-item" },
            to: "/console/feedback-manage",
            activeClass: "active",
        }));
        const __VLS_86 = __VLS_85({
            ...{ class: "anime-nav-item" },
            to: "/console/feedback-manage",
            activeClass: "active",
        }, ...__VLS_functionalComponentArgsRest(__VLS_85));
        __VLS_87.slots.default;
        const __VLS_88 = {}.ClipboardList;
        /** @type {[typeof __VLS_components.ClipboardList, ]} */ ;
        // @ts-ignore
        const __VLS_89 = __VLS_asFunctionalComponent(__VLS_88, new __VLS_88({
            ...{ class: "anime-nav-icon" },
            size: (20),
        }));
        const __VLS_90 = __VLS_89({
            ...{ class: "anime-nav-icon" },
            size: (20),
        }, ...__VLS_functionalComponentArgsRest(__VLS_89));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        var __VLS_87;
    }
    if (__VLS_ctx.isAdmin) {
        const __VLS_92 = {}.RouterLink;
        /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
        // @ts-ignore
        const __VLS_93 = __VLS_asFunctionalComponent(__VLS_92, new __VLS_92({
            ...{ class: "anime-nav-item" },
            to: "/console/admin/apps",
            activeClass: "active",
        }));
        const __VLS_94 = __VLS_93({
            ...{ class: "anime-nav-item" },
            to: "/console/admin/apps",
            activeClass: "active",
        }, ...__VLS_functionalComponentArgsRest(__VLS_93));
        __VLS_95.slots.default;
        const __VLS_96 = {}.Bot;
        /** @type {[typeof __VLS_components.Bot, ]} */ ;
        // @ts-ignore
        const __VLS_97 = __VLS_asFunctionalComponent(__VLS_96, new __VLS_96({
            ...{ class: "anime-nav-icon" },
            size: (20),
        }));
        const __VLS_98 = __VLS_97({
            ...{ class: "anime-nav-icon" },
            size: (20),
        }, ...__VLS_functionalComponentArgsRest(__VLS_97));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        var __VLS_95;
    }
    if (__VLS_ctx.isAdmin) {
        const __VLS_100 = {}.RouterLink;
        /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
        // @ts-ignore
        const __VLS_101 = __VLS_asFunctionalComponent(__VLS_100, new __VLS_100({
            ...{ class: "anime-nav-item" },
            to: "/console/admin/kbs",
            activeClass: "active",
        }));
        const __VLS_102 = __VLS_101({
            ...{ class: "anime-nav-item" },
            to: "/console/admin/kbs",
            activeClass: "active",
        }, ...__VLS_functionalComponentArgsRest(__VLS_101));
        __VLS_103.slots.default;
        const __VLS_104 = {}.BookOpen;
        /** @type {[typeof __VLS_components.BookOpen, ]} */ ;
        // @ts-ignore
        const __VLS_105 = __VLS_asFunctionalComponent(__VLS_104, new __VLS_104({
            ...{ class: "anime-nav-icon" },
            size: (20),
        }));
        const __VLS_106 = __VLS_105({
            ...{ class: "anime-nav-icon" },
            size: (20),
        }, ...__VLS_functionalComponentArgsRest(__VLS_105));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        var __VLS_103;
    }
    if (__VLS_ctx.isAdmin) {
        const __VLS_108 = {}.RouterLink;
        /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
        // @ts-ignore
        const __VLS_109 = __VLS_asFunctionalComponent(__VLS_108, new __VLS_108({
            ...{ class: "anime-nav-item" },
            to: "/console/admin/users",
            activeClass: "active",
        }));
        const __VLS_110 = __VLS_109({
            ...{ class: "anime-nav-item" },
            to: "/console/admin/users",
            activeClass: "active",
        }, ...__VLS_functionalComponentArgsRest(__VLS_109));
        __VLS_111.slots.default;
        const __VLS_112 = {}.Users;
        /** @type {[typeof __VLS_components.Users, ]} */ ;
        // @ts-ignore
        const __VLS_113 = __VLS_asFunctionalComponent(__VLS_112, new __VLS_112({
            ...{ class: "anime-nav-icon" },
            size: (20),
        }));
        const __VLS_114 = __VLS_113({
            ...{ class: "anime-nav-icon" },
            size: (20),
        }, ...__VLS_functionalComponentArgsRest(__VLS_113));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        var __VLS_111;
    }
    if (__VLS_ctx.currentUser) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "anime-nav-footer" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ onClick: (__VLS_ctx.openUserProfile) },
            ...{ class: "gus-avatar" },
        });
        if (__VLS_ctx.userProfile?.avatar) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.img)({
                ...{ onError: (__VLS_ctx.onAvatarError) },
                src: (__VLS_ctx.getAvatarUrl(__VLS_ctx.userProfile.avatar)),
                alt: "",
            });
        }
        else {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
            ((__VLS_ctx.userProfile?.nickname || __VLS_ctx.currentUser).charAt(0).toUpperCase());
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ onClick: (__VLS_ctx.openUserProfile) },
            ...{ class: "gus-info" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "gus-name" },
        });
        (__VLS_ctx.userProfile?.nickname || __VLS_ctx.currentUser);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "gus-role" },
        });
        (__VLS_ctx.roleLabel);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (__VLS_ctx.handleLogout) },
            ...{ class: "anime-btn xs" },
        });
        const __VLS_116 = {}.LogOut;
        /** @type {[typeof __VLS_components.LogOut, ]} */ ;
        // @ts-ignore
        const __VLS_117 = __VLS_asFunctionalComponent(__VLS_116, new __VLS_116({
            size: (14),
        }));
        const __VLS_118 = __VLS_117({
            size: (14),
        }, ...__VLS_functionalComponentArgsRest(__VLS_117));
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.main, __VLS_intrinsicElements.main)({
        ...{ class: "anime-main" },
    });
    const __VLS_120 = {}.RouterView;
    /** @type {[typeof __VLS_components.RouterView, typeof __VLS_components.RouterView, ]} */ ;
    // @ts-ignore
    const __VLS_121 = __VLS_asFunctionalComponent(__VLS_120, new __VLS_120({}));
    const __VLS_122 = __VLS_121({}, ...__VLS_functionalComponentArgsRest(__VLS_121));
    {
        const { default: __VLS_thisSlot } = __VLS_123.slots;
        const [{ Component }] = __VLS_getSlotParams(__VLS_thisSlot);
        const __VLS_124 = {}.transition;
        /** @type {[typeof __VLS_components.Transition, typeof __VLS_components.transition, typeof __VLS_components.Transition, typeof __VLS_components.transition, ]} */ ;
        // @ts-ignore
        const __VLS_125 = __VLS_asFunctionalComponent(__VLS_124, new __VLS_124({
            name: "anime-fade",
            mode: "out-in",
        }));
        const __VLS_126 = __VLS_125({
            name: "anime-fade",
            mode: "out-in",
        }, ...__VLS_functionalComponentArgsRest(__VLS_125));
        __VLS_127.slots.default;
        const __VLS_128 = ((Component));
        // @ts-ignore
        const __VLS_129 = __VLS_asFunctionalComponent(__VLS_128, new __VLS_128({}));
        const __VLS_130 = __VLS_129({}, ...__VLS_functionalComponentArgsRest(__VLS_129));
        var __VLS_127;
        __VLS_123.slots['' /* empty slot name completion */];
    }
    var __VLS_123;
    if (__VLS_ctx.showUserProfile) {
        /** @type {[typeof UserProfile, ]} */ ;
        // @ts-ignore
        const __VLS_132 = __VLS_asFunctionalComponent(UserProfile, new UserProfile({
            ...{ 'onClose': {} },
            ...{ 'onUpdated': {} },
            show: (__VLS_ctx.showUserProfile),
            user: (__VLS_ctx.userProfile),
        }));
        const __VLS_133 = __VLS_132({
            ...{ 'onClose': {} },
            ...{ 'onUpdated': {} },
            show: (__VLS_ctx.showUserProfile),
            user: (__VLS_ctx.userProfile),
        }, ...__VLS_functionalComponentArgsRest(__VLS_132));
        let __VLS_135;
        let __VLS_136;
        let __VLS_137;
        const __VLS_138 = {
            onClose: (...[$event]) => {
                if (!!(__VLS_ctx.isLoginPage))
                    return;
                if (!(__VLS_ctx.showUserProfile))
                    return;
                __VLS_ctx.showUserProfile = false;
            }
        };
        const __VLS_139 = {
            onUpdated: (__VLS_ctx.handleUserUpdated)
        };
        var __VLS_134;
    }
}
/** @type {__VLS_StyleScopedClasses['anime-bg-decor']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-floating-stars']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-star']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-star']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-star']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-star']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-sakura-container']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-sakura-petal']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-sakura-petal']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-sakura-petal']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-sakura-petal']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-sakura-petal']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-sakura-petal']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-app']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-brand']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-logo']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-brand-title']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-brand-sub']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-list']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-footer']} */ ;
/** @type {__VLS_StyleScopedClasses['gus-avatar']} */ ;
/** @type {__VLS_StyleScopedClasses['gus-info']} */ ;
/** @type {__VLS_StyleScopedClasses['gus-name']} */ ;
/** @type {__VLS_StyleScopedClasses['gus-role']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['xs']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-main']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            RouterLink: RouterLink,
            RouterView: RouterView,
            Home: Home,
            BarChart3: BarChart3,
            Users: Users,
            BookOpen: BookOpen,
            MessageCircle: MessageCircle,
            Mail: Mail,
            HelpCircle: HelpCircle,
            LogOut: LogOut,
            Bot: Bot,
            ClipboardList: ClipboardList,
            Cpu: Cpu,
            UserProfile: UserProfile,
            isLoginPage: isLoginPage,
            currentUser: currentUser,
            isAdmin: isAdmin,
            showUserProfile: showUserProfile,
            userProfile: userProfile,
            openUserProfile: openUserProfile,
            roleLabel: roleLabel,
            handleUserUpdated: handleUserUpdated,
            getAvatarUrl: getAvatarUrl,
            onAvatarError: onAvatarError,
            handleLogout: handleLogout,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
