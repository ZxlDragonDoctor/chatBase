import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import ChatPage from './pages/ChatPage.vue'
import DashboardPage from './pages/DashboardPage.vue'
import StatisticsPage from './pages/StatisticsPage.vue'
import ImGroupsPage from './pages/ImGroupsPage.vue'
import KnowledgePage from './pages/KnowledgePage.vue'
import LoginPage from './pages/LoginPage.vue'
import FeedbackPage from './pages/FeedbackPage.vue'
import FaqPage from './pages/FaqPage.vue'
import './styles/anime.css'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/console/dashboard' },
    { path: '/login', component: LoginPage, meta: { public: true } },
    { path: '/chat', component: ChatPage, meta: { public: true } },
    { path: '/feedback', component: FeedbackPage, meta: { public: true } },
    { path: '/console', redirect: '/console/dashboard' },
    { path: '/console/dashboard', component: DashboardPage },
    { path: '/console/statistics', component: StatisticsPage },
    { path: '/console/im', component: ImGroupsPage },
    { path: '/console/knowledge', component: KnowledgePage },
    { path: '/console/faq', component: FaqPage },
  ],
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('chatbase_token')
  const isPublic = to.meta?.public
  
  if (!token && !isPublic) {
    next('/login')
  } else if (token && to.path === '/login') {
    next('/console/dashboard')
  } else {
    next()
  }
})

createApp(App).use(router).mount('#app')