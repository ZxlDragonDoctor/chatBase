<template>
  <template v-if="isLoginPage">
    <RouterView v-slot="{ Component }">
      <transition name="anime-fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </RouterView>
  </template>

  <div v-else class="anime-app" @keydown.esc="closeMobileNav">
    <a class="anime-skip-link" href="#main-content">跳转到主内容</a>
    <button
      v-if="mobileNavOpen"
      class="anime-nav-backdrop"
      type="button"
      aria-label="关闭导航菜单"
      @click="closeMobileNav"
    ></button>

    <aside id="primary-navigation" class="anime-nav" :class="{ 'is-open': mobileNavOpen }">
      <div class="anime-nav-brand">
        <div class="anime-logo">
          <img src="/logo.png" alt="logo" style="width:100%;height:100%;object-fit:cover" />
        </div>
        <div>
          <div class="anime-brand-title">ChatBase</div>
          <div class="anime-brand-sub">智能助手管理系统</div>
        </div>
      </div>

      <nav class="anime-nav-list" @click="closeMobileNav">
        <div class="anime-nav-group-label">工作台</div>
        <RouterLink class="anime-nav-item" to="/console/dashboard" exact-active-class="active">
          <Home class="anime-nav-icon" :size="18" />
          <span>首页</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/console/statistics" exact-active-class="active">
          <BarChart3 class="anime-nav-icon" :size="18" />
          <span>统计</span>
        </RouterLink>

        <div class="anime-nav-group-label">消息接入</div>
        <RouterLink class="anime-nav-item" to="/console/im" exact-active-class="active">
          <Users class="anime-nav-icon" :size="18" />
          <span>群聊</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/console/im/single" exact-active-class="active">
          <MessageSquare class="anime-nav-icon" :size="18" />
          <span>私聊</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/console/bots" exact-active-class="active">
          <Cpu class="anime-nav-icon" :size="18" />
          <span>机器人</span>
        </RouterLink>

        <div class="anime-nav-group-label">内容管理</div>
        <RouterLink class="anime-nav-item" to="/console/knowledge" exact-active-class="active">
          <BookOpen class="anime-nav-icon" :size="18" />
          <span>知识库</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/console/app" exact-active-class="active">
          <Bot class="anime-nav-icon" :size="18" />
          <span>应用</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/console/faq" exact-active-class="active">
          <HelpCircle class="anime-nav-icon" :size="18" />
          <span>FAQ</span>
        </RouterLink>

        <div class="anime-nav-group-label">服务</div>
        <RouterLink class="anime-nav-item" to="/chat" exact-active-class="active">
          <MessageCircle class="anime-nav-icon" :size="18" />
          <span>问答</span>
        </RouterLink>
        <RouterLink class="anime-nav-item" to="/feedback" exact-active-class="active">
          <Mail class="anime-nav-icon" :size="18" />
          <span>反馈</span>
        </RouterLink>

        <div v-if="isAdmin" class="anime-nav-group-label">管理</div>
        <RouterLink v-if="isAdmin" class="anime-nav-item" to="/console/feedback-manage" exact-active-class="active">
          <ClipboardList class="anime-nav-icon" :size="20" />
          <span>反馈管理</span>
        </RouterLink>
        <RouterLink v-if="isAdmin" class="anime-nav-item" to="/console/admin/apps" exact-active-class="active">
          <Bot class="anime-nav-icon" :size="20" />
          <span>应用管理</span>
        </RouterLink>
        <RouterLink v-if="isAdmin" class="anime-nav-item" to="/console/admin/kbs" exact-active-class="active">
          <BookOpen class="anime-nav-icon" :size="20" />
          <span>知识库管理</span>
        </RouterLink>
        <RouterLink v-if="isAdmin" class="anime-nav-item" to="/console/admin/users" exact-active-class="active">
          <Users class="anime-nav-icon" :size="20" />
          <span>用户管理</span>
        </RouterLink>
      </nav>

      <!-- User Footer -->
      <div v-if="currentUser" class="anime-nav-footer">
        <button type="button" class="anime-nav-profile" @click="openUserProfile">
          <span class="gus-avatar">
            <img v-if="userProfile?.avatar && !avatarFailed" :src="getAvatarUrl(userProfile.avatar)" alt="" @error="onAvatarError" />
            <span v-else>{{ (userProfile?.nickname || currentUser).charAt(0).toUpperCase() }}</span>
          </span>
          <span class="gus-info">
            <span class="gus-name">{{ userProfile?.nickname || currentUser }}</span>
            <span class="gus-role">{{ roleLabel }}</span>
          </span>
        </button>
        <button class="anime-btn xs" @click="handleLogout">
          <LogOut :size="14" />
        </button>
      </div>
    </aside>

    <main id="main-content" class="anime-main" tabindex="-1">
      <div class="anime-mobile-topbar">
        <button
          class="anime-mobile-menu-toggle"
          type="button"
          :aria-expanded="mobileNavOpen"
          aria-controls="primary-navigation"
          :aria-label="mobileNavOpen ? '关闭导航菜单' : '打开导航菜单'"
          @click="mobileNavOpen = !mobileNavOpen"
        >
          <X v-if="mobileNavOpen" :size="18" />
          <Menu v-else :size="18" />
        </button>
        <span class="anime-mobile-topbar-title">ChatBase 工作台</span>
      </div>
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
import { Home, BarChart3, Users, BookOpen, MessageCircle, Mail, HelpCircle, LogOut, Bot, ClipboardList, Cpu, MessageSquare, Menu, X } from 'lucide-vue-next'
import UserProfile from './components/UserProfile.vue'
import { getCurrentUser as fetchUserProfile, logout } from './api/user'
import type { UserVO } from './api/user'

const router = useRouter()
const route = useRoute()

const isLoginPage = computed(() => route.path === '/login')
const currentUser = ref('')
const isAdmin = ref(false)
const role = ref('')
const mobileNavOpen = ref(false)

function closeMobileNav() {
  mobileNavOpen.value = false
}

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
const avatarFailed = ref(false)

watch(userProfile, () => { avatarFailed.value = false })

async function openUserProfile() {
  closeMobileNav()
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

function onAvatarError() {
  avatarFailed.value = true
}

async function handleLogout() {
  try {
    await logout()
  } catch {
    // 忽略注销接口失败，本地仍清理
  }
  localStorage.removeItem('chatbase_token')
  localStorage.removeItem('chatbase_user')
  localStorage.removeItem('chatbase_original_username')
  localStorage.removeItem('chatbase_role')
  localStorage.removeItem('chatbase_admin_id')
  router.push('/login')
}

watch(() => route.path, () => {
  syncAuthState()
  closeMobileNav()
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
