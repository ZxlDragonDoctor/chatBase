<template>
  <div class="anime-page-shell">
    <section class="anime-card" style="padding: 40px; text-align: center;">
      <div style="margin-bottom: 30px;">
        <div style="display: inline-block; width: 80px; height: 80px; border-radius: 50%; background: var(--anime-gradient-pink); display: grid; place-items: center; animation: heartbeat 2s ease-in-out infinite;">
          <span style="font-size: 40px;">✿</span>
        </div>
        <div style="font-size: 28px; font-weight: 700; color: var(--anime-pink); margin-top: 16px;">ChatBase</div>
        <div style="font-size: 16px; color: var(--anime-text-muted);">✿ 智能助手管理系统 ✿</div>
      </div>

      <div style="background: var(--anime-bg-card); border: 3px solid var(--anime-pink); border-radius: var(--anime-radius-xl); padding: 30px; max-width: 400px; margin: 0 auto; box-shadow: var(--anime-shadow-card);">
        <div style="font-size: 18px; font-weight: 700; color: var(--anime-purple); margin-bottom: 24px;">
          <span style="animation: float 2s ease-in-out infinite; display: inline-block;">✧</span>
          欢迎登录
          <span style="animation: float 2s ease-in-out infinite; display: inline-block; animation-delay: 0.5s;">✧</span>
        </div>

        <div class="form-group">
          <label class="form-label">用户名</label>
          <div style="position: relative;">
            <User class="anime-nav-icon" style="position: absolute; left: 14px; top: 50%; transform: translateY(-50%); color: var(--anime-pink);" :size="18" />
            <input v-model="username" class="anime-input" style="padding-left: 44px;" placeholder="请输入用户名" :disabled="loading" />
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">密码</label>
          <div style="position: relative;">
            <Lock class="anime-nav-icon" style="position: absolute; left: 14px; top: 50%; transform: translateY(-50%); color: var(--anime-pink);" :size="18" />
            <input v-model="password" class="anime-input" style="padding-left: 44px;" type="password" placeholder="请输入密码" :disabled="loading" @keyup.enter="handleLogin" />
          </div>
        </div>

        <div v-if="error" class="anime-error" style="margin-bottom: 16px;">{{ error }}</div>

        <button class="anime-btn primary" style="width: 100%; justify-content: center;" :disabled="loading || !username.trim() || !password.trim()" @click="handleLogin">
          <span v-if="loading" class="anime-loader-spinner" style="width: 18px; height: 18px;"></span>
          <span v-else>✿ 登录 ✿</span>
        </button>

        <div style="margin-top: 24px; border-top: 2px solid var(--anime-border); padding-top: 16px;">
          <div style="color: var(--anime-text-muted); font-size: 14px;">
            还没有账号？
            <button class="anime-btn ghost" style="padding: 6px 16px;" @click="showRegister = true">
              立即注册 ★
            </button>
          </div>
        </div>
      </div>

      <div style="margin-top: 20px;">
        <span class="anime-status-badge">
          <span class="anime-status-dot"></span>
          <span>✧ STATUS: READY ✧</span>
        </span>
      </div>

      <div v-if="showRegister" class="anime-modal-overlay" @click.self="showRegister = false">
        <div class="anime-modal">
          <div class="anime-modal-header">
            <span class="anime-modal-title">✿ 用户注册 ✿</span>
            <button class="anime-modal-close" @click="showRegister = false">✕</button>
          </div>
          <div class="anime-modal-body">
            <div class="form-group">
              <label class="form-label">用户名</label>
              <input v-model="regUsername" class="anime-input" placeholder="请输入用户名" :disabled="regLoading" />
            </div>
            <div class="form-group">
              <label class="form-label">密码</label>
              <input v-model="regPassword" class="anime-input" type="password" placeholder="请输入密码" :disabled="regLoading" />
            </div>
            <div class="form-group">
              <label class="form-label">昵称（可选）</label>
              <input v-model="regNickname" class="anime-input" placeholder="请输入昵称" :disabled="regLoading" />
            </div>
          </div>
          <div v-if="regError" class="anime-error" style="margin-bottom: 16px;">{{ regError }}</div>
          <div class="anime-modal-footer">
            <button class="anime-btn primary" :disabled="regLoading || !regUsername.trim() || !regPassword.trim()" @click="handleRegister">
              <span v-if="regLoading" class="anime-loader-spinner" style="width: 16px; height: 16px;"></span>
              <span v-else>注册</span>
            </button>
            <button class="anime-btn ghost" @click="showRegister = false">取消</button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from 'lucide-vue-next'
import { login, register } from '../api/user'

const router = useRouter()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref<string | null>(null)

const showRegister = ref(false)
const regUsername = ref('')
const regPassword = ref('')
const regNickname = ref('')
const regLoading = ref(false)
const regError = ref<string | null>(null)

async function handleLogin() {
  if (!username.value.trim() || !password.value.trim()) return
  loading.value = true
  error.value = null
  try {
    const resp = await login(username.value.trim(), password.value)
    if (resp.success && resp.token) {
      localStorage.setItem('chatbase_token', resp.token)
      localStorage.setItem('chatbase_user', resp.user?.username || username.value.trim())
      if (resp.user?.role) {
        localStorage.setItem('chatbase_role', resp.user.role)
      }
      if (resp.user?.id) {
        localStorage.setItem('chatbase_admin_id', resp.user.id.toString())
      }
      router.push('/console/dashboard')
    } else {
      error.value = resp.message || '用户名或密码错误'
    }
  } catch (e: any) {
    error.value = e?.message || '登录失败'
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!regUsername.value.trim() || !regPassword.value.trim()) return
  regLoading.value = true
  regError.value = null
  try {
    const resp = await register(regUsername.value.trim(), regPassword.value, regNickname.value.trim())
    if (resp.success) {
      showRegister.value = false
      username.value = regUsername.value
      password.value = regPassword.value
      regUsername.value = ''
      regPassword.value = ''
      regNickname.value = ''
    } else {
      regError.value = resp.message || '注册失败'
    }
  } catch (e: any) {
    regError.value = e?.message || '注册失败'
  } finally {
    regLoading.value = false
  }
}
</script>

<style scoped>
.form-group {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--anime-text-primary);
  margin-bottom: 8px;
}

.anime-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.anime-modal {
  background: var(--anime-bg-card);
  border: 3px solid var(--anime-pink);
  border-radius: var(--anime-radius-xl);
  padding: 24px;
  width: 400px;
  max-width: 90vw;
}

.anime-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.anime-modal-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--anime-pink);
}

.anime-modal-close {
  background: transparent;
  border: none;
  font-size: 20px;
  color: var(--anime-text-muted);
  cursor: pointer;
}

.anime-modal-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.anime-modal-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 16px;
}

@keyframes heartbeat {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}
</style>