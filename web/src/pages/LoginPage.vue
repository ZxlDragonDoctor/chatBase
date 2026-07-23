<template>
  <div class="login-page">
    <div class="login-bg-decor"></div>

    <div class="sakura-container">
      <div
        v-for="i in 6"
        :key="i"
        class="sakura-petal"
        :style="{
          left: Math.random() * 100 + '%',
          animationDelay: (i * 1.2) + 's',
          animationDuration: (8 + Math.random() * 6) + 's',
          width: (10 + Math.random() * 8) + 'px',
          height: (10 + Math.random() * 8) + 'px',
          opacity: 0.3 + Math.random() * 0.3
        }"
      ></div>
    </div>

    <div class="login-layout">
      <!-- Left: Illustration Panel -->
      <div class="login-illustration">
        <div class="login-ill-bg">
          <img :src="animeBgUrl" alt="" class="login-ill-img" @load="illLoaded = true" />
          <div class="login-ill-overlay"></div>
        </div>
        <div class="login-ill-content">
          <div class="ill-badge">
            <span class="ill-badge-dot"></span>
            <span>ChatBase</span>
          </div>
          <div class="ill-title">智能助手管理系统</div>
          <div class="ill-desc">
            数据采集 · AI 问答 · 知识管理 · 群聊分析
          </div>
          <div class="ill-features">
            <div class="ill-feature">
              <span class="ill-feature-icon">✦</span>
              <span>多平台消息采集</span>
            </div>
            <div class="ill-feature">
              <span class="ill-feature-icon">✦</span>
              <span>Dify AI 智能问答</span>
            </div>
            <div class="ill-feature">
              <span class="ill-feature-icon">✦</span>
              <span>知识库管理系统</span>
            </div>
            <div class="ill-feature">
              <span class="ill-feature-icon">✦</span>
              <span>数据统计与分析</span>
            </div>
          </div>
          <div class="ill-footer">
            <span>anime.pictures</span>
          </div>
        </div>
      </div>

      <!-- Right: Login Panel -->
      <div class="login-panel">
        <div class="login-card">
          <div class="login-card-inner">
            <div class="login-logo">
              <div class="login-logo-icon">
                <img src="/logo.png" alt="logo" class="login-logo-img" />
              </div>
            </div>
            <h1 class="login-title">欢迎回来</h1>
            <p class="login-subtitle">请登录您的账号</p>

            <div class="login-form">
              <div class="login-field">
                <label class="login-label">用户名</label>
                <div class="login-input-wrap">
                  <span class="login-input-icon">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                  </span>
                  <input v-model="username" class="login-input" placeholder="请输入用户名" :disabled="loading" />
                </div>
              </div>

              <div class="login-field">
                <label class="login-label">密码</label>
                <div class="login-input-wrap">
                  <span class="login-input-icon">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                  </span>
                  <input v-model="password" class="login-input" type="password" placeholder="请输入密码" :disabled="loading" @keyup.enter="handleLogin" />
                </div>
              </div>

              <div v-if="error" class="login-error">{{ error }}</div>

              <button class="login-btn" :disabled="loading || !username.trim() || !password.trim()" @click="handleLogin">
                <span v-if="loading" class="login-btn-loader"></span>
                <span v-else>登 录</span>
              </button>

              <div class="login-register-link">
                还没有账号？
                <button class="login-link-btn" @click="showRegister = true">立即注册</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Register Modal -->
    <div v-if="showRegister" class="anime-modal-overlay" @click.self="showRegister = false">
      <div class="anime-modal">
        <div class="anime-modal-header">
          <span class="anime-modal-title">用户注册</span>
          <button class="anime-modal-close" @click="showRegister = false">✕</button>
        </div>
        <div class="anime-modal-body">
          <div class="anime-form-group">
            <label>用户名 *</label>
            <input v-model="regUsername" class="anime-input" placeholder="请输入用户名" :disabled="regLoading" />
          </div>
          <div class="anime-form-group">
            <label>密码 *</label>
            <input v-model="regPassword" class="anime-input" type="password" placeholder="请输入密码" :disabled="regLoading" />
          </div>
          <div class="anime-form-group">
            <label>昵称（可选）</label>
            <input v-model="regNickname" class="anime-input" placeholder="请输入昵称" :disabled="regLoading" />
          </div>
          <div v-if="regError" class="anime-error" style="margin-bottom: 16px;">{{ regError }}</div>
        </div>
        <div class="anime-modal-footer">
          <button class="anime-btn ghost" @click="showRegister = false">取消</button>
          <button class="anime-btn primary" :disabled="regLoading || !regUsername.trim() || !regPassword.trim()" @click="handleRegister">
            <span v-if="regLoading" class="anime-loader-spinner"></span>
            <span v-else>注册</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { login, register } from '../api/user'

const router = useRouter()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref<string | null>(null)
const illLoaded = ref(false)

const showRegister = ref(false)
const regUsername = ref('')
const regPassword = ref('')
const regNickname = ref('')
const regLoading = ref(false)
const regError = ref<string | null>(null)

const animeBgUrl = ref('https://t.alcy.cc/moe?' + Date.now())

function refreshBg() {
  animeBgUrl.value = 'https://t.alcy.cc/moe?' + Date.now()
  illLoaded.value = false
}

async function handleLogin() {
  if (!username.value.trim() || !password.value.trim()) return
  loading.value = true
  error.value = null
  try {
    const resp = await login(username.value.trim(), password.value)
    if (resp.success && resp.token) {
      localStorage.setItem('chatbase_token', resp.token)
      localStorage.setItem('chatbase_user', resp.user?.nickname || resp.user?.username || username.value.trim())
      localStorage.setItem('chatbase_original_username', resp.user?.username || username.value.trim())
      if (resp.user?.role) localStorage.setItem('chatbase_role', resp.user.role)
      if (resp.user?.id) localStorage.setItem('chatbase_admin_id', resp.user.id.toString())
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

onMounted(() => {
  setInterval(refreshBg, 60000)
})
</script>

<style scoped>
.login-page {
  position: fixed;
  inset: 0;
  display: flex;
  overflow: hidden;
  background: #1a1a2e;
}

.login-bg-decor {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  background:
    radial-gradient(ellipse 500px 500px at 20% 30%, rgba(255, 107, 157, 0.08) 0%, transparent 60%),
    radial-gradient(ellipse 400px 400px at 80% 70%, rgba(79, 195, 247, 0.06) 0%, transparent 60%),
    radial-gradient(ellipse 300px 300px at 50% 50%, rgba(179, 157, 219, 0.05) 0%, transparent 60%);
}

/* ─── Sakura Petals ─── */

.sakura-container {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 1;
  overflow: hidden;
}

.sakura-petal {
  position: absolute;
  top: -20px;
  background: radial-gradient(ellipse at 30% 30%, rgba(255, 107, 157, 0.35), rgba(255, 107, 157, 0.08));
  border-radius: 50% 0 50% 0;
  animation: sakuraFall linear infinite;
}

@keyframes sakuraFall {
  0% {
    opacity: 0;
    transform: translateY(-20px) rotate(0deg) scale(0.8);
  }
  8% {
    opacity: 1;
  }
  92% {
    opacity: 0.6;
  }
  100% {
    opacity: 0;
    transform: translateY(calc(100vh + 20px)) rotate(720deg) scale(0.3);
  }
}

/* ─── Layout ─── */

.login-layout {
  display: flex;
  width: 100%;
  height: 100%;
  position: relative;
  z-index: 2;
}

/* ─── Left: Illustration ─── */

.login-illustration {
  flex: 0 0 55%;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-ill-bg {
  position: absolute;
  inset: 0;
}

.login-ill-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 0.8s ease;
}

.login-ill-img[src] {
  opacity: 0.6;
}

.login-ill-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    135deg,
    rgba(26, 26, 46, 0.85) 0%,
    rgba(26, 26, 46, 0.4) 40%,
    rgba(26, 26, 46, 0.2) 70%,
    rgba(26, 26, 46, 0.6) 100%
  );
}

.login-ill-content {
  position: relative;
  z-index: 1;
  padding: 60px;
  max-width: 480px;
  animation: illContentIn 0.8s ease-out;
}

@keyframes illContentIn {
  from {
    opacity: 0;
    transform: translateX(-30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.ill-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1px;
  margin-bottom: 40px;
}

.ill-badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #81c784;
  box-shadow: 0 0 8px rgba(129, 199, 132, 0.5);
}

.ill-title {
  font-size: 36px;
  font-weight: 800;
  color: #fff;
  line-height: 1.2;
  margin-bottom: 16px;
  letter-spacing: -0.5px;
}

.ill-desc {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 36px;
  line-height: 1.6;
}

.ill-features {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.ill-feature {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.65);
  font-size: 14px;
  animation: featureIn 0.6s ease-out backwards;
}

.ill-feature:nth-child(1) { animation-delay: 0.1s; }
.ill-feature:nth-child(2) { animation-delay: 0.2s; }
.ill-feature:nth-child(3) { animation-delay: 0.3s; }
.ill-feature:nth-child(4) { animation-delay: 0.4s; }

@keyframes featureIn {
  from { opacity: 0; transform: translateX(-20px); }
  to { opacity: 1; transform: translateX(0); }
}

.ill-feature-icon {
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: rgba(255, 107, 157, 0.2);
  color: #ff6b9d;
  font-size: 11px;
  flex-shrink: 0;
}

.ill-footer {
  position: absolute;
  bottom: 30px;
  left: 60px;
  color: rgba(255, 255, 255, 0.2);
  font-size: 11px;
}

/* ─── Right: Login Panel ─── */

.login-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  animation: panelIn 0.8s ease-out;
}

@keyframes panelIn {
  from {
    opacity: 0;
    transform: translateX(30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  padding: 48px 40px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.3);
}

.login-card-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.login-logo {
  margin-bottom: 24px;
}

.login-logo-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: #fff;
  display: grid;
  place-items: center;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border: 2px solid rgba(255, 107, 157, 0.2);
  overflow: hidden;
}

.login-logo-img {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 50%;
}

.login-title {
  font-size: 24px;
  font-weight: 800;
  color: #fff;
  margin: 0 0 6px;
  letter-spacing: -0.3px;
}

.login-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.4);
  margin: 0 0 32px;
}

.login-form {
  width: 100%;
}

.login-field {
  margin-bottom: 20px;
}

.login-label {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 8px;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.login-input-wrap {
  position: relative;
}

.login-input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: rgba(255, 255, 255, 0.25);
  display: flex;
  pointer-events: none;
}

.login-input {
  width: 100%;
  padding: 14px 16px 14px 44px;
  background: rgba(255, 255, 255, 0.06);
  border: 1.5px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  color: #fff;
  font-family: 'Nunito', 'Quicksand', 'Segoe UI', system-ui, sans-serif;
  font-size: 14px;
  outline: none;
  transition: all 0.2s ease;
}

.login-input:focus {
  border-color: rgba(255, 107, 157, 0.4);
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.08);
}

.login-input::placeholder {
  color: rgba(255, 255, 255, 0.2);
}

.login-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.login-error {
  padding: 10px 14px;
  background: rgba(255, 107, 157, 0.1);
  border: 1px solid rgba(255, 107, 157, 0.2);
  border-radius: 10px;
  color: #ff6b9d;
  font-size: 13px;
  margin-bottom: 16px;
  text-align: center;
}

.login-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #ff6b9d 0%, #b39ddb 100%);
  border: none;
  border-radius: 14px;
  color: #fff;
  font-family: 'Nunito', 'Quicksand', 'Segoe UI', system-ui, sans-serif;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  letter-spacing: 2px;
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 30px rgba(255, 107, 157, 0.3);
}

.login-btn:active {
  transform: scale(0.98);
}

.login-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.login-btn-loader {
  width: 18px;
  height: 18px;
  border: 2.5px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.login-register-link {
  margin-top: 24px;
  text-align: center;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.3);
}

.login-link-btn {
  background: none;
  border: none;
  color: #ff6b9d;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s;
}

.login-link-btn:hover {
  background: rgba(255, 107, 157, 0.1);
}

@media (max-width: 900px) {
  .login-illustration {
    display: none;
  }

  .login-panel {
    flex: 1;
    padding: 24px;
  }

  .login-card {
    padding: 36px 28px;
  }
}
</style>
