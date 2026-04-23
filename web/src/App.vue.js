/// <reference types="../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed } from 'vue';
import { RouterLink, RouterView, useRouter, useRoute } from 'vue-router';
import { Home, BarChart3, Users, BookOpen, MessageCircle, Mail, HelpCircle, User, LogOut, Bot } from 'lucide-vue-next';
const router = useRouter();
const route = useRoute();
const isLoginPage = computed(() => route.path === '/login');
const currentUser = computed(() => localStorage.getItem('chatbase_user'));
function handleLogout() {
    localStorage.removeItem('chatbase_token');
    localStorage.removeItem('chatbase_user');
    router.push('/login');
}
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
if (__VLS_ctx.isLoginPage) {
    const __VLS_0 = {}.RouterView;
    /** @type {[typeof __VLS_components.RouterView, ]} */ ;
    // @ts-ignore
    const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({}));
    const __VLS_2 = __VLS_1({}, ...__VLS_functionalComponentArgsRest(__VLS_1));
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
    const __VLS_4 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
        ...{ class: "anime-nav-item" },
        to: "/console/dashboard",
        activeClass: "active",
    }));
    const __VLS_6 = __VLS_5({
        ...{ class: "anime-nav-item" },
        to: "/console/dashboard",
        activeClass: "active",
    }, ...__VLS_functionalComponentArgsRest(__VLS_5));
    __VLS_7.slots.default;
    const __VLS_8 = {}.Home;
    /** @type {[typeof __VLS_components.Home, ]} */ ;
    // @ts-ignore
    const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
        ...{ class: "anime-nav-icon" },
        size: (22),
    }));
    const __VLS_10 = __VLS_9({
        ...{ class: "anime-nav-icon" },
        size: (22),
    }, ...__VLS_functionalComponentArgsRest(__VLS_9));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    var __VLS_7;
    const __VLS_12 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
        ...{ class: "anime-nav-item" },
        to: "/console/statistics",
        activeClass: "active",
    }));
    const __VLS_14 = __VLS_13({
        ...{ class: "anime-nav-item" },
        to: "/console/statistics",
        activeClass: "active",
    }, ...__VLS_functionalComponentArgsRest(__VLS_13));
    __VLS_15.slots.default;
    const __VLS_16 = {}.BarChart3;
    /** @type {[typeof __VLS_components.BarChart3, ]} */ ;
    // @ts-ignore
    const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
        ...{ class: "anime-nav-icon" },
        size: (22),
    }));
    const __VLS_18 = __VLS_17({
        ...{ class: "anime-nav-icon" },
        size: (22),
    }, ...__VLS_functionalComponentArgsRest(__VLS_17));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    var __VLS_15;
    const __VLS_20 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
        ...{ class: "anime-nav-item" },
        to: "/console/im",
        activeClass: "active",
    }));
    const __VLS_22 = __VLS_21({
        ...{ class: "anime-nav-item" },
        to: "/console/im",
        activeClass: "active",
    }, ...__VLS_functionalComponentArgsRest(__VLS_21));
    __VLS_23.slots.default;
    const __VLS_24 = {}.Users;
    /** @type {[typeof __VLS_components.Users, ]} */ ;
    // @ts-ignore
    const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
        ...{ class: "anime-nav-icon" },
        size: (22),
    }));
    const __VLS_26 = __VLS_25({
        ...{ class: "anime-nav-icon" },
        size: (22),
    }, ...__VLS_functionalComponentArgsRest(__VLS_25));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    var __VLS_23;
    const __VLS_28 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
        ...{ class: "anime-nav-item" },
        to: "/console/knowledge",
        activeClass: "active",
    }));
    const __VLS_30 = __VLS_29({
        ...{ class: "anime-nav-item" },
        to: "/console/knowledge",
        activeClass: "active",
    }, ...__VLS_functionalComponentArgsRest(__VLS_29));
    __VLS_31.slots.default;
    const __VLS_32 = {}.BookOpen;
    /** @type {[typeof __VLS_components.BookOpen, ]} */ ;
    // @ts-ignore
    const __VLS_33 = __VLS_asFunctionalComponent(__VLS_32, new __VLS_32({
        ...{ class: "anime-nav-icon" },
        size: (22),
    }));
    const __VLS_34 = __VLS_33({
        ...{ class: "anime-nav-icon" },
        size: (22),
    }, ...__VLS_functionalComponentArgsRest(__VLS_33));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    var __VLS_31;
    const __VLS_36 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_37 = __VLS_asFunctionalComponent(__VLS_36, new __VLS_36({
        ...{ class: "anime-nav-item" },
        to: "/console/app",
        activeClass: "active",
    }));
    const __VLS_38 = __VLS_37({
        ...{ class: "anime-nav-item" },
        to: "/console/app",
        activeClass: "active",
    }, ...__VLS_functionalComponentArgsRest(__VLS_37));
    __VLS_39.slots.default;
    const __VLS_40 = {}.Bot;
    /** @type {[typeof __VLS_components.Bot, ]} */ ;
    // @ts-ignore
    const __VLS_41 = __VLS_asFunctionalComponent(__VLS_40, new __VLS_40({
        ...{ class: "anime-nav-icon" },
        size: (22),
    }));
    const __VLS_42 = __VLS_41({
        ...{ class: "anime-nav-icon" },
        size: (22),
    }, ...__VLS_functionalComponentArgsRest(__VLS_41));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    var __VLS_39;
    const __VLS_44 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_45 = __VLS_asFunctionalComponent(__VLS_44, new __VLS_44({
        ...{ class: "anime-nav-item" },
        to: "/console/faq",
        activeClass: "active",
    }));
    const __VLS_46 = __VLS_45({
        ...{ class: "anime-nav-item" },
        to: "/console/faq",
        activeClass: "active",
    }, ...__VLS_functionalComponentArgsRest(__VLS_45));
    __VLS_47.slots.default;
    const __VLS_48 = {}.HelpCircle;
    /** @type {[typeof __VLS_components.HelpCircle, ]} */ ;
    // @ts-ignore
    const __VLS_49 = __VLS_asFunctionalComponent(__VLS_48, new __VLS_48({
        ...{ class: "anime-nav-icon" },
        size: (22),
    }));
    const __VLS_50 = __VLS_49({
        ...{ class: "anime-nav-icon" },
        size: (22),
    }, ...__VLS_functionalComponentArgsRest(__VLS_49));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    var __VLS_47;
    const __VLS_52 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_53 = __VLS_asFunctionalComponent(__VLS_52, new __VLS_52({
        ...{ class: "anime-nav-item" },
        to: "/chat",
        activeClass: "active",
    }));
    const __VLS_54 = __VLS_53({
        ...{ class: "anime-nav-item" },
        to: "/chat",
        activeClass: "active",
    }, ...__VLS_functionalComponentArgsRest(__VLS_53));
    __VLS_55.slots.default;
    const __VLS_56 = {}.MessageCircle;
    /** @type {[typeof __VLS_components.MessageCircle, ]} */ ;
    // @ts-ignore
    const __VLS_57 = __VLS_asFunctionalComponent(__VLS_56, new __VLS_56({
        ...{ class: "anime-nav-icon" },
        size: (22),
    }));
    const __VLS_58 = __VLS_57({
        ...{ class: "anime-nav-icon" },
        size: (22),
    }, ...__VLS_functionalComponentArgsRest(__VLS_57));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    var __VLS_55;
    const __VLS_60 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_61 = __VLS_asFunctionalComponent(__VLS_60, new __VLS_60({
        ...{ class: "anime-nav-item" },
        to: "/feedback",
        activeClass: "active",
    }));
    const __VLS_62 = __VLS_61({
        ...{ class: "anime-nav-item" },
        to: "/feedback",
        activeClass: "active",
    }, ...__VLS_functionalComponentArgsRest(__VLS_61));
    __VLS_63.slots.default;
    const __VLS_64 = {}.Mail;
    /** @type {[typeof __VLS_components.Mail, ]} */ ;
    // @ts-ignore
    const __VLS_65 = __VLS_asFunctionalComponent(__VLS_64, new __VLS_64({
        ...{ class: "anime-nav-icon" },
        size: (22),
    }));
    const __VLS_66 = __VLS_65({
        ...{ class: "anime-nav-icon" },
        size: (22),
    }, ...__VLS_functionalComponentArgsRest(__VLS_65));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    var __VLS_63;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-nav-footer" },
    });
    if (__VLS_ctx.currentUser) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "anime-user-info" },
        });
        const __VLS_68 = {}.User;
        /** @type {[typeof __VLS_components.User, ]} */ ;
        // @ts-ignore
        const __VLS_69 = __VLS_asFunctionalComponent(__VLS_68, new __VLS_68({
            ...{ class: "anime-nav-icon" },
            size: (16),
        }));
        const __VLS_70 = __VLS_69({
            ...{ class: "anime-nav-icon" },
            size: (16),
        }, ...__VLS_functionalComponentArgsRest(__VLS_69));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-user-name" },
        });
        (__VLS_ctx.currentUser);
    }
    if (__VLS_ctx.currentUser) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (__VLS_ctx.handleLogout) },
            ...{ class: "anime-btn ghost sm" },
        });
        const __VLS_72 = {}.LogOut;
        /** @type {[typeof __VLS_components.LogOut, ]} */ ;
        // @ts-ignore
        const __VLS_73 = __VLS_asFunctionalComponent(__VLS_72, new __VLS_72({
            size: (16),
        }));
        const __VLS_74 = __VLS_73({
            size: (16),
        }, ...__VLS_functionalComponentArgsRest(__VLS_73));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-status-badge" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "anime-status-dot" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.main, __VLS_intrinsicElements.main)({
        ...{ class: "anime-main" },
    });
    const __VLS_76 = {}.RouterView;
    /** @type {[typeof __VLS_components.RouterView, ]} */ ;
    // @ts-ignore
    const __VLS_77 = __VLS_asFunctionalComponent(__VLS_76, new __VLS_76({}));
    const __VLS_78 = __VLS_77({}, ...__VLS_functionalComponentArgsRest(__VLS_77));
}
/** @type {__VLS_StyleScopedClasses['anime-bg-decor']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-floating-stars']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-star']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-star']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-star']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-star']} */ ;
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
/** @type {__VLS_StyleScopedClasses['anime-nav-footer']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-user-info']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-user-name']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['sm']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-status-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-status-dot']} */ ;
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
            User: User,
            LogOut: LogOut,
            Bot: Bot,
            isLoginPage: isLoginPage,
            currentUser: currentUser,
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
