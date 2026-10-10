<template>
  <main class="login-page">
    <a class="anime-skip-link" href="#login-form">跳转到登录表单</a>
    <div class="login-layout">
      <section class="login-illustration" aria-label="ChatBase 平台介绍">
        <div class="login-ill-content">
          <div class="ill-badge">
            <span class="ill-badge-dot"></span>
            <span>CHATBASE WORKSPACE</span>
          </div>
          <p class="ill-title">让团队知识<br />成为可靠的答案</p>
          <div class="ill-desc">
            连接消息渠道、知识库和 AI 助手，在一个工作台完成管理与协作。
          </div>
          <div class="ill-features">
            <div class="ill-feature">
              <span class="ill-feature-icon">01</span>
              <span>多平台消息采集</span>
            </div>
            <div class="ill-feature">
              <span class="ill-feature-icon">02</span>
              <span>Dify AI 智能问答</span>
            </div>
            <div class="ill-feature">
              <span class="ill-feature-icon">03</span>
              <span>知识库管理系统</span>
            </div>
          </div>
        </div>
        <div class="login-illustration-mark" aria-hidden="true">
          <span>CB</span>
          <div class="login-mark-orbit login-mark-orbit-one"></div>
          <div class="login-mark-orbit login-mark-orbit-two"></div>
        </div>
      </section>

      <section class="login-panel" aria-label="账号登录">
        <div class="login-card">
          <div class="login-card-inner">
            <div class="login-logo">
              <div class="login-logo-icon">
                <img src="/logo.png" alt="ChatBase" class="login-logo-img" />
              </div>
            </div>
            <h1 class="login-title">欢迎回来</h1>
            <p class="login-subtitle">请登录您的账号</p>

            <form id="login-form" class="login-form" @submit.prevent="handleLogin">
              <div class="login-field">
                <label class="login-label" for="login-username">用户名</label>
                <div class="login-input-wrap">
                  <span class="login-input-icon">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                  </span>
                  <input id="login-username" v-model="username" class="login-input" name="username" autocomplete="username" placeholder="请输入用户名" :disabled="loading" />
                </div>
              </div>

              <div class="login-field">
                <label class="login-label" for="login-password">密码</label>
                <div class="login-input-wrap">
                  <span class="login-input-icon">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                  </span>
                  <input id="login-password" v-model="password" class="login-input" name="password" type="password" autocomplete="current-password" placeholder="请输入密码" :disabled="loading" />
                </div>
              </div>

              <div v-if="error" class="login-error" role="alert">{{ error }}</div>

              <button class="login-btn" type="submit" :disabled="loading || !username.trim() || !password.trim()" :aria-busy="loading">
                <span v-if="loading" class="login-btn-loader" aria-hidden="true"></span>
                <span>{{ loading ? '登录中…' : '登录工作台' }}</span>
              </button>

              <div class="login-register-link">
                账号由管理员统一分配
              </div>
            </form>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api/user'

const router = useRouter()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref<string | null>(null)
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

</script>

<style scoped>
.login-page {
  min-height: 100vh;
  min-height: 100dvh;
  display: grid;
  place-items: center;
  padding: clamp(16px, 3vw, 44px);
  overflow: auto;
  color: var(--ui-text);
  background: var(--ui-canvas);
}

.login-layout {
  width: min(1180px, 100%);
  min-height: min(720px, calc(100dvh - 88px));
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(360px, 0.92fr);
  gap: 24px;
}

.login-illustration {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  display: flex;
  align-items: center;
  min-height: 620px;
  padding: clamp(32px, 5vw, 72px);
  border-radius: 20px;
  color: #fff;
  background: linear-gradient(145deg, #0c5d53 0%, #087f70 58%, #176f69 100%);
}

.login-illustration::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -1;
  background:
    radial-gradient(circle at 82% 18%, rgb(255 255 255 / 15%), transparent 28%),
    linear-gradient(90deg, transparent 49.8%, rgb(255 255 255 / 5%) 50%, transparent 50.2%),
    linear-gradient(transparent 49.8%, rgb(255 255 255 / 5%) 50%, transparent 50.2%);
  background-size: auto, 46px 46px, 46px 46px;
  mask-image: linear-gradient(135deg, transparent 5%, #000 80%);
}

.login-ill-content {
  position: relative;
  z-index: 2;
  max-width: 470px;
}

.ill-badge {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 36px;
  padding: 8px 12px;
  border: 1px solid rgb(255 255 255 / 24%);
  border-radius: 999px;
  color: rgb(255 255 255 / 88%);
  background: rgb(255 255 255 / 9%);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
}

.ill-badge-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #a9e4c5;
}

.ill-title {
  margin: 0 0 18px;
  color: #fff;
  font-size: clamp(34px, 4vw, 48px);
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: -0.04em;
}

.ill-desc {
  max-width: 390px;
  margin-bottom: 36px;
  color: rgb(255 255 255 / 76%);
  font-size: 15px;
  line-height: 1.8;
}

.ill-features {
  display: grid;
  gap: 16px;
}

.ill-feature {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgb(255 255 255 / 90%);
  font-size: 14px;
}

.ill-feature-icon {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border: 1px solid rgb(255 255 255 / 22%);
  border-radius: 9px;
  color: #d0eee5;
  background: rgb(255 255 255 / 10%);
  font-size: 10px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.login-illustration-mark {
  position: absolute;
  right: 7%;
  bottom: 4%;
  width: 270px;
  height: 270px;
  display: grid;
  place-items: center;
  border: 1px solid rgb(255 255 255 / 12%);
  border-radius: 50%;
  color: rgb(255 255 255 / 9%);
  font-size: 110px;
  font-weight: 800;
  letter-spacing: -0.1em;
  user-select: none;
}

.login-mark-orbit {
  position: absolute;
  inset: 15%;
  border: 1px solid rgb(255 255 255 / 13%);
  border-radius: 50%;
}

.login-mark-orbit-one { transform: scaleX(0.46) rotate(35deg); }
.login-mark-orbit-two { transform: scaleX(0.46) rotate(-35deg); }

.login-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(12px, 3vw, 40px);
}

.login-card {
  width: 100%;
  max-width: 430px;
  padding: clamp(28px, 4vw, 44px);
  border: 1px solid var(--ui-border);
  border-radius: 18px;
  background: var(--ui-surface);
  box-shadow: 0 12px 36px rgb(19 48 41 / 8%);
}

.login-card-inner { display: flex; flex-direction: column; align-items: flex-start; }
.login-logo { margin-bottom: 26px; }
.login-logo-icon { width: 48px; height: 48px; overflow: hidden; border: 1px solid var(--ui-border); border-radius: 12px; background: #fff; }
.login-logo-img { width: 100%; height: 100%; object-fit: cover; }
.login-title { margin: 0 0 8px; color: var(--ui-text); font-size: 28px; font-weight: 700; letter-spacing: -0.04em; }
.login-subtitle { margin: 0 0 30px; color: var(--ui-text-secondary); font-size: 14px; }
.login-form { width: 100%; }
.login-field { margin-bottom: 20px; }
.login-label { display: block; margin-bottom: 8px; color: var(--ui-text-secondary); font-size: 13px; font-weight: 600; }
.login-input-wrap { position: relative; }
.login-input-icon { position: absolute; top: 50%; left: 14px; display: flex; color: var(--ui-text-muted); transform: translateY(-50%); pointer-events: none; }
.login-input {
  width: 100%;
  min-height: 46px;
  padding: 11px 14px 11px 42px;
  border: 1px solid var(--ui-border);
  border-radius: 9px;
  outline: none;
  color: var(--ui-text);
  background: var(--ui-surface-muted);
  font: inherit;
  font-size: 14px;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, background-color 0.16s ease;
}

.login-input:focus { border-color: var(--ui-primary); background: #fff; box-shadow: 0 0 0 3px rgb(8 127 112 / 12%); }
.login-input::placeholder { color: #8b9893; }
.login-input:disabled { opacity: 0.6; cursor: not-allowed; }
.login-error { margin-bottom: 16px; padding: 10px 12px; border: 1px solid var(--ui-danger-border); border-radius: var(--ui-radius-md); color: var(--ui-danger); background: var(--ui-danger-soft); font-size: 13px; }
.login-btn {
  width: 100%;
  min-height: 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  border: 1px solid var(--ui-primary);
  border-radius: 9px;
  color: #fff;
  background: var(--ui-primary);
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.16s ease, border-color 0.16s ease;
}

.login-btn:hover { border-color: var(--ui-primary-hover); background: var(--ui-primary-hover); }
.login-btn:focus-visible { outline: 2px solid var(--ui-focus); outline-offset: 3px; }
.login-btn:disabled { opacity: 0.58; cursor: not-allowed; }
.login-btn-loader { width: 18px; height: 18px; border: 2px solid rgb(255 255 255 / 40%); border-top-color: #fff; border-radius: 50%; animation: login-spin 0.8s linear infinite; }
@keyframes login-spin { to { transform: rotate(360deg); } }
.login-register-link { margin-top: 18px; color: var(--ui-text-muted); font-size: 12px; text-align: center; }

@media (max-width: 900px) {
  .login-page { padding: 20px; }
  .login-layout { width: min(520px, 100%); min-height: 0; display: block; }
  .login-illustration { display: none; }
  .login-panel { padding: 0; }
}

@media (max-width: 520px) {
  .login-page { padding: 12px; }
  .login-card { padding: 26px 22px; }
  .login-title { font-size: 25px; }
}
</style>
