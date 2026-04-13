import { createApp } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import App from './App.vue';
import ChatPage from './pages/ChatPage.vue';
import UploadPage from './pages/UploadPage.vue';
import DashboardPage from './pages/DashboardPage.vue';
import ImGroupsPage from './pages/ImGroupsPage.vue';
import './styles.css';
const router = createRouter({
    history: createWebHistory(),
    routes: [
        { path: '/', redirect: '/console/dashboard' },
        { path: '/console', redirect: '/console/dashboard' },
        { path: '/console/dashboard', component: DashboardPage },
        { path: '/console/im', component: ImGroupsPage },
        { path: '/chat', component: ChatPage },
        { path: '/upload', component: UploadPage },
    ],
});
createApp(App).use(router).mount('#app');
