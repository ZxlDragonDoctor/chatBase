<template>
  <div class="anime-bg-decor"></div>
  <div class="anime-floating-stars">
    <span class="anime-star" style="top: 10%; left: 5%;">✦</span>
    <span class="anime-star" style="top: 30%; left: 88%; animation-delay: 0.8s;">✦</span>
    <span class="anime-star" style="top: 70%; left: 12%; animation-delay: 1.6s;">✦</span>
    <span class="anime-star" style="top: 88%; left: 92%; animation-delay: 2.4s;">✦</span>
  </div>

  <div class="anime-sakura-container">
    <span class="anime-sakura-petal" style="left: 5%; animation-duration: 12s; animation-delay: 0s;"></span>
    <span class="anime-sakura-petal" style="left: 20%; animation-duration: 15s; animation-delay: 2s;"></span>
    <span class="anime-sakura-petal" style="left: 40%; animation-duration: 10s; animation-delay: 4s;"></span>
    <span class="anime-sakura-petal" style="left: 55%; animation-duration: 14s; animation-delay: 1s;"></span>
    <span class="anime-sakura-petal" style="left: 70%; animation-duration: 11s; animation-delay: 3s;"></span>
    <span class="anime-sakura-petal" style="left: 85%; animation-duration: 13s; animation-delay: 5s;"></span>
  </div>

  <template v-if="isLoginPage">
    <RouterView v-slot="{ Component }">
      <transition name="anime-fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </RouterView>
  </template>

  <div v-else class="anime-app">
    <aside class="anime-nav">
      <div class="anime-nav-brand">
        <div class="anime-logo">
          <img src="/logo.png" alt="logo" style="width:100%;height:100%;object-fit:cover" />
        </div>
        <div>
          <div class="anime-brand-title">ChatBase</div>
          <div class="anime-brand-sub">智能助手管理系统</div>
        </div>
      </div>

      <nav class="anime-nav-list">
        <RouterLink class="anime-nav-item" to="/console/dashboard" active-class="active">
          <Home class="anime-nav-icon" :size="20" />
          <span>首页</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/console/statistics" active-class="active">
          <BarChart3 class="anime-nav-icon" :size="20" />
          <span>统计</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/console/im" active-class="active">
          <Users class="anime-nav-icon" :size="20" />
          <span>群聊</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/console/im/single" active-class="active">
          <MessageSquare class="anime-nav-icon" :size="20" />
          <span>私聊</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/console/knowledge" active-class="active">
          <BookOpen class="anime-nav-icon" :size="20" />
          <span>知识库</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/console/app" active-class="active">
          <Bot class="anime-nav-icon" :size="20" />
          <span>应用</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/console/bots" active-class="active">
          <Cpu class="anime-nav-icon" :size="20" />
          <span>机器人</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/console/faq" active-class="active">
          <HelpCircle class="anime-nav-icon" :size="20" />
          <span>FAQ</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/chat" active-class="active">
          <MessageCircle class="anime-nav-icon" :size="20" />
          <span>问答</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/feedback" active-class="active">
          <Mail class="anime-nav-icon" :size="20" />
          <span>反馈</span>
        </RouterLink>
        <div style="margin: 8px 0 4px; padding: 0 16px;">
          <div style="height: 1px; background: var(--anime-border);"></div>
        </div>
        <RouterLink v-if="isAdmin" class="anime-nav-item" to="/console/feedback-manage" active-class="active">
          <ClipboardList class="anime-nav-icon" :size="20" />
          <span>反馈管理</span>
        </RouterLink>
        <RouterLink v-if="isAdmin" class="anime-nav-item" to="/console/admin/apps" active-class="active">
          <Bot class="anime-nav-icon" :size="20" />
          <span>应用管理</span>
        </RouterLink>
        <RouterLink v-if="isAdmin" class="anime-nav-item" to="/console/admin/kbs" active-class="active">
          <BookOpen class="anime-nav-icon" :size="20" />
          <span>知识库管理</span>
        </RouterLink>
        <RouterLink v-if="isAdmin" class="anime-nav-item" to="/console/admin/users" active-class="active">
          <Users class="anime-nav-icon" :size="20" />
          <span>用户管理</span>
        </RouterLink>
      </nav>

      <!-- User Footer -->
      <div v-if="currentUser" class="anime-nav-footer">
        <div class="gus-avatar" @click="openUserProfile">
          <img v-if="userProfile?.avatar" :src="getAvatarUrl(userProfile.avatar)" alt="" @error="onAvatarError" />
          <span v-else>{{ (userProfile?.nickname || currentUser).charAt(0).toUpperCase() }}</span>
        </div>
        <div class="gus-info" @click="openUserProfile">
          <div class="gus-name">{{ userProfile?.nickname || currentUser }}</div>
          <div class="gus-role">{{ roleLabel }}</div>
        </div>
        <button class="anime-btn xs" @click="handleLogout">
          <LogOut :size="14" />
        </button>
      </div>
    </aside>

    <main class="anime-main">
      <RouterView v-slot="{ Component }">
        <transition name="anime-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </RouterView>
    </main>

    <!-- User Profile Modal -->
    <UserProfile v-if="showUserProfile" :show="showUserProfile" :user="userProfile" @close="showUserProfile = false" @updated="handleUserUpdated" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { RouterLink, RouterView, useRouter, useRoute } from 'vue-router'
import { Home, BarChart3, Users, BookOpen, MessageCircle, Mail, HelpCircle, LogOut, Bot, ClipboardList, Cpu, MessageSquare } from 'lucide-vue-next'
import UserProfile from './components/UserProfile.vue'
import { getCurrentUser as fetchUserProfile } from './api/user'
import type { UserVO } from './api/user'

const router = useRouter()
const route = useRoute()

const isLoginPage = computed(() => route.path === '/login')
const currentUser = ref('')
const isAdmin = ref(false)
const role = ref('')

function syncAuthState() {
  currentUser.value = localStorage.getItem('chatbase_user') || ''
  isAdmin.value = localStorage.getItem('chatbase_role') === 'admin'
  role.value = localStorage.getItem('chatbase_role') || ''
}

function getOriginalUsername(): string {
  return localStorage.getItem('chatbase_original_username') || localStorage.getItem('chatbase_user') || ''
}

const showUserProfile = ref(false)
const userProfile = ref<UserVO | null>(null)

async function openUserProfile() {
  showUserProfile.value = true
  await loadUserProfile()
}

const roleLabel = computed(() => {
  switch (role.value) {
    case 'admin': return '管理员'
    case 'user': return '用户'
    default: return '访客'
  }
})

async function loadUserProfile() {
  if (!localStorage.getItem('chatbase_token')) return
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
  if (displayName) localStorage.setItem('chatbase_user', displayName)
  localStorage.setItem('chatbase_original_username', user.username)
  if (user.role) localStorage.setItem('chatbase_role', user.role)
  syncAuthState()
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
  localStorage.removeItem('chatbase_original_username')
  localStorage.removeItem('chatbase_role')
  localStorage.removeItem('chatbase_admin_id')
  router.push('/login')
}

watch(() => route.path, () => {
  syncAuthState()
  if (localStorage.getItem('chatbase_token')) loadUserProfile()
})

onMounted(() => {
  syncAuthState()
  loadUserProfile()
})
</script>

<style scoped>
.anime-nav-footer {
  margin-top: auto;
}

.anime-fade-enter-active,
.anime-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.anime-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.anime-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
