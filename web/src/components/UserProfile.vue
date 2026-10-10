<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="show"
        class="user-profile-overlay"
        role="dialog"
        ref="profileDialogRef"
        tabindex="-1"
        :aria-modal="showEditModal || showPasswordModal ? 'false' : 'true'"
        aria-labelledby="user-profile-title"
        @click.self="$emit('close')"
        @keydown="handleProfileDialogKeydown"
      >
        <div class="user-profile-card anime-card" @click.stop>
          <div class="user-profile-header">
            <h2 id="user-profile-title" class="user-profile-title">用户信息</h2>
            <button type="button" class="user-profile-close" aria-label="关闭用户信息" @click="$emit('close')">✕</button>
          </div>
          
          <div class="user-profile-body">
            <div v-if="!user" class="user-loading-state" role="status" aria-live="polite">
              <span class="anime-loader-spinner" aria-hidden="true" style="width: 32px; height: 32px;"></span>
              <div>正在加载用户信息...</div>
            </div>
            <template v-else>
              <div class="user-avatar-section">
                <button ref="avatarTriggerRef" type="button" class="user-avatar-large" aria-label="更换头像" :aria-describedby="avatarUploading ? 'avatar-upload-status' : undefined" @click="openCropper">
                  <img v-if="user.avatar" :src="getAvatarUrl(user.avatar)" alt="头像" class="avatar-img" />
                  <template v-else>
                    <span v-if="user.nickname">{{ user.nickname.charAt(0).toUpperCase() }}</span>
                    <span v-else>{{ (user.username || 'U').charAt(0).toUpperCase() }}</span>
                  </template>
                  <div class="avatar-upload-overlay" aria-hidden="true">
                    <Camera :size="20" />
                  </div>
                </button>
                <div class="user-avatar-info">
                  <div class="user-display-name">{{ user.nickname || user.username || '未知用户' }}</div>
                  <span class="anime-badge" :class="roleBadgeClass">{{ roleLabel }}</span>
                  <span v-if="avatarUploading" id="avatar-upload-status" class="avatar-upload-hint" role="status" aria-live="polite">上传中...</span>
                </div>
              </div>
              
              <div class="user-info-grid">
                <div class="user-info-item">
                  <span class="user-info-label"><User :size="14" /> 用户名</span>
                  <span class="user-info-value">{{ user.username || '-' }}</span>
                </div>
                <div class="user-info-item">
                  <span class="user-info-label"><Hash :size="14" /> 用户ID</span>
                  <span class="user-info-value user-info-code">{{ user.id || '-' }}</span>
                </div>
                <div class="user-info-item" v-if="user.email">
                  <span class="user-info-label"><Mail :size="14" /> 邮箱</span>
                  <span class="user-info-value">{{ user.email }}</span>
                </div>
                <div class="user-info-item" v-if="user.phone">
                  <span class="user-info-label"><Phone :size="14" /> 手机</span>
                  <span class="user-info-value">{{ maskPhone(user.phone) }}</span>
                </div>
                <div class="user-info-item" v-if="user.createTime">
                  <span class="user-info-label"><Calendar :size="14" /> 注册时间</span>
                  <span class="user-info-value">{{ formatDate(user.createTime) }}</span>
                </div>
                <div class="user-info-item">
                  <span class="user-info-label"><Clock :size="14" /> 在线状态</span>
                  <span class="user-info-value user-online-status">
                    <span class="status-dot online" aria-hidden="true"></span>
                    在线
                  </span>
                </div>
              </div>
              
              <div class="user-profile-actions">
                <button ref="editTriggerRef" class="anime-btn primary" @click="showEditModal = true">
                  <Edit :size="16" />
                  <span>修改资料</span>
                </button>
                <button ref="passwordTriggerRef" class="anime-btn ghost" @click="showPasswordModal = true">
                  <Key :size="16" />
                  <span>修改密码</span>
                </button>
              </div>
            </template>
          </div>
        </div>
        
        <!-- 修改资料弹窗 -->
        <div v-if="showEditModal" ref="editDialogRef" class="edit-modal-overlay" role="dialog" aria-modal="true" aria-labelledby="user-profile-edit-title" tabindex="-1" @keydown="handleEditDialogKeydown" @click.self="showEditModal = false">
          <div class="edit-modal anime-card" @click.stop>
            <div class="edit-modal-header">
              <h3 id="user-profile-edit-title" class="edit-modal-title">修改资料</h3>
              <button type="button" class="edit-modal-close" aria-label="关闭修改资料" @click="showEditModal = false">✕</button>
            </div>
            <div class="edit-modal-body">
              <div class="form-group">
                <label class="form-label" for="user-profile-nickname">昵称</label>
                <input id="user-profile-nickname" v-model="editForm.nickname" class="anime-input" placeholder="请输入昵称" />
              </div>
              <div class="form-group">
                <label class="form-label" for="user-profile-email">邮箱</label>
                <input id="user-profile-email" v-model="editForm.email" class="anime-input" type="email" placeholder="请输入邮箱" />
              </div>
              <div class="form-group">
                <label class="form-label" for="user-profile-phone">手机号</label>
                <input id="user-profile-phone" v-model="editForm.phone" class="anime-input" placeholder="请输入手机号" />
              </div>
            </div>
            <div v-if="editError" class="anime-error" role="alert" style="margin: 12px 0;">{{ editError }}</div>
            <div v-if="editSuccess" class="anime-success" role="status" aria-live="polite" style="margin: 12px 0;">{{ editSuccess }}</div>
            <div class="edit-modal-footer">
              <button class="anime-btn primary" :disabled="editLoading" @click="handleUpdateProfile">
                <span v-if="editLoading" class="anime-loader-spinner" style="width: 14px; height: 14px;"></span>
                <span v-else>保存</span>
              </button>
              <button class="anime-btn ghost" @click="showEditModal = false">取消</button>
            </div>
          </div>
        </div>
        
        <!-- 修改密码弹窗 -->
        <div v-if="showPasswordModal" ref="passwordDialogRef" class="edit-modal-overlay" role="dialog" aria-modal="true" aria-labelledby="user-password-edit-title" tabindex="-1" @keydown="handlePasswordDialogKeydown" @click.self="showPasswordModal = false">
          <div class="edit-modal anime-card" @click.stop>
            <div class="edit-modal-header">
              <h3 id="user-password-edit-title" class="edit-modal-title">修改密码</h3>
              <button type="button" class="edit-modal-close" aria-label="关闭修改密码" @click="showPasswordModal = false">✕</button>
            </div>
            <div class="edit-modal-body">
              <div class="form-group">
                <label class="form-label" for="user-current-password">当前密码</label>
                <input id="user-current-password" v-model="passwordForm.oldPassword" class="anime-input" type="password" placeholder="请输入当前密码" />
              </div>
              <div class="form-group">
                <label class="form-label" for="user-new-password">新密码</label>
                <input id="user-new-password" v-model="passwordForm.newPassword" class="anime-input" type="password" placeholder="请输入新密码" />
              </div>
              <div class="form-group">
                <label class="form-label" for="user-confirm-password">确认新密码</label>
                <input id="user-confirm-password" v-model="passwordForm.confirmPassword" class="anime-input" type="password" placeholder="请再次输入新密码" />
              </div>
            </div>
            <div v-if="passwordError" class="anime-error" role="alert" style="margin: 12px 0;">{{ passwordError }}</div>
            <div v-if="passwordSuccess" class="anime-success" role="status" aria-live="polite" style="margin: 12px 0;">{{ passwordSuccess }}</div>
            <div class="edit-modal-footer">
              <button class="anime-btn primary" :disabled="passwordLoading" @click="handleChangePassword">
                <span v-if="passwordLoading" class="anime-loader-spinner" style="width: 14px; height: 14px;"></span>
                <span v-else>确认修改</span>
              </button>
              <button class="anime-btn ghost" @click="showPasswordModal = false">取消</button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
    
    <!-- 头像裁切弹窗 -->
    <AvatarCropper 
      v-if="showCropper"
      :image-url="selectedImageUrl"
      :visible="showCropper"
      @confirm="handleAvatarCrop"
      @cancel="closeCropper"
    />
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { User, Hash, Mail, Phone, Calendar, Clock, Edit, Key, Camera } from 'lucide-vue-next'
import { getCurrentUser, updateUserProfile, changePassword, uploadAvatar, type UserVO } from '../api/user'
import AvatarCropper from './AvatarCropper.vue'

const props = defineProps<{
  show: boolean
  user: UserVO | null
}>()

const emit = defineEmits<{
  close: []
  updated: [user: UserVO]
}>()

const showEditModal = ref(false)
const showPasswordModal = ref(false)
const showCropper = ref(false)
const selectedImageUrl = ref('')
const editLoading = ref(false)
const editError = ref<string | null>(null)
const editSuccess = ref<string | null>(null)
const passwordLoading = ref(false)
const passwordError = ref<string | null>(null)
const passwordSuccess = ref<string | null>(null)
const avatarUploading = ref(false)
const profileDialogRef = ref<HTMLDivElement>()
const editDialogRef = ref<HTMLDivElement>()
const passwordDialogRef = ref<HTMLDivElement>()
const avatarTriggerRef = ref<HTMLButtonElement>()
const editTriggerRef = ref<HTMLButtonElement>()
const passwordTriggerRef = ref<HTMLButtonElement>()
let profileTrigger: HTMLElement | null = null

function trapDialogFocus(event: KeyboardEvent, dialog: HTMLElement | undefined) {
  if (event.key !== 'Tab' || !dialog) return

  const focusableElements = Array.from(dialog.querySelectorAll<HTMLElement>(
    'a[href], button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])'
  )).filter(element => element.getClientRects().length > 0)
  const first = focusableElements[0]
  const last = focusableElements[focusableElements.length - 1]

  if (!first || !last) {
    event.preventDefault()
    dialog.focus()
    return
  }

  const focusIsInside = dialog.contains(document.activeElement)
  if (event.shiftKey && (!focusIsInside || document.activeElement === first)) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && (!focusIsInside || document.activeElement === last)) {
    event.preventDefault()
    first.focus()
  }
}

function handleProfileDialogKeydown(event: KeyboardEvent) {
  if (showEditModal.value || showPasswordModal.value) return
  if (event.key === 'Escape') {
    event.preventDefault()
    emit('close')
    return
  }
  trapDialogFocus(event, profileDialogRef.value)
}

function handleEditDialogKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    event.preventDefault()
    event.stopPropagation()
    showEditModal.value = false
    return
  }
  trapDialogFocus(event, editDialogRef.value)
}

function handlePasswordDialogKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    event.preventDefault()
    event.stopPropagation()
    showPasswordModal.value = false
    return
  }
  trapDialogFocus(event, passwordDialogRef.value)
}

onMounted(async () => {
  profileTrigger = document.activeElement instanceof HTMLElement ? document.activeElement : null
  await nextTick()
  profileDialogRef.value?.querySelector<HTMLButtonElement>('button:not(:disabled)')?.focus()
})

onBeforeUnmount(() => {
  if (profileTrigger?.isConnected) profileTrigger.focus()
})

watch(showEditModal, async (isOpen) => {
  await nextTick()
  if (isOpen) {
    editDialogRef.value?.querySelector<HTMLInputElement>('input:not(:disabled)')?.focus()
  } else {
    editTriggerRef.value?.focus()
  }
})

watch(showPasswordModal, async (isOpen) => {
  await nextTick()
  if (isOpen) {
    passwordDialogRef.value?.querySelector<HTMLInputElement>('input:not(:disabled)')?.focus()
  } else {
    passwordTriggerRef.value?.focus()
  }
})

const editForm = ref({
  nickname: '',
  email: '',
  phone: ''
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const roleLabel = computed(() => {
  switch (props.user?.role) {
    case 'admin': return '管理员'
    case 'user': return '普通用户'
    default: return '访客'
  }
})

const roleBadgeClass = computed(() => {
  switch (props.user?.role) {
    case 'admin': return 'pink'
    case 'user': return 'blue'
    default: return 'muted'
  }
})

function maskPhone(phone: string): string {
  if (!phone || phone.length < 7) return phone
  return phone.slice(0, 3) + '****' + phone.slice(-4)
}

function formatDate(date: string | Date | undefined): string {
  if (!date) return '-'
  const d = new Date(date)
  return d.toLocaleString('zh-CN', { 
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

function getAvatarUrl(path: string): string {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return `/api${path}`
}

function isValidEmail(email: string): boolean {
  if (!email) return true
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

function isValidPhone(phone: string): boolean {
  if (!phone) return true
  return /^1[3-9]\d{9}$/.test(phone)
}

function openCropper() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/jpeg,image/png,image/gif,image/webp'
  input.onchange = (e) => {
    const file = (e.target as HTMLInputElement).files?.[0]
    if (file) {
      selectedImageUrl.value = URL.createObjectURL(file)
      showCropper.value = true
    }
  }
  input.click()
}

function closeCropper() {
  showCropper.value = false
  nextTick(() => avatarTriggerRef.value?.focus())
}

async function handleAvatarCrop(croppedBlob: Blob) {
  const file = new File([croppedBlob], 'avatar.jpg', { type: 'image/jpeg' })
  avatarUploading.value = true
  editError.value = null
  
  try {
    const resp = await uploadAvatar(props.user?.username || '', file)
    if (resp.success && resp.user) {
      emit('updated', resp.user as UserVO)
      editSuccess.value = '头像上传成功'
      setTimeout(() => { editSuccess.value = null }, 2000)
    } else {
      editError.value = resp.message || '头像上传失败'
    }
  } catch (e: any) {
    editError.value = e?.response?.data?.message || '头像上传失败'
  } finally {
    avatarUploading.value = false
    showCropper.value = false
    nextTick(() => avatarTriggerRef.value?.focus())
  }
}

watch(() => props.user, (u) => {
  if (u) {
    editForm.value = {
      nickname: u.nickname || '',
      email: u.email || '',
      phone: u.phone || ''
    }
  }
}, { immediate: true })

async function handleUpdateProfile() {
  const username = props.user?.username
  if (!username) {
    editError.value = '用户名不存在'
    return
  }
  
  // Validate email
  if (editForm.value.email && !isValidEmail(editForm.value.email)) {
    editError.value = '邮箱格式不正确'
    return
  }
  
  // Validate phone
  if (editForm.value.phone && !isValidPhone(editForm.value.phone)) {
    editError.value = '手机号格式不正确（11位数字）'
    return
  }
  
  editLoading.value = true
  editError.value = null
  editSuccess.value = null
  
  try {
    const resp = await updateUserProfile(username, editForm.value)
    
    if (resp.success === true && resp.user) {
      editSuccess.value = '资料修改成功'
      setTimeout(() => {
        showEditModal.value = false
        emit('updated', resp.user as UserVO)
      }, 1000)
    } else {
      editError.value = resp.message || '修改失败'
    }
  } catch (e: any) {
    editError.value = e?.response?.data?.message || e?.message || '修改失败，请重试'
  } finally {
    editLoading.value = false
  }
}

async function handleChangePassword() {
  if (!passwordForm.value.oldPassword) {
    passwordError.value = '请输入当前密码'
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    passwordError.value = '两次输入的密码不一致'
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    passwordError.value = '新密码长度不能少于6位'
    return
  }
  
  passwordLoading.value = true
  passwordError.value = null
  passwordSuccess.value = null
  
  try {
    const resp = await changePassword(
      props.user?.username || '',
      passwordForm.value.oldPassword,
      passwordForm.value.newPassword
    )
    if (resp.success) {
      passwordSuccess.value = '密码修改成功'
      passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
      setTimeout(() => { showPasswordModal.value = false }, 1500)
    } else {
      // Backend returns "原密码不正确" when old password is wrong
      passwordError.value = resp.message || '原密码不正确'
    }
  } catch (e: any) {
    const errMsg = e?.response?.data?.message || e?.message || '密码修改失败'
    // Check if error message indicates wrong old password
    if (errMsg.includes('原密码') || errMsg.includes('密码错误')) {
      passwordError.value = '原密码不正确'
    } else {
      passwordError.value = errMsg
    }
  } finally {
    passwordLoading.value = false
  }
}
</script>

<style scoped>
.user-profile-overlay,
.edit-modal-overlay {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgb(23 35 33 / 42%);
}

.user-profile-overlay { z-index: 1000; }
.edit-modal-overlay { z-index: 1001; }

.user-profile-card,
.edit-modal {
  padding: 0;
  overflow: auto;
  background: var(--ui-surface);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-lg);
  box-shadow: var(--ui-shadow-card);
}

.user-profile-card {
  width: min(480px, calc(100vw - 32px));
  max-height: calc(100vh - 32px);
}

.user-profile-header,
.edit-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--ui-border);
  background: var(--ui-surface-muted);
}

.user-profile-title,
.edit-modal-title {
  margin: 0;
  color: var(--ui-text);
  font-size: 18px;
  font-weight: 700;
}

.edit-modal-title { font-size: 16px; }

.user-profile-close,
.edit-modal-close {
  display: inline-grid;
  place-items: center;
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
  border: 0;
  border-radius: var(--ui-radius-sm);
  background: transparent;
  color: var(--ui-text-muted);
  font-size: 20px;
  cursor: pointer;
}

.user-profile-close:hover,
.edit-modal-close:hover {
  background: var(--ui-primary-soft);
  color: var(--ui-primary-hover);
}

.user-profile-close:focus-visible,
.edit-modal-close:focus-visible,
.user-avatar-large:focus-visible {
  outline: 2px solid var(--ui-focus);
  outline-offset: 2px;
}

.user-profile-body { padding: 24px; }

.user-avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.user-avatar-large {
  position: relative;
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  flex: 0 0 64px;
  overflow: hidden;
  padding: 0;
  border: 1px solid var(--ui-border);
  border-radius: 50%;
  background: var(--ui-primary-soft);
  color: var(--ui-primary);
  box-shadow: var(--ui-shadow-card);
  font: inherit;
  font-size: 28px;
  font-weight: 700;
  cursor: pointer;
}

.user-avatar-large:hover .avatar-upload-overlay,
.user-avatar-large:focus-visible .avatar-upload-overlay { opacity: 1; }

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-upload-overlay {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgb(23 35 33 / 58%);
  color: var(--ui-surface);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.avatar-upload-hint {
  color: var(--ui-primary-hover);
  font-size: 12px;
}

.user-avatar-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-display-name {
  color: var(--ui-text);
  font-size: 20px;
  font-weight: 700;
}

.user-info-grid {
  display: grid;
  gap: 10px;
  margin-bottom: 24px;
}

.user-info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  background: var(--ui-surface-muted);
}

.user-info-label {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--ui-text-secondary);
  font-size: 14px;
  font-weight: 600;
}

.user-info-value {
  color: var(--ui-text);
  font-size: 14px;
  font-weight: 600;
  text-align: right;
  overflow-wrap: anywhere;
}

.user-info-code {
  color: var(--ui-text-secondary);
  font-family: ui-monospace, "Cascadia Code", Consolas, monospace;
}

.user-online-status {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--ui-success);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.online { background: var(--ui-success); }

.user-profile-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

.user-profile-actions .anime-btn,
.edit-modal-footer .anime-btn { min-height: 44px; }

.edit-modal {
  width: min(400px, calc(100vw - 32px));
  max-height: calc(100vh - 32px);
}

.edit-modal-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
}

.edit-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 12px 20px;
  border-top: 1px solid var(--ui-border);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  color: var(--ui-text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.user-loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 48px 24px;
  color: var(--ui-text-secondary);
  font-size: 14px;
}

.user-avatar-info :deep(.anime-badge.muted) {
  color: var(--ui-text-secondary);
}

.fade-enter-active,
.fade-leave-active { transition: opacity 0.2s ease; }

.fade-enter-from,
.fade-leave-to { opacity: 0; }

@media (max-width: 480px) {
  .user-profile-body { padding: 18px; }
  .user-info-item { align-items: flex-start; }
  .edit-modal-footer .anime-btn { flex: 1; }
}

@media (prefers-reduced-motion: reduce) {
  .fade-enter-active,
  .fade-leave-active,
  .avatar-upload-overlay { transition: none; }
}
</style>
