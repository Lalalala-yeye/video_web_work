<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { isLoggedIn, isAdmin } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const menuItems = [
  { key: 'upload', label: '上传视频', path: '/studio/upload' },
  { key: 'edit', label: '修改视频', path: '/studio/edit' },
  { key: 'live', label: '我的直播', path: '/studio/live' },
]

const activeKey = computed(() => {
  if (route.path.startsWith('/studio/edit')) return 'edit'
  if (route.path.startsWith('/studio/live')) return 'live'
  return 'upload'
})

onMounted(() => {
  if (!isLoggedIn()) {
    ElMessage.warning('请先登录')
    router.replace('/login')
  }
})

function go(path) {
  router.push(path)
}

function goAdmin() {
  router.push('/admin/pending')
}
</script>

<template>
  <div class="studio-layout page-container">
    <aside class="studio-sidebar">
      <h2 class="sidebar-title">创作中心</h2>
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
        <button v-if="isAdmin()" type="button" class="sidebar-item admin-link" @click="goAdmin">
          管理后台
        </button>
      </nav>
    </aside>
    <main class="studio-main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.studio-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  min-height: calc(100vh - var(--doinb-header-height) - 48px);
}

.studio-main {
  flex: 1;
  min-width: 0;
  width: 100%;
}

.studio-sidebar {
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
  transition: background 0.2s, color 0.2s;
}

.sidebar-item:hover {
  background: var(--doinb-bg-page);
}

.sidebar-item.active {
  color: var(--doinb-primary);
  background: #ecf5ff;
  font-weight: 500;
  border-right: 3px solid var(--doinb-primary);
}

.admin-link {
  margin-top: 8px;
  border-top: 1px solid var(--doinb-border-light);
  color: #e6a23c;
}

@media (max-width: 768px) {
  .studio-layout {
    flex-direction: column;
  }

  .studio-sidebar {
    width: 100%;
    position: static;
  }

  .sidebar-nav {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 8px;
    padding: 8px 12px;
  }

  .sidebar-item.active {
    border-right: none;
    border-bottom: 2px solid var(--doinb-primary);
  }
}
</style>
