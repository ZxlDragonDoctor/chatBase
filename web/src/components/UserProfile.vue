<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="show" class="user-profile-overlay" @click.self="$emit('close')">
        <div class="user-profile-card anime-card" @click.stop>
          <div class="user-profile-header">
            <span class="user-profile-title">✿ 用户信息 ✿</span>
            <button class="user-profile-close" @click="$emit('close')">✕</button>
          </div>
          
          <div class="user-profile-body">
            <div v-if="!user" class="user-loading-state">
              <span class="anime-loader-spinner" style="width: 32px; height: 32px;"></span>
              <div>正在加载用户信息...</div>
            </div>
            <template v-else>
              <div class="user-avatar-section">
                <div class="user-avatar-large" @click="openCropper">
                  <img v-if="user.avatar" :src="getAvatarUrl(user.avatar)" alt="头像" class="avatar-img" />
                  <template v-else>
                    <span v-if="user.nickname">{{ user.nickname.charAt(0).toUpperCase() }}</span>
                    <span v-else>{{ (user.username || 'U').charAt(0).toUpperCase() }}</span>
                  </template>
                  <div class="avatar-upload-overlay">
                    <Camera :size="20" />
                  </div>
                </div>
                <div class="user-avatar-info">
                  <div class="user-display-name">{{ user.nickname || user.username || '未知用户' }}</div>
                  <span class="anime-badge" :class="roleBadgeClass">{{ roleLabel }}</span>
                  <span v-if="avatarUploading" class="avatar-upload-hint">上传中...</span>
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
                    <span class="status-dot online"></span>
                    在线
                  </span>
                </div>
              </div>
              
              <div class="user-profile-actions">
                <button class="anime-btn primary" @click="showEditModal = true">
                  <Edit :size="16" />
                  <span>修改资料</span>
                </button>
                <button class="anime-btn ghost" @click="showPasswordModal = true">
                  <Key :size="16" />
                  <span>修改密码</span>
                </button>
              </div>
            </template>
          </div>
        </div>
        
        <!-- 修改资料弹窗 -->
        <div v-if="showEditModal" class="edit-modal-overlay" @click.self="showEditModal = false">
          <div class="edit-modal anime-card">
            <div class="edit-modal-header">
              <span class="edit-modal-title">✿ 修改资料 ✿</span>
              <button class="edit-modal-close" @click="showEditModal = false">✕</button>
            </div>
            <div class="edit-modal-body">
              <div class="form-group">
                <label class="form-label">昵称</label>
                <input v-model="editForm.nickname" class="anime-input" placeholder="请输入昵称" />
              </div>
              <div class="form-group">
                <label class="form-label">邮箱</label>
                <input v-model="editForm.email" class="anime-input" type="email" placeholder="请输入邮箱" />
              </div>
              <div class="form-group">
                <label class="form-label">手机号</label>
                <input v-model="editForm.phone" class="anime-input" placeholder="请输入手机号" />
              </div>
            </div>
            <div v-if="editError" class="anime-error" style="margin: 12px 0;">{{ editError }}</div>
            <div v-if="editSuccess" class="anime-success" style="margin: 12px 0;">{{ editSuccess }}</div>
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
        <div v-if="showPasswordModal" class="edit-modal-overlay" @click.self="showPasswordModal = false">
          <div class="edit-modal anime-card">
            <div class="edit-modal-header">
              <span class="edit-modal-title">✿ 修改密码 ✿</span>
              <button class="edit-modal-close" @click="showPasswordModal = false">✕</button>
            </div>
            <div class="edit-modal-body">
              <div class="form-group">
                <label class="form-label">当前密码</label>
                <input v-model="passwordForm.oldPassword" class="anime-input" type="password" placeholder="请输入当前密码" />
              </div>
              <div class="form-group">
                <label class="form-label">新密码</label>
                <input v-model="passwordForm.newPassword" class="anime-input" type="password" placeholder="请输入新密码" />
              </div>
              <div class="form-group">
                <label class="form-label">确认新密码</label>
                <input v-model="passwordForm.confirmPassword" class="anime-input" type="password" placeholder="请再次输入新密码" />
              </div>
            </div>
            <div v-if="passwordError" class="anime-error" style="margin: 12px 0;">{{ passwordError }}</div>
            <div v-if="passwordSuccess" class="anime-success" style="margin: 12px 0;">{{ passwordSuccess }}</div>
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
      @cancel="showCropper = false"
    />
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
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
.user-profile-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.user-profile-card {
  width: 480px;
  max-width: 90vw;
  padding: 0;
  overflow: hidden;
}

.user-profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 2px solid var(--anime-border);
  background: var(--anime-bg);
}

.user-profile-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--anime-pink);
}

.user-profile-close {
  background: transparent;
  border: none;
  font-size: 20px;
  color: var(--anime-text-muted);
  cursor: pointer;
}

.user-profile-body {
  padding: 24px;
}

.user-avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.user-avatar-large {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--anime-gradient-pink);
  display: grid;
  place-items: center;
  font-size: 28px;
  font-weight: 700;
  color: var(--anime-bg);
  box-shadow: var(--anime-shadow-card);
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.user-avatar-large:hover .avatar-upload-overlay {
  opacity: 1;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: grid;
  place-items: center;
  opacity: 0;
  transition: opacity 0.2s;
  color: white;
}

.avatar-upload-hint {
  font-size: 12px;
  color: var(--anime-pink);
}

.user-avatar-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-display-name {
  font-size: 20px;
  font-weight: 700;
  color: var(--anime-text-primary);
}

.user-info-grid {
  display: grid;
  gap: 12px;
  margin-bottom: 24px;
}

.user-info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--anime-bg);
  border-radius: var(--anime-radius-lg);
  border: 2px solid var(--anime-border);
}

.user-info-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--anime-text-secondary);
}

.user-info-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--anime-text-primary);
}

.user-info-code {
  font-family: monospace;
  color: var(--anime-purple);
}

.user-online-status {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #22c55e;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.online {
  background: #22c55e;
  box-shadow: 0 0 6px #22c55e;
}

.user-profile-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* Edit & Password Modal */
.edit-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1001;
}

.edit-modal {
  width: 400px;
  max-width: 90vw;
  padding: 0;
  overflow: hidden;
}

.edit-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 2px solid var(--anime-border);
  background: var(--anime-bg);
}

.edit-modal-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--anime-pink);
}

.edit-modal-close {
  background: transparent;
  border: none;
  font-size: 18px;
  color: var(--anime-text-muted);
  cursor: pointer;
}

.edit-modal-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edit-modal-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding: 12px 20px;
  border-top: 2px solid var(--anime-border);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--anime-text-secondary);
}

/* Transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.user-loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 48px 24px;
  color: var(--anime-text-muted);
  font-size: 14px;
}
</style>
