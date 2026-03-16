import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import ChatPage from './pages/ChatPage.vue'
import UploadPage from './pages/UploadPage.vue'
import './styles.css'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/chat' },
    { path: '/chat', component: ChatPage },
    { path: '/upload', component: UploadPage },
  ],
})

createApp(App).use(router).mount('#app')

