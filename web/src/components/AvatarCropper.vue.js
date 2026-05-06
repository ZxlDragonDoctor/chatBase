/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref, watch, onMounted, onBeforeUnmount } from 'vue';
import Cropper from 'cropperjs';
import 'cropperjs/dist/cropper.css';
const props = defineProps();
const emit = defineEmits();
const containerRef = ref();
const imageRef = ref();
const previewRef = ref();
const processing = ref(false);
let cropper = null;
function initCropper() {
    destroyCropper();
    if (!imageRef.value || !containerRef.value) {
        return;
    }
    setTimeout(() => {
        if (!imageRef.value)
            return;
        try {
            cropper = new Cropper(imageRef.value, {
                aspectRatio: 1,
                viewMode: 1,
                dragMode: 'move',
                autoCropArea: 0.8,
                responsive: true,
                guides: true,
                center: true,
                highlight: true,
                cropBoxMovable: true,
                cropBoxResizable: true,
                toggleDragModeOnDblclick: false,
                ready() {
                    updatePreview();
                },
                crop() {
                    updatePreview();
                }
            });
        }
        catch (e) {
            console.error('Cropper init error:', e);
        }
    }, 100);
}
function updatePreview() {
    if (!cropper || !previewRef.value)
        return;
    try {
        const canvas = cropper.getCroppedCanvas({
            width: 200,
            height: 200,
            imageSmoothingEnabled: true,
            imageSmoothingQuality: 'high'
        });
        if (!canvas)
            return;
        const ctx = previewRef.value.getContext('2d');
        if (!ctx)
            return;
        ctx.clearRect(0, 0, 150, 150);
        ctx.save();
        ctx.beginPath();
        ctx.arc(75, 75, 75, 0, Math.PI * 2, true);
        ctx.closePath();
        ctx.clip();
        ctx.drawImage(canvas, 0, 0, 150, 150);
        ctx.restore();
    }
    catch (e) {
        console.error('Preview error:', e);
    }
}
function rotate(deg) {
    cropper?.rotate(deg);
}
function doZoom(ratio) {
    cropper?.zoom(ratio);
}
function doReset() {
    cropper?.reset();
}
function doConfirm() {
    if (!cropper || processing.value)
        return;
    processing.value = true;
    try {
        const canvas = cropper.getCroppedCanvas({
            width: 200,
            height: 200,
            imageSmoothingEnabled: true,
            imageSmoothingQuality: 'high'
        });
        if (!canvas) {
            processing.value = false;
            return;
        }
        canvas.toBlob((blob) => {
            if (blob) {
                emit('confirm', blob);
            }
            processing.value = false;
        }, 'image/jpeg', 0.9);
    }
    catch (e) {
        console.error('Confirm error:', e);
        processing.value = false;
    }
}
function destroyCropper() {
    if (cropper) {
        cropper.destroy();
        cropper = null;
    }
}
onMounted(() => {
    if (props.visible) {
        initCropper();
    }
});
watch(() => props.visible, (val) => {
    if (val) {
        initCropper();
    }
    else {
        destroyCropper();
    }
});
watch(() => props.imageUrl, () => {
    if (props.visible) {
        initCropper();
    }
});
onBeforeUnmount(() => {
    destroyCropper();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
// CSS variable injection 
// CSS variable injection end 
const __VLS_0 = {}.Teleport;
/** @type {[typeof __VLS_components.Teleport, typeof __VLS_components.Teleport, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    to: "body",
}));
const __VLS_2 = __VLS_1({
    to: "body",
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_3.slots.default;
const __VLS_4 = {}.Transition;
/** @type {[typeof __VLS_components.Transition, typeof __VLS_components.Transition, ]} */ ;
// @ts-ignore
const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
    name: "fade",
}));
const __VLS_6 = __VLS_5({
    name: "fade",
}, ...__VLS_functionalComponentArgsRest(__VLS_5));
__VLS_7.slots.default;
if (__VLS_ctx.visible) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.visible))
                    return;
                __VLS_ctx.$emit('cancel');
            } },
        ...{ class: "ac-overlay" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "ac-modal anime-card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "ac-header" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "ac-title" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.visible))
                    return;
                __VLS_ctx.$emit('cancel');
            } },
        ...{ class: "ac-close" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "ac-body" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "ac-area" },
        ref: "containerRef",
    });
    /** @type {typeof __VLS_ctx.containerRef} */ ;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.img)({
        ref: "imageRef",
        src: (__VLS_ctx.imageUrl),
        alt: "avatar",
    });
    /** @type {typeof __VLS_ctx.imageRef} */ ;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "ac-preview-panel" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "ac-preview-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "ac-preview-circle" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.canvas, __VLS_intrinsicElements.canvas)({
        ref: "previewRef",
        width: "150",
        height: "150",
    });
    /** @type {typeof __VLS_ctx.previewRef} */ ;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "ac-toolbar" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.visible))
                    return;
                __VLS_ctx.rotate(-90);
            } },
        ...{ class: "ac-tbtn" },
        title: "左旋90°",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "ac-tlabel" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.visible))
                    return;
                __VLS_ctx.rotate(90);
            } },
        ...{ class: "ac-tbtn" },
        title: "右旋90°",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "ac-tlabel" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.visible))
                    return;
                __VLS_ctx.doZoom(0.1);
            } },
        ...{ class: "ac-tbtn" },
        title: "放大",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "ac-tlabel" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.visible))
                    return;
                __VLS_ctx.doZoom(-0.1);
            } },
        ...{ class: "ac-tbtn" },
        title: "缩小",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "ac-tlabel" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.doReset) },
        ...{ class: "ac-tbtn" },
        title: "重置",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "ac-tlabel" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "ac-footer" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.visible))
                    return;
                __VLS_ctx.$emit('cancel');
            } },
        ...{ class: "anime-btn ghost" },
        disabled: (__VLS_ctx.processing),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.doConfirm) },
        ...{ class: "anime-btn primary" },
        disabled: (__VLS_ctx.processing),
    });
    if (__VLS_ctx.processing) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-loader-spinner" },
            ...{ style: {} },
        });
    }
    else {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    }
}
var __VLS_7;
var __VLS_3;
/** @type {__VLS_StyleScopedClasses['ac-overlay']} */ ;
/** @type {__VLS_StyleScopedClasses['ac-modal']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card']} */ ;
/** @type {__VLS_StyleScopedClasses['ac-header']} */ ;
/** @type {__VLS_StyleScopedClasses['ac-title']} */ ;
/** @type {__VLS_StyleScopedClasses['ac-close']} */ ;
/** @type {__VLS_StyleScopedClasses['ac-body']} */ ;
/** @type {__VLS_StyleScopedClasses['ac-area']} */ ;
/** @type {__VLS_StyleScopedClasses['ac-preview-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['ac-preview-label']} */ ;
/** @type {__VLS_StyleScopedClasses['ac-preview-circle']} */ ;
/** @type {__VLS_StyleScopedClasses['ac-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['ac-tbtn']} */ ;
/** @type {__VLS_StyleScopedClasses['ac-tlabel']} */ ;
/** @type {__VLS_StyleScopedClasses['ac-tbtn']} */ ;
/** @type {__VLS_StyleScopedClasses['ac-tlabel']} */ ;
/** @type {__VLS_StyleScopedClasses['ac-tbtn']} */ ;
/** @type {__VLS_StyleScopedClasses['ac-tlabel']} */ ;
/** @type {__VLS_StyleScopedClasses['ac-tbtn']} */ ;
/** @type {__VLS_StyleScopedClasses['ac-tlabel']} */ ;
/** @type {__VLS_StyleScopedClasses['ac-tbtn']} */ ;
/** @type {__VLS_StyleScopedClasses['ac-tlabel']} */ ;
/** @type {__VLS_StyleScopedClasses['ac-footer']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-loader-spinner']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            containerRef: containerRef,
            imageRef: imageRef,
            previewRef: previewRef,
            processing: processing,
            rotate: rotate,
            doZoom: doZoom,
            doReset: doReset,
            doConfirm: doConfirm,
        };
    },
    __typeEmits: {},
    __typeProps: {},
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeEmits: {},
    __typeProps: {},
});
; /* PartiallyEnd: #4569/main.vue */
