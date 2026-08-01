import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import ChatPage from './pages/ChatPage.vue'
import DashboardPage from './pages/DashboardPage.vue'
import StatisticsPage from './pages/StatisticsPage.vue'
import ImGroupsPage from './pages/ImGroupsPage.vue'
import ImSingleChatPage from './pages/ImSingleChatPage.vue'
import KnowledgePage from './pages/KnowledgePage.vue'
import LoginPage from './pages/LoginPage.vue'
import FeedbackPage from './pages/FeedbackPage.vue'
import FeedbackManagePage from './pages/FeedbackManagePage.vue'
import FaqPage from './pages/FaqPage.vue'
import AppPage from './pages/AppPage.vue'
import BotManagePage from './pages/BotManagePage.vue'
import AdminAppsPage from './pages/AdminAppsPage.vue'
import AdminKbsPage from './pages/AdminKbsPage.vue'
import UserManagePage from './pages/UserManagePage.vue'
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
    { path: '/console/im/single', component: ImSingleChatPage },
    { path: '/console/knowledge', component: KnowledgePage },
    { path: '/console/app', component: AppPage },
    { path: '/console/bots', component: BotManagePage },
    { path: '/console/faq', component: FaqPage },
    { path: '/console/feedback-manage', component: FeedbackManagePage, meta: { requiresAdmin: true } },
    { path: '/console/admin/apps', component: AdminAppsPage, meta: { requiresAdmin: true } },
    { path: '/console/admin/kbs', component: AdminKbsPage, meta: { requiresAdmin: true } },
    { path: '/console/admin/users', component: UserManagePage, meta: { requiresAdmin: true } },
  ],
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('chatbase_token')
  const userRole = localStorage.getItem('chatbase_role')
  const isPublic = to.meta?.public
  const requiresAdmin = to.meta?.requiresAdmin
  
  if (!token && !isPublic) {
    next('/login')
  } else if (token && to.path === '/login') {
    next('/console/dashboard')
  } else if (requiresAdmin && userRole !== 'admin') {
    next('/console/dashboard')
  } else {
    next()
  }
})

createApp(App).use(router).mount('#app')