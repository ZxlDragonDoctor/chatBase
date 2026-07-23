/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
const __VLS_props = defineProps();
function getSizeClass(rank) {
    if (rank <= 3)
        return 'large';
    if (rank <= 10)
        return 'medium';
    return 'small';
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-keyword-cloud" },
});
for (const [kw] of __VLS_getVForSourceType((__VLS_ctx.keywords))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        key: (kw.keyword),
        ...{ class: "anime-keyword" },
        ...{ class: (__VLS_ctx.getSizeClass(kw.rank)) },
        ...{ style: ({ opacity: 1 - (kw.rank - 1) * 0.03 }) },
    });
    (kw.keyword);
}
if (__VLS_ctx.keywords.length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-empty" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "anime-empty-text" },
    });
}
/** @type {__VLS_StyleScopedClasses['anime-keyword-cloud']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-keyword']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-empty-text']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            getSizeClass: getSizeClass,
        };
    },
    __typeProps: {},
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeProps: {},
});
; /* PartiallyEnd: #4569/main.vue */
