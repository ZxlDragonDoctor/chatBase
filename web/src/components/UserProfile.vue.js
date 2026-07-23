/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref, computed, watch } from 'vue';
import { User, Hash, Mail, Phone, Calendar, Clock, Edit, Key, Camera } from 'lucide-vue-next';
import { updateUserProfile, changePassword, uploadAvatar } from '../api/user';
import AvatarCropper from './AvatarCropper.vue';
const props = defineProps();
const emit = defineEmits();
const showEditModal = ref(false);
const showPasswordModal = ref(false);
const showCropper = ref(false);
const selectedImageUrl = ref('');
const editLoading = ref(false);
const editError = ref(null);
const editSuccess = ref(null);
const passwordLoading = ref(false);
const passwordError = ref(null);
const passwordSuccess = ref(null);
const avatarUploading = ref(false);
const editForm = ref({
    nickname: '',
    email: '',
    phone: ''
});
const passwordForm = ref({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
});
const roleLabel = computed(() => {
    switch (props.user?.role) {
        case 'admin': return '管理员';
        case 'user': return '普通用户';
        default: return '访客';
    }
});
const roleBadgeClass = computed(() => {
    switch (props.user?.role) {
        case 'admin': return 'pink';
        case 'user': return 'blue';
        default: return 'muted';
    }
});
function maskPhone(phone) {
    if (!phone || phone.length < 7)
        return phone;
    return phone.slice(0, 3) + '****' + phone.slice(-4);
}
function formatDate(date) {
    if (!date)
        return '-';
    const d = new Date(date);
    return d.toLocaleString('zh-CN', {
        year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit'
    });
}
function getAvatarUrl(path) {
    if (!path)
        return '';
    if (path.startsWith('http'))
        return path;
    return `/api${path}`;
}
function isValidEmail(email) {
    if (!email)
        return true;
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}
function isValidPhone(phone) {
    if (!phone)
        return true;
    return /^1[3-9]\d{9}$/.test(phone);
}
function openCropper() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/jpeg,image/png,image/gif,image/webp';
    input.onchange = (e) => {
        const file = e.target.files?.[0];
        if (file) {
            selectedImageUrl.value = URL.createObjectURL(file);
            showCropper.value = true;
        }
    };
    input.click();
}
async function handleAvatarCrop(croppedBlob) {
    const file = new File([croppedBlob], 'avatar.jpg', { type: 'image/jpeg' });
    avatarUploading.value = true;
    editError.value = null;
    try {
        const resp = await uploadAvatar(props.user?.username || '', file);
        if (resp.success && resp.user) {
            emit('updated', resp.user);
            editSuccess.value = '头像上传成功';
            setTimeout(() => { editSuccess.value = null; }, 2000);
        }
        else {
            editError.value = resp.message || '头像上传失败';
        }
    }
    catch (e) {
        editError.value = e?.response?.data?.message || '头像上传失败';
    }
    finally {
        avatarUploading.value = false;
        showCropper.value = false;
    }
}
watch(() => props.user, (u) => {
    if (u) {
        editForm.value = {
            nickname: u.nickname || '',
            email: u.email || '',
            phone: u.phone || ''
        };
    }
}, { immediate: true });
async function handleUpdateProfile() {
    const username = props.user?.username;
    if (!username) {
        editError.value = '用户名不存在';
        return;
    }
    // Validate email
    if (editForm.value.email && !isValidEmail(editForm.value.email)) {
        editError.value = '邮箱格式不正确';
        return;
    }
    // Validate phone
    if (editForm.value.phone && !isValidPhone(editForm.value.phone)) {
        editError.value = '手机号格式不正确（11位数字）';
        return;
    }
    editLoading.value = true;
    editError.value = null;
    editSuccess.value = null;
    try {
        const resp = await updateUserProfile(username, editForm.value);
        if (resp.success === true && resp.user) {
            editSuccess.value = '资料修改成功';
            setTimeout(() => {
                showEditModal.value = false;
                emit('updated', resp.user);
            }, 1000);
        }
        else {
            editError.value = resp.message || '修改失败';
        }
    }
    catch (e) {
        editError.value = e?.response?.data?.message || e?.message || '修改失败，请重试';
    }
    finally {
        editLoading.value = false;
    }
}
async function handleChangePassword() {
    if (!passwordForm.value.oldPassword) {
        passwordError.value = '请输入当前密码';
        return;
    }
    if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
        passwordError.value = '两次输入的密码不一致';
        return;
    }
    if (passwordForm.value.newPassword.length < 6) {
        passwordError.value = '新密码长度不能少于6位';
        return;
    }
    passwordLoading.value = true;
    passwordError.value = null;
    passwordSuccess.value = null;
    try {
        const resp = await changePassword(props.user?.username || '', passwordForm.value.oldPassword, passwordForm.value.newPassword);
        if (resp.success) {
            passwordSuccess.value = '密码修改成功';
            passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' };
            setTimeout(() => { showPasswordModal.value = false; }, 1500);
        }
        else {
            // Backend returns "原密码不正确" when old password is wrong
            passwordError.value = resp.message || '原密码不正确';
        }
    }
    catch (e) {
        const errMsg = e?.response?.data?.message || e?.message || '密码修改失败';
        // Check if error message indicates wrong old password
        if (errMsg.includes('原密码') || errMsg.includes('密码错误')) {
            passwordError.value = '原密码不正确';
        }
        else {
            passwordError.value = errMsg;
        }
    }
    finally {
        passwordLoading.value = false;
    }
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['user-avatar-large']} */ ;
/** @type {__VLS_StyleScopedClasses['avatar-upload-overlay']} */ ;
/** @type {__VLS_StyleScopedClasses['status-dot']} */ ;
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
if (__VLS_ctx.show) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.show))
                    return;
                __VLS_ctx.$emit('close');
            } },
        ...{ class: "user-profile-overlay" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onClick: () => { } },
        ...{ class: "user-profile-card anime-card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "user-profile-header" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "user-profile-title" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.show))
                    return;
                __VLS_ctx.$emit('close');
            } },
        ...{ class: "user-profile-close" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "user-profile-body" },
    });
    if (!__VLS_ctx.user) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "user-loading-state" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-loader-spinner" },
            ...{ style: {} },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    }
    else {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "user-avatar-section" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ onClick: (__VLS_ctx.openCropper) },
            ...{ class: "user-avatar-large" },
        });
        if (__VLS_ctx.user.avatar) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.img)({
                src: (__VLS_ctx.getAvatarUrl(__VLS_ctx.user.avatar)),
                alt: "头像",
                ...{ class: "avatar-img" },
            });
        }
        else {
            if (__VLS_ctx.user.nickname) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
                (__VLS_ctx.user.nickname.charAt(0).toUpperCase());
            }
            else {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
                ((__VLS_ctx.user.username || 'U').charAt(0).toUpperCase());
            }
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "avatar-upload-overlay" },
        });
        const __VLS_8 = {}.Camera;
        /** @type {[typeof __VLS_components.Camera, ]} */ ;
        // @ts-ignore
        const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
            size: (20),
        }));
        const __VLS_10 = __VLS_9({
            size: (20),
        }, ...__VLS_functionalComponentArgsRest(__VLS_9));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "user-avatar-info" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "user-display-name" },
        });
        (__VLS_ctx.user.nickname || __VLS_ctx.user.username || '未知用户');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "anime-badge" },
            ...{ class: (__VLS_ctx.roleBadgeClass) },
        });
        (__VLS_ctx.roleLabel);
        if (__VLS_ctx.avatarUploading) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "avatar-upload-hint" },
            });
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "user-info-grid" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "user-info-item" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "user-info-label" },
        });
        const __VLS_12 = {}.User;
        /** @type {[typeof __VLS_components.User, ]} */ ;
        // @ts-ignore
        const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
            size: (14),
        }));
        const __VLS_14 = __VLS_13({
            size: (14),
        }, ...__VLS_functionalComponentArgsRest(__VLS_13));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "user-info-value" },
        });
        (__VLS_ctx.user.username || '-');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "user-info-item" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "user-info-label" },
        });
        const __VLS_16 = {}.Hash;
        /** @type {[typeof __VLS_components.Hash, ]} */ ;
        // @ts-ignore
        const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
            size: (14),
        }));
        const __VLS_18 = __VLS_17({
            size: (14),
        }, ...__VLS_functionalComponentArgsRest(__VLS_17));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "user-info-value user-info-code" },
        });
        (__VLS_ctx.user.id || '-');
        if (__VLS_ctx.user.email) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "user-info-item" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "user-info-label" },
            });
            const __VLS_20 = {}.Mail;
            /** @type {[typeof __VLS_components.Mail, ]} */ ;
            // @ts-ignore
            const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
                size: (14),
            }));
            const __VLS_22 = __VLS_21({
                size: (14),
            }, ...__VLS_functionalComponentArgsRest(__VLS_21));
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "user-info-value" },
            });
            (__VLS_ctx.user.email);
        }
        if (__VLS_ctx.user.phone) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "user-info-item" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "user-info-label" },
            });
            const __VLS_24 = {}.Phone;
            /** @type {[typeof __VLS_components.Phone, ]} */ ;
            // @ts-ignore
            const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
                size: (14),
            }));
            const __VLS_26 = __VLS_25({
                size: (14),
            }, ...__VLS_functionalComponentArgsRest(__VLS_25));
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "user-info-value" },
            });
            (__VLS_ctx.maskPhone(__VLS_ctx.user.phone));
        }
        if (__VLS_ctx.user.createTime) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "user-info-item" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "user-info-label" },
            });
            const __VLS_28 = {}.Calendar;
            /** @type {[typeof __VLS_components.Calendar, ]} */ ;
            // @ts-ignore
            const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
                size: (14),
            }));
            const __VLS_30 = __VLS_29({
                size: (14),
            }, ...__VLS_functionalComponentArgsRest(__VLS_29));
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "user-info-value" },
            });
            (__VLS_ctx.formatDate(__VLS_ctx.user.createTime));
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "user-info-item" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "user-info-label" },
        });
        const __VLS_32 = {}.Clock;
        /** @type {[typeof __VLS_components.Clock, ]} */ ;
        // @ts-ignore
        const __VLS_33 = __VLS_asFunctionalComponent(__VLS_32, new __VLS_32({
            size: (14),
        }));
        const __VLS_34 = __VLS_33({
            size: (14),
        }, ...__VLS_functionalComponentArgsRest(__VLS_33));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "user-info-value user-online-status" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "status-dot online" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "user-profile-actions" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(__VLS_ctx.show))
                        return;
                    if (!!(!__VLS_ctx.user))
                        return;
                    __VLS_ctx.showEditModal = true;
                } },
            ...{ class: "anime-btn primary" },
        });
        const __VLS_36 = {}.Edit;
        /** @type {[typeof __VLS_components.Edit, ]} */ ;
        // @ts-ignore
        const __VLS_37 = __VLS_asFunctionalComponent(__VLS_36, new __VLS_36({
            size: (16),
        }));
        const __VLS_38 = __VLS_37({
            size: (16),
        }, ...__VLS_functionalComponentArgsRest(__VLS_37));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(__VLS_ctx.show))
                        return;
                    if (!!(!__VLS_ctx.user))
                        return;
                    __VLS_ctx.showPasswordModal = true;
                } },
            ...{ class: "anime-btn ghost" },
        });
        const __VLS_40 = {}.Key;
        /** @type {[typeof __VLS_components.Key, ]} */ ;
        // @ts-ignore
        const __VLS_41 = __VLS_asFunctionalComponent(__VLS_40, new __VLS_40({
            size: (16),
        }));
        const __VLS_42 = __VLS_41({
            size: (16),
        }, ...__VLS_functionalComponentArgsRest(__VLS_41));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    }
    if (__VLS_ctx.showEditModal) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ onClick: (...[$event]) => {
                    if (!(__VLS_ctx.show))
                        return;
                    if (!(__VLS_ctx.showEditModal))
                        return;
                    __VLS_ctx.showEditModal = false;
                } },
            ...{ class: "edit-modal-overlay" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "edit-modal anime-card" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "edit-modal-header" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "edit-modal-title" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(__VLS_ctx.show))
                        return;
                    if (!(__VLS_ctx.showEditModal))
                        return;
                    __VLS_ctx.showEditModal = false;
                } },
            ...{ class: "edit-modal-close" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "edit-modal-body" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "form-group" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
            ...{ class: "form-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
            ...{ class: "anime-input" },
            placeholder: "请输入昵称",
        });
        (__VLS_ctx.editForm.nickname);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "form-group" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
            ...{ class: "form-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
            ...{ class: "anime-input" },
            type: "email",
            placeholder: "请输入邮箱",
        });
        (__VLS_ctx.editForm.email);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "form-group" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
            ...{ class: "form-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
            ...{ class: "anime-input" },
            placeholder: "请输入手机号",
        });
        (__VLS_ctx.editForm.phone);
        if (__VLS_ctx.editError) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "anime-error" },
                ...{ style: {} },
            });
            (__VLS_ctx.editError);
        }
        if (__VLS_ctx.editSuccess) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "anime-success" },
                ...{ style: {} },
            });
            (__VLS_ctx.editSuccess);
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "edit-modal-footer" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (__VLS_ctx.handleUpdateProfile) },
            ...{ class: "anime-btn primary" },
            disabled: (__VLS_ctx.editLoading),
        });
        if (__VLS_ctx.editLoading) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-loader-spinner" },
                ...{ style: {} },
            });
        }
        else {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(__VLS_ctx.show))
                        return;
                    if (!(__VLS_ctx.showEditModal))
                        return;
                    __VLS_ctx.showEditModal = false;
                } },
            ...{ class: "anime-btn ghost" },
        });
    }
    if (__VLS_ctx.showPasswordModal) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ onClick: (...[$event]) => {
                    if (!(__VLS_ctx.show))
                        return;
                    if (!(__VLS_ctx.showPasswordModal))
                        return;
                    __VLS_ctx.showPasswordModal = false;
                } },
            ...{ class: "edit-modal-overlay" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "edit-modal anime-card" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "edit-modal-header" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "edit-modal-title" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(__VLS_ctx.show))
                        return;
                    if (!(__VLS_ctx.showPasswordModal))
                        return;
                    __VLS_ctx.showPasswordModal = false;
                } },
            ...{ class: "edit-modal-close" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "edit-modal-body" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "form-group" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
            ...{ class: "form-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
            ...{ class: "anime-input" },
            type: "password",
            placeholder: "请输入当前密码",
        });
        (__VLS_ctx.passwordForm.oldPassword);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "form-group" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
            ...{ class: "form-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
            ...{ class: "anime-input" },
            type: "password",
            placeholder: "请输入新密码",
        });
        (__VLS_ctx.passwordForm.newPassword);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "form-group" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
            ...{ class: "form-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
            ...{ class: "anime-input" },
            type: "password",
            placeholder: "请再次输入新密码",
        });
        (__VLS_ctx.passwordForm.confirmPassword);
        if (__VLS_ctx.passwordError) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "anime-error" },
                ...{ style: {} },
            });
            (__VLS_ctx.passwordError);
        }
        if (__VLS_ctx.passwordSuccess) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "anime-success" },
                ...{ style: {} },
            });
            (__VLS_ctx.passwordSuccess);
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "edit-modal-footer" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (__VLS_ctx.handleChangePassword) },
            ...{ class: "anime-btn primary" },
            disabled: (__VLS_ctx.passwordLoading),
        });
        if (__VLS_ctx.passwordLoading) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "anime-loader-spinner" },
                ...{ style: {} },
            });
        }
        else {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(__VLS_ctx.show))
                        return;
                    if (!(__VLS_ctx.showPasswordModal))
                        return;
                    __VLS_ctx.showPasswordModal = false;
                } },
            ...{ class: "anime-btn ghost" },
        });
    }
}
var __VLS_7;
if (__VLS_ctx.showCropper) {
    /** @type {[typeof AvatarCropper, ]} */ ;
    // @ts-ignore
    const __VLS_44 = __VLS_asFunctionalComponent(AvatarCropper, new AvatarCropper({
        ...{ 'onConfirm': {} },
        ...{ 'onCancel': {} },
        imageUrl: (__VLS_ctx.selectedImageUrl),
        visible: (__VLS_ctx.showCropper),
    }));
    const __VLS_45 = __VLS_44({
        ...{ 'onConfirm': {} },
        ...{ 'onCancel': {} },
        imageUrl: (__VLS_ctx.selectedImageUrl),
        visible: (__VLS_ctx.showCropper),
    }, ...__VLS_functionalComponentArgsRest(__VLS_44));
    let __VLS_47;
    let __VLS_48;
    let __VLS_49;
    const __VLS_50 = {
        onConfirm: (__VLS_ctx.handleAvatarCrop)
    };
    const __VLS_51 = {
        onCancel: (...[$event]) => {
            if (!(__VLS_ctx.showCropper))
                return;
            __VLS_ctx.showCropper = false;
        }
    };
    var __VLS_46;
}
var __VLS_3;
/** @type {__VLS_StyleScopedClasses['user-profile-overlay']} */ ;
/** @type {__VLS_StyleScopedClasses['user-profile-card']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card']} */ ;
/** @type {__VLS_StyleScopedClasses['user-profile-header']} */ ;
/** @type {__VLS_StyleScopedClasses['user-profile-title']} */ ;
/** @type {__VLS_StyleScopedClasses['user-profile-close']} */ ;
/** @type {__VLS_StyleScopedClasses['user-profile-body']} */ ;
/** @type {__VLS_StyleScopedClasses['user-loading-state']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-loader-spinner']} */ ;
/** @type {__VLS_StyleScopedClasses['user-avatar-section']} */ ;
/** @type {__VLS_StyleScopedClasses['user-avatar-large']} */ ;
/** @type {__VLS_StyleScopedClasses['avatar-img']} */ ;
/** @type {__VLS_StyleScopedClasses['avatar-upload-overlay']} */ ;
/** @type {__VLS_StyleScopedClasses['user-avatar-info']} */ ;
/** @type {__VLS_StyleScopedClasses['user-display-name']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['avatar-upload-hint']} */ ;
/** @type {__VLS_StyleScopedClasses['user-info-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['user-info-item']} */ ;
/** @type {__VLS_StyleScopedClasses['user-info-label']} */ ;
/** @type {__VLS_StyleScopedClasses['user-info-value']} */ ;
/** @type {__VLS_StyleScopedClasses['user-info-item']} */ ;
/** @type {__VLS_StyleScopedClasses['user-info-label']} */ ;
/** @type {__VLS_StyleScopedClasses['user-info-value']} */ ;
/** @type {__VLS_StyleScopedClasses['user-info-code']} */ ;
/** @type {__VLS_StyleScopedClasses['user-info-item']} */ ;
/** @type {__VLS_StyleScopedClasses['user-info-label']} */ ;
/** @type {__VLS_StyleScopedClasses['user-info-value']} */ ;
/** @type {__VLS_StyleScopedClasses['user-info-item']} */ ;
/** @type {__VLS_StyleScopedClasses['user-info-label']} */ ;
/** @type {__VLS_StyleScopedClasses['user-info-value']} */ ;
/** @type {__VLS_StyleScopedClasses['user-info-item']} */ ;
/** @type {__VLS_StyleScopedClasses['user-info-label']} */ ;
/** @type {__VLS_StyleScopedClasses['user-info-value']} */ ;
/** @type {__VLS_StyleScopedClasses['user-info-item']} */ ;
/** @type {__VLS_StyleScopedClasses['user-info-label']} */ ;
/** @type {__VLS_StyleScopedClasses['user-info-value']} */ ;
/** @type {__VLS_StyleScopedClasses['user-online-status']} */ ;
/** @type {__VLS_StyleScopedClasses['status-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['online']} */ ;
/** @type {__VLS_StyleScopedClasses['user-profile-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['edit-modal-overlay']} */ ;
/** @type {__VLS_StyleScopedClasses['edit-modal']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card']} */ ;
/** @type {__VLS_StyleScopedClasses['edit-modal-header']} */ ;
/** @type {__VLS_StyleScopedClasses['edit-modal-title']} */ ;
/** @type {__VLS_StyleScopedClasses['edit-modal-close']} */ ;
/** @type {__VLS_StyleScopedClasses['edit-modal-body']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-error']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-success']} */ ;
/** @type {__VLS_StyleScopedClasses['edit-modal-footer']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-loader-spinner']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['edit-modal-overlay']} */ ;
/** @type {__VLS_StyleScopedClasses['edit-modal']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-card']} */ ;
/** @type {__VLS_StyleScopedClasses['edit-modal-header']} */ ;
/** @type {__VLS_StyleScopedClasses['edit-modal-title']} */ ;
/** @type {__VLS_StyleScopedClasses['edit-modal-close']} */ ;
/** @type {__VLS_StyleScopedClasses['edit-modal-body']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-label']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-input']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-error']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-success']} */ ;
/** @type {__VLS_StyleScopedClasses['edit-modal-footer']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-loader-spinner']} */ ;
/** @type {__VLS_StyleScopedClasses['anime-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            User: User,
            Hash: Hash,
            Mail: Mail,
            Phone: Phone,
            Calendar: Calendar,
            Clock: Clock,
            Edit: Edit,
            Key: Key,
            Camera: Camera,
            AvatarCropper: AvatarCropper,
            showEditModal: showEditModal,
            showPasswordModal: showPasswordModal,
            showCropper: showCropper,
            selectedImageUrl: selectedImageUrl,
            editLoading: editLoading,
            editError: editError,
            editSuccess: editSuccess,
            passwordLoading: passwordLoading,
            passwordError: passwordError,
            passwordSuccess: passwordSuccess,
            avatarUploading: avatarUploading,
            editForm: editForm,
            passwordForm: passwordForm,
            roleLabel: roleLabel,
            roleBadgeClass: roleBadgeClass,
            maskPhone: maskPhone,
            formatDate: formatDate,
            getAvatarUrl: getAvatarUrl,
            openCropper: openCropper,
            handleAvatarCrop: handleAvatarCrop,
            handleUpdateProfile: handleUpdateProfile,
            handleChangePassword: handleChangePassword,
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
