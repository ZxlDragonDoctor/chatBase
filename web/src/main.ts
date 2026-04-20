import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import ChatPage from './pages/ChatPage.vue'
import DashboardPage from './pages/DashboardPage.vue'
import StatisticsPage from './pages/StatisticsPage.vue'
import ImGroupsPage from './pages/ImGroupsPage.vue'
import KnowledgePage from './pages/KnowledgePage.vue'
import LoginPage from './pages/LoginPage.vue'
import './styles/anime.css'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/console/dashboard' },
    { path: '/login', component: LoginPage },
    { path: '/console', redirect: '/console/dashboard' },
    { path: '/console/dashboard', component: DashboardPage },
    { path: '/console/statistics', component: StatisticsPage },
    { path: '/console/im', component: ImGroupsPage },
    { path: '/console/knowledge', component: KnowledgePage },
    { path: '/chat', component: ChatPage },
  ],
})

createApp(App).use(router).mount('#app')