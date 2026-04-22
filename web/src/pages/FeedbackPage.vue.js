/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref, computed } from 'vue';
import { Star, Send, RefreshCw, CheckCircle } from 'lucide-vue-next';
import { submitFeedbackForm } from '../api/feedbackForm';
import { getOrCreateUserId } from '../lib/user';
const userId = getOrCreateUserId();
const rating = ref(0);
const feedbackType = ref('other');
const content = ref('');
const contactInfo = ref('');
const contactError = ref('');
const submitting = ref(false);
const submitted = ref(false);
const error = ref(null);
const feedbackTypes = [
    { value: 'accurate', label: '回答准确' },
    { value: 'inaccurate', label: '回答不准确' },
    { value: 'partial', label: '部分正确' },
    { value: 'off_topic', label: '偏离主题' },
    { value: 'slow', label: '响应太慢' },
    { value: 'other', label: '其他问题' },
];
const ratingText = computed(() => {
    const texts = ['', '很不满意', '不满意', '一般', '满意', '很满意'];
    return texts[rating.value] || '';
});
const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
const PHONE_REGEX = /^1[3-9]\d{9}$/;
function validateContact() {
    if (!contactInfo.value || contactInfo.value.trim() === '') {
        contactError.value = '';
        return true;
    }
    const trimmed = contactInfo.value.trim();
    if (EMAIL_REGEX.test(trimmed)) {
        contactError.value = '';
        return true;
    }
    if (PHONE_REGEX.test(trimmed)) {
        contactError.value = '';
        return true;
    }
    contactError.value = '请输入有效的邮箱或手机号';
    return false;
}
async function handleSubmit() {
    if (rating.value === 0) {
        error.value = '请选择满意度评分';
        return;
    }
    if (!validateContact()) {
        error.value = '联系方式格式不正确';
        return;
    }
    submitting.value = true;
    error.value = null;
    try {
        const result = await submitFeedbackForm({
            userId,
            rating: rating.value,
            feedbackType: feedbackType.value,
            content: content.value,
            contact: contactInfo.value.trim(),
        });
        if (result.success) {
            submitted.value = true;
        }
        else {
            error.value = result.message;
        }
    }
    catch (e) {
        error.value = e?.message || '提交失败，请稍后再试';
    }
    finally {
        submitting.value = false;
    }
}
function resetForm() {
    rating.value = 0;
    feedbackType.value = 'other';
    content.value = '';
    contactInfo.value = '';
    contactError.value = '';
    error.value = null;
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['star-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['star-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['type-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['type-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['active']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "anime-page-shell" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "anime-card" },
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
    ...{ class: "anime-card-body" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.form, __VLS_intrinsicElements.form)({
    ...{ onSubmit: (__VLS_ctx.handleSubmit) },
    ...{ class: "feedback-form" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "form-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "rating-stars" },
});
for (const [n] of __VLS_getVForSourceType((5))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.rating = n;
            } },
        key: (n),
        type: "button",
        ...{ class: "star-btn" },
        ...{ class: ({ active: __VLS_ctx.rating >= n }) },
    });
    const __VLS_0 = {}.Star;
    /** @type {[typeof __VLS_components.Star, ]} */ ;
    // @ts-ignore
    const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
        size: (24),
        fill: (__VLS_ctx.rating >= n ? '#ffb7c5' : 'none'),
    }));
    const __VLS_2 = __VLS_1({
        size: (24),
        fill: (__VLS_ctx.rating >= n ? '#ffb7c5' : 'none'),
    }, ...__VLS_functionalComponentArgsRest(__VLS_1));
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "rating-text" },
});
(__VLS_ctx.ratingText);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "form-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "feedback-types" },
});
for (const [t] of __VLS_getVForSourceType((__VLS_ctx.feedbackTypes))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.feedbackType = t.value;
            } },
        key: (t.value),
        type: "button",
        ...{ class: "type-btn" },
        ...{ class: ({ active: __VLS_ctx.feedbackType === t.value }) },
    });
    (t.label);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "form-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
    value: (__VLS_ctx.content),
    ...{ class: "anime-input feedback-textarea" },
    placeholder: "请详细描述您的反馈内容，帮助我们更好地了解您的需求...",
    rows: "6",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "form-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ onBlur: (__VLS_ctx.validateContact) },
    ...{ onInput: (...[$event]) => {
            __VLS_ctx.contactError = '';
        } },
    value: (__VLS_ctx.contactInfo),
    type: "text",
    ...{ class: "anime-input" },
    ...{ class: ({ 'input-error': __VLS_ctx.contactError }) },
    placeholder: "邮箱或手机号，方便我们回复您",
});
if (__VLS_ctx.contactError) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "field-error" },
    });
    (__VLS_ctx.contactError);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "field-hint" },
});
if (__VLS_ctx.error) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "anime-error" },
    });
    (__VLS_ctx.error);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.resetForm) },
    type: "button",
    ...{ class: "anime-btn ghost" },
});
const __VLS_4 = {}.RefreshCw;
/** @type {[typeof __VLS_components.RefreshCw, ]} */ ;
// @ts-ignore
const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
    size: (18),
}));
const __VLS_6 = __VLS_5({
    size: (18),
}, ...__VLS_functionalComponentArgsRest(__VLS_5));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    type: "submit",
    ...{ class: "anime-btn primary" },
    disabled: (__VLS_ctx.submitting || !__VLS_ctx.rating),
});
const __VLS_8 = {}.Send;
/** @type {[typeof __VLS_components.Send, ]} */ ;
// @ts-ignore
const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
    size: (18),
}));
const __VLS_10 = __VLS_9({
    size: (18),
}, ...__VLS_functionalComponentArgsRest(__VLS_9));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.submitting ? '提交中...' : '提交反馈');
if (__VLS_ctx.submitted) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "success-message" },
    });
    const __VLS_12 = {}.CheckCircle;
    /** @type {[typeof __VLS_components.CheckCircle, ]} */ ;
    // @ts-ignore
    const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
        size: (48),
        ...{ style: {} },
    }));
    const __VLS_14 = __VLS_13({
        size: (48),
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_13));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "success-title" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "success-desc" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.submitted))
                    return;
                __VLS_ctx.submitted = false;
                __VLS_ctx.resetForm();
            } },
        ...{ class: "anime-btn ghost" },
    });
}
/** @type {__VLS_StyleScopedClasses['anime-page-shell']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-header']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-title']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-desc']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card-body']} */ ;
/** @type {__VLS_StyleScopedClasses['feedback-form']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section']} */ ;
/** @type {__VLS_StyleScopedClasses['form-label']} */ ;
/** @type {__VLS_StyleScopedClasses['rating-stars']} */ ;
/** @type {__VLS_StyleScopedClasses['star-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['rating-text']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section']} */ ;
/** @type {__VLS_StyleScopedClasses['form-label']} */ ;
/** @type {__VLS_StyleScopedClasses['feedback-types']} */ ;
/** @type {__VLS_StyleScopedClasses['type-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section']} */ ;
/** @type {__VLS_StyleScopedClasses['form-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['feedback-textarea']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section']} */ ;
/** @type {__VLS_StyleScopedClasses['form-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['field-error']} */ ;
/** @type {__VLS_StyleScopedClasses['field-hint']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-error']} */ ;
/** @type {__VLS_StyleScopedClasses['form-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['success-message']} */ ;
/** @type {__VLS_StyleScopedClasses['success-title']} */ ;
/** @type {__VLS_StyleScopedClasses['success-desc']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            Star: Star,
            Send: Send,
            RefreshCw: RefreshCw,
            CheckCircle: CheckCircle,
            rating: rating,
            feedbackType: feedbackType,
            content: content,
            contactInfo: contactInfo,
            contactError: contactError,
            submitting: submitting,
            submitted: submitted,
            error: error,
            feedbackTypes: feedbackTypes,
            ratingText: ratingText,
            validateContact: validateContact,
            handleSubmit: handleSubmit,
            resetForm: resetForm,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
