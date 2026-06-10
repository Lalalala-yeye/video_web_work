<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { isLoggedIn, isAdmin } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const menuItems = [
  { key: 'pending', label: '待审视频', path: '/admin/pending' },
  { key: 'report', label: '举报复审', path: '/admin/report' },
]

const activeKey = computed(() => {
  if (route.path.includes('/admin/report')) return 'report'
  return 'pending'
})

onMounted(() => {
  if (!isLoggedIn()) {
    ElMessage.warning('请先登录')
    router.replace('/login')
    return
  }
  if (!isAdmin()) {
    ElMessage.error('需要管理员权限')
    router.replace('/')
  }
})

function go(path) {
  router.push(path)
}
</script>

<template>
  <div class="admin-layout page-container">
    <aside class="admin-sidebar">
      <h2 class="sidebar-title">管理后台</h2>
      <nav class="sidebar-nav">
        <button
          v-for="item in menuItems"
          :key="item.key"
          type="button"
          class="sidebar-item"
          :class="{ active: activeKey === item.key }"
          @click="go(item.path)"
        >
          {{ item.label }}
        </button>
      </nav>
      <router-link to="/" class="back-link">返回首页</router-link>
    </aside>
    <main class="admin-main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  min-height: calc(100vh - var(--doinb-header-height) - 48px);
}

.admin-sidebar {
  width: 200px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid var(--doinb-border-light);
  border-radius: var(--doinb-radius);
  padding: 16px 0;
  position: sticky;
  top: calc(var(--doinb-header-height) + 16px);
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  padding: 0 16px 12px;
  margin: 0;
  border-bottom: 1px solid var(--doinb-border-light);
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  padding-top: 8px;
}

.sidebar-item {
  border: none;
  background: none;
  text-align: left;
  padding: 12px 16px;
  font-size: 14px;
  color: var(--doinb-text-regular);
  cursor: pointer;
}

.sidebar-item.active {
  color: var(--doinb-primary);
  background: #ecf5ff;
  font-weight: 500;
  border-right: 3px solid var(--doinb-primary);
}

.back-link {
  display: block;
  padding: 12px 16px;
  margin-top: 12px;
  font-size: 13px;
  color: var(--doinb-text-secondary);
  border-top: 1px solid var(--doinb-border-light);
}
</style>
