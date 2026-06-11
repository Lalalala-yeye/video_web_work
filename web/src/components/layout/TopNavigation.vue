<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search, ArrowDown, User, SwitchButton, Menu, Close } from '@element-plus/icons-vue'
import AppAvatar from '@/components/AppAvatar.vue'
import NotificationPanel from '@/components/NotificationPanel.vue'
import {
  getUser,
  isLoggedIn,
  isAdmin,
  clearAuth,
  clearAllAccounts,
  refreshStoredUser,
  getAccounts,
  switchAccount,
  openLoginInNewTab,
  AUTH_UPDATED_EVENT,
} from '@/utils/auth'
import { FAVICON_URL, BELL_ICON_URL } from '@/constants/staticAssets'

const emit = defineEmits(['logout'])
const router = useRouter()
const route = useRoute()

const searchQuery = ref('')
const showMenu = ref(false)
const showNotifications = ref(false)
const showMobileNav = ref(false)
const unreadCount = ref(0)
const useFallbackBell = ref(false)
const notificationPanelRef = ref(null)
const loggedIn = ref(isLoggedIn())
const user = ref(getUser())
const accounts = ref(getAccounts())

const otherAccounts = computed(() =>
  accounts.value.filter(a => Number(a.user?.id) !== Number(user.value?.id))
)

const navItems = computed(() => {
  const items = [
    { path: '/', label: '首页' },
    { path: '/live', label: '直播' },
    { path: '/subscribe', label: '关注' },
  ]
  if (loggedIn.value) {
    items.push({ path: '/studio', label: '创作中心' })
  }
  return items
})

const displayName = computed(() => user.value?.nickname || user.value?.username || '')

onMounted(() => {
  refreshAuth()
  window.addEventListener(AUTH_UPDATED_EVENT, onAuthUpdated)
})

onUnmounted(() => {
  window.removeEventListener(AUTH_UPDATED_EVENT, onAuthUpdated)
})

watch(() => route.path, () => {
  showMobileNav.value = false
  refreshAuth()
})

function onAuthUpdated(e) {
  user.value = e.detail ?? getUser()
  loggedIn.value = isLoggedIn()
  accounts.value = getAccounts()
}

async function refreshAuth() {
  loggedIn.value = isLoggedIn()
  accounts.value = getAccounts()
  if (loggedIn.value) {
    user.value = await refreshStoredUser()
  } else {
    user.value = null
  }
}

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

function onSearch() {
  const q = searchQuery.value.trim()
  if (!q) return
  showMobileNav.value = false
  router.push({ path: '/search', query: { keyword: q } })
}

async function onSwitchAccount(userId) {
  showMenu.value = false
  if (switchAccount(userId)) {
    await refreshAuth()
    router.push('/')
  }
}

function onAddAccount() {
  showMenu.value = false
  openLoginInNewTab()
}

async function onLogout() {
  showMenu.value = false
  showNotifications.value = false
  try {
    if (loggedIn.value) {
      const { logout } = await import('@/api/user')
      await logout()
    }
  } catch {
    /* ignore */
  }
  clearAuth()
  await refreshAuth()
  emit('logout')
  router.push('/')
}

async function onLogoutAll() {
  showMenu.value = false
  showNotifications.value = false
  clearAllAccounts()
  await refreshAuth()
  emit('logout')
  router.push('/')
}

function toggleNotifications() {
  showMenu.value = false
  showMobileNav.value = false
  showNotifications.value = !showNotifications.value
}

function onUnreadChange(count) {
  unreadCount.value = count
}

function closeNotifications() {
  showNotifications.value = false
}

function toggleMobileNav() {
  showMenu.value = false
  showNotifications.value = false
  showMobileNav.value = !showMobileNav.value
}
</script>

<template>
  <header class="top-nav">
    <div class="inner">
      <router-link to="/" class="logo">
        <img :src="FAVICON_URL" alt="doinb" class="logo-icon-img" />
        <span class="logo-text">doinb</span>
      </router-link>

      <button type="button" class="mobile-menu-btn" aria-label="打开导航" @click="toggleMobileNav">
        <el-icon><Close v-if="showMobileNav" /><Menu v-else /></el-icon>
      </button>

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
        <div class="notify-wrap">
          <button type="button" class="notify-btn" aria-label="通知" @click="toggleNotifications">
            <img
              v-if="!useFallbackBell"
              :src="BELL_ICON_URL"
              alt=""
              class="notify-icon"
              @error="useFallbackBell = true"
            />
            <span v-else class="notify-fallback">!</span>
            <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </button>
          <div v-if="showNotifications" class="menu-backdrop" @click="closeNotifications" />
          <NotificationPanel
            ref="notificationPanelRef"
            :visible="showNotifications"
            @close="closeNotifications"
            @unread-change="onUnreadChange"
          />
        </div>
        <button type="button" class="user-trigger" @click="showMenu = !showMenu; showNotifications = false">
          <AppAvatar :size="32" :src="user.avatar" :name="displayName" />
          <span class="user-name">{{ displayName }}</span>
          <el-icon><ArrowDown /></el-icon>
        </button>
        <div v-if="showMenu" class="menu-backdrop" @click="showMenu = false" />
        <div v-if="showMenu" class="dropdown dropdown--wide">
          <div v-if="otherAccounts.length" class="account-section">
            <p class="section-label">切换账号</p>
            <button
              v-for="acc in otherAccounts"
              :key="acc.user.id"
              type="button"
              class="menu-item account-item"
              @click="onSwitchAccount(acc.user.id)"
            >
              <AppAvatar :size="24" :src="acc.user.avatar" :name="acc.user.nickname || acc.user.username" />
              <span>{{ acc.user.nickname || acc.user.username }}</span>
            </button>
          </div>
          <button type="button" class="menu-item" @click="onAddAccount">新标签页登录其他账号</button>
          <router-link to="/profile" class="menu-item" @click="showMenu = false">
            <el-icon><User /></el-icon>
            个人中心
          </router-link>
          <router-link v-if="isAdmin()" to="/admin" class="menu-item menu-item--admin" @click="showMenu = false">
            管理后台
          </router-link>
          <button type="button" class="menu-item menu-item--danger" @click="onLogout">
            <el-icon><SwitchButton /></el-icon>
            退出当前账号
          </button>
          <button
            v-if="accounts.length > 1"
            type="button"
            class="menu-item menu-item--danger"
            @click="onLogoutAll"
          >
            退出全部账号
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

    <div v-if="showMobileNav" class="mobile-panel">
      <nav class="mobile-links">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="mobile-link"
          :class="{ active: isActive(item.path) }"
        >
          {{ item.label }}
        </router-link>
      </nav>
      <form class="mobile-search-form" @submit.prevent="onSearch">
        <el-input v-model="searchQuery" placeholder="搜索视频、直播、用户" clearable />
        <el-button type="primary" native-type="submit">搜索</el-button>
      </form>
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
  padding: 0 22px;
  display: flex;
  align-items: center;
  gap: 28px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.logo-icon-img {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  object-fit: cover;
  box-shadow: 0 8px 18px color-mix(in srgb, var(--doinb-macaron-c) 18%, transparent);
}

.logo-text {
  font-size: 21px;
  font-weight: 800;
  color: var(--doinb-text-primary);
}

.mobile-menu-btn {
  display: none;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--doinb-text-primary);
  cursor: pointer;
  align-items: center;
  justify-content: center;
}

.nav-links {
  display: flex;
  gap: 28px;
  flex-shrink: 0;
}

.nav-link {
  font-size: 14px;
  font-weight: 700;
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
  max-width: 430px;
  position: relative;
  display: flex;
  align-items: center;
}

.search-input {
  width: 100%;
}

.search-btn {
  position: absolute;
  right: 10px;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--doinb-text-secondary);
  display: flex;
  padding: 5px;
  border-radius: 999px;
}

.search-btn:hover {
  color: var(--doinb-primary);
}

.user-area {
  position: relative;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.notify-wrap {
  position: relative;
}

.notify-btn {
  position: relative;
  width: 36px;
  height: 36px;
  border: none;
  background: none;
  border-radius: 999px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.notify-btn:hover,
.user-trigger:hover,
.mobile-menu-btn:hover {
  background: color-mix(in srgb, var(--doinb-primary-bg) 80%, white);
}

.notify-icon {
  width: 22px;
  height: 22px;
}

.notify-fallback {
  font-size: 18px;
  line-height: 1;
  font-weight: 700;
}

.badge {
  position: absolute;
  top: 2px;
  right: 2px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: var(--doinb-macaron-b);
  color: #fff;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border: none;
  background: none;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.2s;
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
  border-radius: 4px;
  box-shadow:
    0 18px 42px rgba(38, 42, 64, 0.12),
    0 0 0 1px color-mix(in srgb, var(--doinb-border-light) 88%, transparent);
  z-index: 102;
  overflow: hidden;
}

.dropdown--wide {
  width: 220px;
}

.account-section {
  border-bottom: 1px solid var(--doinb-border-light);
  padding-bottom: 4px;
  margin-bottom: 4px;
}

.section-label {
  padding: 8px 16px 4px;
  font-size: 12px;
  color: var(--doinb-text-secondary);
}

.account-item span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  background: color-mix(in srgb, var(--doinb-primary-bg) 78%, white);
}

.menu-item--admin {
  color: var(--doinb-primary);
  font-weight: 500;
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

.mobile-panel {
  display: none;
}

@media (max-width: 768px) {
  .inner {
    gap: 12px;
    padding: 0 14px;
  }

  .mobile-menu-btn {
    display: flex;
  }

  .nav-links,
  .search-form,
  .user-name {
    display: none;
  }

  .auth-btns {
    margin-left: auto;
  }

  .mobile-panel {
    display: block;
    position: fixed;
    top: var(--doinb-header-height);
    left: 0;
    right: 0;
    padding: 12px 14px 16px;
    background: #fff;
    border-bottom: 1px solid var(--doinb-border-light);
    box-shadow: 0 18px 42px rgba(38, 42, 64, 0.12);
    z-index: 99;
  }

  .mobile-links {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
    margin-bottom: 12px;
  }

  .mobile-link {
    padding: 10px 12px;
    border-radius: var(--doinb-radius-sm);
    background: var(--doinb-bg-page);
    color: var(--doinb-text-regular);
    font-weight: 600;
  }

  .mobile-link.active {
    color: var(--doinb-primary);
    background: var(--doinb-primary-bg);
  }

  .mobile-search-form {
    display: grid;
    grid-template-columns: 1fr auto;
    gap: 8px;
  }
}
</style>
