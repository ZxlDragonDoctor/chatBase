/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed } from 'vue';
const props = defineProps();
const colorClass = computed(() => {
    if (props.color)
        return props.color;
    return '';
});
const formattedValue = computed(() => {
    if (props.value === undefined || props.value === null)
        return '—';
    const val = typeof props.value === 'string' ? parseFloat(props.value) : props.value;
    switch (props.format) {
        case 'tokens':
            return formatTokens(val);
        case 'percent':
            return `${val.toFixed(1)}%`;
        case 'raw':
            return props.value;
        default:
            return formatNumber(val);
    }
});
function formatNumber(n) {
    if (n >= 1000000)
        return `${(n / 1000000).toFixed(1)}M`;
    if (n >= 1000)
        return `${(n / 1000).toFixed(1)}K`;
    return n.toString();
}
function formatTokens(n) {
    if (n >= 1000000)
        return `${(n / 1000000).toFixed(1)}M`;
    if (n >= 1000)
        return `${(n / 1000).toFixed(1)}K`;
    return n.toString();
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-card" },
    ...{ class: ({ 'loading': __VLS_ctx.loading }) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-value" },
    ...{ class: (__VLS_ctx.colorClass) },
});
if (__VLS_ctx.loading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "anime-loader-spinner" },
    });
}
else {
    (__VLS_ctx.formattedValue);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-stat-label" },
});
(__VLS_ctx.label);
/** @type {__VLS_StyleScopedClasses['anime-stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-loader-spinner']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-stat-label']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            colorClass: colorClass,
            formattedValue: formattedValue,
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
