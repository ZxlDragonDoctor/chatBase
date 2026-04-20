<template>
  <div class="anime-bg-decor"></div>
  <div class="anime-floating-stars">
    <span class="anime-star" style="top: 10%; left: 5%;">✧</span>
    <span class="anime-star" style="top: 30%; left: 85%; animation-delay: 0.5s;">✿</span>
    <span class="anime-star" style="top: 70%; left: 15%; animation-delay: 1s;">★</span>
    <span class="anime-star" style="top: 90%; left: 90%; animation-delay: 1.5s;">✧</span>
  </div>
  <div class="anime-app">
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
        <RouterLink class="anime-nav-item" to="/chat" active-class="active">
          <MessageCircle class="anime-nav-icon" :size="22" />
          <span>问答</span>
        </RouterLink>
      </nav>
      <div class="anime-nav-footer">
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
import { RouterLink, RouterView } from 'vue-router'
import { Home, BarChart3, Users, BookOpen, MessageCircle } from 'lucide-vue-next'
</script>