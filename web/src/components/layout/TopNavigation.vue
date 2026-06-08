<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search, ArrowDown, User, SwitchButton } from '@element-plus/icons-vue'
import AppAvatar from '@/components/AppAvatar.vue'
import { getUser, isLoggedIn, clearAuth } from '@/utils/auth'
import { logout as logoutApi } from '@/api/user'

const emit = defineEmits(['logout'])
const router = useRouter()
const route = useRoute()

const searchQuery = ref('')
const showMenu = ref(false)
const loggedIn = ref(isLoggedIn())
const user = ref(getUser())

const navItems = [
  { path: '/', label: '首页' },
  { path: '/live', label: '直播' },
  { path: '/subscribe', label: '订阅' }
]

const displayName = computed(() => user.value?.nickname || user.value?.username || '')

onMounted(refreshAuth)
watch(() => route.path, refreshAuth)

function refreshAuth() {
  loggedIn.value = isLoggedIn()
  user.value = getUser()
}

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

function onSearch() {
  const q = searchQuery.value.trim()
  if (!q) return
  router.push({ path: '/search', query: { keyword: q } })
}

async function onLogout() {
  showMenu.value = false
  try {
    if (loggedIn.value) await logoutApi()
  } catch {
    /* ignore */
  }
  clearAuth()
  refreshAuth()
  emit('logout')
  router.push('/')
}
</script>

<template>
  <header class="top-nav">
    <div class="inner">
      <router-link to="/" class="logo">
        <span class="logo-icon">D</span>
        <span class="logo-text">doinb</span>
      </router-link>

      <nav class="nav-links">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-link"
          :class="{ active: isActive(item.path) }"
        >
          {{ item.label }}
        </router-link>
      </nav>

      <form class="search-form" @submit.prevent="onSearch">
        <el-input
          v-model="searchQuery"
          placeholder="搜索视频、直播、用户"
          clearable
          class="search-input"
        />
        <button type="submit" class="search-btn" aria-label="搜索">
          <el-icon><Search /></el-icon>
        </button>
      </form>

      <div v-if="loggedIn && user" class="user-area">
        <button type="button" class="user-trigger" @click="showMenu = !showMenu">
          <AppAvatar :size="32" :src="user.avatar" :name="displayName" />
          <span class="user-name">{{ displayName }}</span>
          <el-icon><ArrowDown /></el-icon>
        </button>
        <div v-if="showMenu" class="menu-backdrop" @click="showMenu = false" />
        <div v-if="showMenu" class="dropdown">
          <router-link to="/profile" class="menu-item" @click="showMenu = false">
            <el-icon><User /></el-icon>
            个人中心
          </router-link>
          <button type="button" class="menu-item menu-item--danger" @click="onLogout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </button>
        </div>
      </div>
      <div v-else class="auth-btns">
        <router-link to="/login">
          <el-button text>登录</el-button>
        </router-link>
        <router-link to="/register">
          <el-button type="primary">注册</el-button>
        </router-link>
      </div>
    </div>
  </header>
</template>

<style scoped>
.top-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: var(--doinb-header-height);
  background: #fff;
  border-bottom: 1px solid var(--doinb-border-light);
  z-index: 100;
}

.inner {
  max-width: var(--doinb-content-max);
  margin: 0 auto;
  height: 100%;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: var(--doinb-primary);
  color: #fff;
  border-radius: var(--doinb-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: var(--doinb-text-primary);
}

.nav-links {
  display: flex;
  gap: 24px;
  flex-shrink: 0;
}

.nav-link {
  font-size: 15px;
  color: var(--doinb-text-regular);
  transition: color 0.2s;
}

.nav-link:hover,
.nav-link.active {
  color: var(--doinb-primary);
  font-weight: 500;
}

.search-form {
  flex: 1;
  max-width: 400px;
  position: relative;
  display: flex;
  align-items: center;
}

.search-input {
  width: 100%;
}

.search-btn {
  position: absolute;
  right: 8px;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--doinb-text-secondary);
  display: flex;
  padding: 4px;
}

.search-btn:hover {
  color: var(--doinb-primary);
}

.user-area {
  position: relative;
  flex-shrink: 0;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border: none;
  background: none;
  border-radius: var(--doinb-radius-sm);
  cursor: pointer;
  transition: background 0.2s;
}

.user-trigger:hover {
  background: var(--doinb-bg-page);
}

.user-name {
  font-size: 14px;
  color: var(--doinb-text-regular);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-backdrop {
  position: fixed;
  inset: 0;
  z-index: 101;
}

.dropdown {
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  width: 160px;
  background: #fff;
  border: 1px solid var(--doinb-border-light);
  border-radius: var(--doinb-radius-sm);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 102;
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 12px 16px;
  font-size: 14px;
  color: var(--doinb-text-regular);
  background: none;
  border: none;
  cursor: pointer;
  text-align: left;
  transition: background 0.2s;
}

.menu-item:hover {
  background: var(--doinb-bg-page);
}

.menu-item--danger {
  border-top: 1px solid var(--doinb-border-light);
}

.auth-btns {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .nav-links,
  .user-name {
    display: none;
  }
}
</style>
