<script setup>
import { computed, ref, onMounted, provide, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchPendingVideos, fetchReportReviewVideos } from '@/api/admin'
import { getUser } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const pendingCount = ref(0)
const reportCount = ref(0)

const menuItems = computed(() => [
  { key: 'dashboard', label: '概览', path: '/admin/dashboard' },
  { key: 'pending', label: '待审视频', path: '/admin/pending', badge: pendingCount.value },
  { key: 'report', label: '举报复审', path: '/admin/report', badge: reportCount.value },
])

const activeKey = computed(() => {
  if (route.path.includes('/admin/report')) return 'report'
  if (route.path.includes('/admin/pending')) return 'pending'
  if (route.path.includes('/admin/preview')) return 'pending'
  return 'dashboard'
})

const adminName = computed(() => {
  const user = getUser()
  return user?.nickname || user?.username || '管理员'
})

async function loadCounts() {
  try {
    const [pendingRes, reportRes] = await Promise.all([
      fetchPendingVideos(1, 1),
      fetchReportReviewVideos(1, 1),
    ])
    if (pendingRes.data.code === 200) {
      pendingCount.value = pendingRes.data.data?.total || 0
    }
    if (reportRes.data.code === 200) {
      reportCount.value = reportRes.data.data?.total || 0
    }
  } catch {
    /* ignore */
  }
}

function go(path) {
  router.push(path)
}

provide('refreshAdminCounts', loadCounts)

watch(() => route.path, () => {
  if (route.path.startsWith('/admin')) loadCounts()
})

onMounted(loadCounts)
</script>

<template>
  <div class="admin-layout page-container">
    <aside class="admin-sidebar">
      <h2 class="sidebar-title">管理后台</h2>
      <p class="sidebar-user">{{ adminName }}</p>
      <nav class="sidebar-nav">
        <button
          v-for="item in menuItems"
          :key="item.key"
          type="button"
          class="sidebar-item"
          :class="{ active: activeKey === item.key }"
          @click="go(item.path)"
        >
          <span>{{ item.label }}</span>
          <span v-if="item.badge > 0" class="badge">{{ item.badge > 99 ? '99+' : item.badge }}</span>
        </button>
      </nav>
      <router-link to="/studio" class="back-link">创作中心</router-link>
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

.admin-main {
  flex: 1;
  min-width: 0;
  width: 100%;
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
  padding: 0 16px 4px;
  margin: 0;
}

.sidebar-user {
  padding: 0 16px 12px;
  margin: 0;
  font-size: 12px;
  color: var(--doinb-text-secondary);
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.sidebar-item.active {
  color: var(--doinb-primary);
  background: var(--doinb-primary-bg);
  font-weight: 500;
  border-right: 3px solid var(--doinb-primary);
}

.badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #f56c6c;
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
  font-weight: 600;
}

.back-link {
  display: block;
  padding: 10px 16px;
  font-size: 13px;
  color: var(--doinb-text-secondary);
}

.back-link + .back-link {
  padding-top: 0;
}

.back-link:first-of-type {
  margin-top: 12px;
  border-top: 1px solid var(--doinb-border-light);
  padding-top: 12px;
}
</style>
