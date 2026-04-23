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
      </nav>
      <div class="anime-nav-footer">
        <div class="anime-user-info" v-if="currentUser">
          <User class="anime-nav-icon" :size="16" />
          <span class="anime-user-name">{{ currentUser }}</span>
        </div>
        <button class="anime-btn ghost sm" v-if="currentUser" @click="handleLogout">
          <LogOut :size="16" />
          <span>退出</span>
        </button>
        <div class="anime-status-badge">
          <span class="anime-status-dot"></span>
          <span>✧ STATUS: ONLINE ✧</span>
        </div>
      </div>
    </aside>
    <main class="anime-main">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRouter, useRoute } from 'vue-router'
import { Home, BarChart3, Users, BookOpen, MessageCircle, Mail, HelpCircle, User, LogOut, Bot } from 'lucide-vue-next'

const router = useRouter()
const route = useRoute()

const isLoginPage = computed(() => route.path === '/login')
const currentUser = computed(() => localStorage.getItem('chatbase_user'))

function handleLogout() {
  localStorage.removeItem('chatbase_token')
  localStorage.removeItem('chatbase_user')
  router.push('/login')
}
</script>

<style scoped>
.anime-user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--anime-bg);
  border-radius: var(--anime-radius-lg);
  margin-bottom: 12px;
}

.anime-user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--anime-purple);
}

.anime-btn.sm {
  padding: 6px 12px;
  font-size: 12px;
  margin-bottom: 12px;
}
</style>