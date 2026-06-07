<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { RouterLink, RouterView } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUser, clearAuth, isLoggedIn } from '@/utils/auth'
import { logout as logoutApi } from '@/api/user'

const router = useRouter()
const route = useRoute()
const loggedIn = ref(isLoggedIn())
const user = ref(getUser())

onMounted(() => {
  refreshAuth()
})

watch(() => route.path, () => {
  refreshAuth()
})

function refreshAuth() {
  loggedIn.value = isLoggedIn()
  user.value = getUser()
}

async function handleLogout() {
  try {
    if (loggedIn.value) {
      await logoutApi()
    }
  } catch {
    // 网络失败也清除本地登录态
  }
  clearAuth()
  refreshAuth()
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>

<template>
  <div class="layout">
    <header class="header">
      <router-link to="/" class="brand">doinb</router-link>
      <nav class="nav">
        <RouterLink to="/">首页</RouterLink>
        <template v-if="loggedIn">
          <span class="nickname">{{ user?.nickname || user?.username }}</span>
          <a href="#" class="nav-link" @click.prevent="handleLogout">退出</a>
        </template>
        <template v-else>
          <RouterLink to="/login">登录</RouterLink>
          <RouterLink to="/register">注册</RouterLink>
        </template>
      </nav>
    </header>
    <main class="main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 56px;
  border-bottom: 1px solid #eee;
  background: #fff;
}

.brand {
  font-size: 1.25rem;
  font-weight: 700;
  color: #409eff;
  text-decoration: none;
}

.nav {
  display: flex;
  align-items: center;
  gap: 16px;
}

.nav a {
  color: #333;
  text-decoration: none;
  font-size: 14px;
}

.nav a.router-link-active {
  color: #409eff;
}

.nav-link {
  cursor: pointer;
}

.nickname {
  font-size: 14px;
  color: #666;
}

.main {
  flex: 1;
  max-width: 960px;
  width: 100%;
  margin: 0 auto;
  padding: 24px;
  box-sizing: border-box;
}
</style>
