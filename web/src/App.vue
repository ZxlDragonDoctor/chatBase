<template>
  <div class="anime-bg-decor"></div>
  <div class="anime-floating-stars">
    <span class="anime-star" style="top: 10%; left: 5%;">✧</span>
    <span class="anime-star" style="top: 30%; left: 85%; animation-delay: 0.5s;">✿</span>
    <span class="anime-star" style="top: 70%; left: 15%; animation-delay: 1s;">★</span>
    <span class="anime-star" style="top: 90%; left: 90%; animation-delay: 1.5s;">✧</span>
  </div>
  
  <template v-if="isLoginPage">
    <RouterView />
  </template>
  
  <div v-else class="anime-app">
    <aside class="anime-nav">
      <div class="anime-nav-brand">
        <div class="anime-logo">✿</div>
        <div>
          <div class="anime-brand-title">ChatBase</div>
          <div class="anime-brand-sub">✿ 数据采集统计面板 ✿</div>
        </div>
      </div>
      <nav class="anime-nav-list">
        <RouterLink class="anime-nav-item" to="/console/dashboard" active-class="active">
          <Home class="anime-nav-icon" :size="22" />
          <span>首页</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/console/statistics" active-class="active">
          <BarChart3 class="anime-nav-icon" :size="22" />
          <span>统计</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/console/im" active-class="active">
          <Users class="anime-nav-icon" :size="22" />
          <span>群聊</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/console/knowledge" active-class="active">
          <BookOpen class="anime-nav-icon" :size="22" />
          <span>知识库</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/console/app" active-class="active">
          <Bot class="anime-nav-icon" :size="22" />
          <span>应用</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/console/faq" active-class="active">
          <HelpCircle class="anime-nav-icon" :size="22" />
          <span>FAQ</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/chat" active-class="active">
          <MessageCircle class="anime-nav-icon" :size="22" />
          <span>问答</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/feedback" active-class="active">
          <Mail class="anime-nav-icon" :size="22" />
          <span>反馈</span>
        </RouterLink>
        <RouterLink v-if="isAdmin" class="anime-nav-item" to="/console/feedback-manage" active-class="active">
          <ClipboardList class="anime-nav-icon" :size="22" />
          <span>反馈管理</span>
        </RouterLink>
      </nav>
      <div class="anime-nav-footer">
        <div class="anime-nav-footer-text">✿ ChatBase v1.0 ✿</div>
      </div>
    </aside>
    <main class="anime-main">
      <!-- 全局用户状态栏 -->
      <div v-if="currentUser" class="global-user-status-bar">
        <div class="gus-avatar" @click="openUserProfile">
          <img v-if="userProfile?.avatar" :src="getAvatarUrl(userProfile.avatar)" alt="头像" @error="onAvatarError" />
          <span v-else>{{ (userProfile?.nickname || currentUser).charAt(0).toUpperCase() }}</span>
        </div>
        <div class="gus-info" @click="openUserProfile">
          <span class="gus-name">{{ userProfile?.nickname || currentUser }}</span>
          <span class="anime-badge sm" :class="roleBadgeClass">{{ roleLabel }}</span>
          <span v-if="userProfile?.email" class="gus-contact"><Mail :size="12" /> {{ userProfile.email }}</span>
        </div>
        <span class="gus-divider">|</span>
        <span class="gus-active">最后活跃: <strong>{{ lastActive }}</strong></span>
        <button class="anime-btn ghost xs" @click="openUserProfile">
          <User :size="14" />
          <span>详情</span>
        </button>
        <button class="anime-btn ghost xs danger" @click="handleLogout">
          <LogOut :size="14" />
          <span>退出</span>
        </button>
      </div>
      <RouterView />
    </main>
    
    <!-- 用户详情弹窗 -->
    <UserProfile v-if="showUserProfile" :show="showUserProfile" :user="userProfile" @close="showUserProfile = false" @updated="handleUserUpdated" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { RouterLink, RouterView, useRouter, useRoute } from 'vue-router'
import { Home, BarChart3, Users, BookOpen, MessageCircle, Mail, HelpCircle, User, LogOut, Bot, ClipboardList } from 'lucide-vue-next'
import UserProfile from './components/UserProfile.vue'
import { getCurrentUser as fetchUserProfile } from './api/user'
import type { UserVO } from './api/user'

const router = useRouter()
const route = useRoute()

const isLoginPage = computed(() => route.path === '/login')
const currentUser = computed(() => localStorage.getItem('chatbase_user'))
const isAdmin = computed(() => localStorage.getItem('chatbase_role') === 'admin')

function getOriginalUsername(): string {
  return localStorage.getItem('chatbase_original_username') || localStorage.getItem('chatbase_user') || ''
}
const lastActive = computed(() => {
  const now = new Date()
  return now.toLocaleString('zh-CN', { 
    month: '2-digit', day: '2-digit', 
    hour: '2-digit', minute: '2-digit' 
  })
})

const showUserProfile = ref(false)
const userProfile = ref<UserVO | null>(null)

async function openUserProfile() {
  showUserProfile.value = true
  await loadUserProfile()
}

const roleLabel = computed(() => {
  const role = localStorage.getItem('chatbase_role')
  switch (role) {
    case 'admin': return '管理员'
    case 'user': return '用户'
    default: return '访客'
  }
})

const roleBadgeClass = computed(() => {
  const role = localStorage.getItem('chatbase_role')
  switch (role) {
    case 'admin': return 'pink'
    case 'user': return 'blue'
    default: return 'muted'
  }
})

async function loadUserProfile() {
  const username = getOriginalUsername()
  if (!username) return
  
  try {
    const user = await fetchUserProfile(username)
    if (user) {
      userProfile.value = user
      localStorage.setItem('chatbase_original_username', user.username)
    }
  } catch (e) {
    console.error('加载用户信息失败', e)
  }
}

function handleUserUpdated(user: UserVO) {
  userProfile.value = user
  const displayName = user.nickname || user.username
  if (displayName) {
    localStorage.setItem('chatbase_user', displayName)
  }
  localStorage.setItem('chatbase_original_username', user.username)
  if (user.role) {
    localStorage.setItem('chatbase_role', user.role)
  }
}

function getAvatarUrl(path: string): string {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return `/api${path}`
}

function onAvatarError(e: Event) {
  const img = e.target as HTMLImageElement
  img.style.display = 'none'
}

function handleLogout() {
  localStorage.removeItem('chatbase_token')
  localStorage.removeItem('chatbase_user')
  localStorage.removeItem('chatbase_role')
  localStorage.removeItem('chatbase_admin_id')
  router.push('/login')
}

onMounted(() => {
  loadUserProfile()
})
</script>

<style scoped>
.global-user-status-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 18px;
  margin-bottom: 16px;
  border-radius: var(--anime-radius-pill);
  background: rgba(255, 183, 197, 0.1);
  border: 1px solid var(--anime-border-light);
  font-size: 13px;
  color: var(--anime-text-secondary);
}

.gus-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--anime-gradient-pink);
  display: grid;
  place-items: center;
  font-size: 16px;
  font-weight: 700;
  color: var(--anime-bg);
  cursor: pointer;
  overflow: hidden;
  flex-shrink: 0;
}

.gus-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.gus-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.gus-name {
  font-weight: 600;
  color: var(--anime-text-primary);
}

.gus-contact {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

.gus-divider {
  color: var(--anime-border);
}

.gus-active strong {
  color: var(--anime-text-primary);
}

.global-user-status-bar .anime-badge.sm {
  padding: 4px 10px;
  font-size: 11px;
}

.global-user-status-bar .anime-btn.xs {
  padding: 4px 10px;
  font-size: 12px;
}

.global-user-status-bar .anime-btn.xs.danger {
  color: #ef4444;
}

.global-user-status-bar .anime-btn.xs.danger:hover {
  border-color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}

.anime-nav-footer {
  padding: 12px;
}

.anime-nav-footer-text {
  text-align: center;
  font-size: 11px;
  color: var(--anime-text-muted);
  opacity: 0.6;
}
</style>