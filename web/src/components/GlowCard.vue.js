/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
const __VLS_props = defineProps();
const __VLS_emit = defineEmits();
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.clickable && __VLS_ctx.$emit('click');
        } },
    ...{ class: "cyber-glow-card" },
    ...{ class: ([__VLS_ctx.color, { 'clickable': __VLS_ctx.clickable }]) },
});
if (__VLS_ctx.$slots.header || __VLS_ctx.title) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "cyber-glow-card-header" },
    });
    var __VLS_0 = {};
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "cyber-glow-card-title" },
    });
    (__VLS_ctx.title);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "cyber-glow-card-body" },
});
var __VLS_2 = {};
if (__VLS_ctx.$slots.footer) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "cyber-glow-card-footer" },
    });
    var __VLS_4 = {};
}
/** @type {__VLS_StyleScopedClasses['cyber-glow-card']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-glow-card-header']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-glow-card-title']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-glow-card-body']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-glow-card-footer']} */ ;
// @ts-ignore
var __VLS_1 = __VLS_0, __VLS_3 = __VLS_2, __VLS_5 = __VLS_4;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeEmits: {},
    __typeProps: {},
});
const __VLS_component = (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeEmits: {},
    __typeProps: {},
});
export default {};
; /* PartiallyEnd: #4569/main.vue */
