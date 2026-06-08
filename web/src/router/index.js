import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
    { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue') },
    { path: '/video/:id', name: 'video', component: () => import('../views/VideoDetailView.vue') },
    { path: '/live', name: 'live', component: () => import('../views/LiveListView.vue') },
    { path: '/live/:id', name: 'live-room', component: () => import('../views/LiveRoomView.vue') },
    { path: '/subscribe', name: 'subscribe', component: () => import('../views/SubscribeView.vue') },
    { path: '/search', name: 'search', component: () => import('../views/SearchView.vue') },
    { path: '/upload', name: 'upload', component: () => import('../views/UploadView.vue') },
    { path: '/profile', name: 'profile', component: () => import('../views/ProfileView.vue') },
  ],
})

export default router
