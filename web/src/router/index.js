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
    {
      path: '/studio',
      component: () => import('../views/studio/StudioLayout.vue'),
      children: [
        { path: '', redirect: '/studio/upload' },
        { path: 'upload', name: 'studio-upload', component: () => import('../views/studio/StudioUploadView.vue') },
        { path: 'edit', name: 'studio-edit', component: () => import('../views/studio/StudioEditView.vue') },
        { path: 'edit/:id', name: 'studio-edit-id', component: () => import('../views/studio/StudioEditView.vue') },
        { path: 'live', name: 'studio-live', component: () => import('../views/studio/StudioLiveView.vue') },
      ],
    },
    {
      path: '/admin',
      component: () => import('../views/admin/AdminLayout.vue'),
      children: [
        { path: '', redirect: '/admin/pending' },
        { path: 'pending', name: 'admin-pending', component: () => import('../views/admin/AdminPendingView.vue') },
        { path: 'report', name: 'admin-report', component: () => import('../views/admin/AdminReportView.vue') },
      ],
    },
    { path: '/upload', redirect: '/studio/upload' },
    { path: '/profile', name: 'profile', component: () => import('../views/ProfileView.vue') },
    { path: '/user/:id', name: 'user-showcase', component: () => import('../views/UserShowcaseView.vue') },
    { path: '/messages/:roomId', name: 'message-room', component: () => import('../views/MessageRoomView.vue') },
  ],
})

export default router
