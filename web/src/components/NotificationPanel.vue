<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import AppAvatar from '@/components/AppAvatar.vue'
import { formatRelativeTime } from '@/utils/format'
import { fetchNotifications, fetchUnreadCount, markNotificationRead } from '@/api/notification'

const props = defineProps({
  visible: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'unread-change'])

const router = useRouter()
const loading = ref(false)
const items = ref([])
const unreadCount = ref(0)

let pollTimer = null

async function loadUnread() {
  try {
    const res = await fetchUnreadCount()
    if (res.data.code === 200) {
      unreadCount.value = res.data.data?.count || 0
      emit('unread-change', unreadCount.value)
    }
  } catch {
    /* ignore */
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await fetchNotifications(1, 30)
    if (res.data.code === 200) {
      items.value = res.data.data?.records || []
    }
  } finally {
    loading.value = false
  }
}

async function openPanel() {
  await Promise.all([loadList(), loadUnread()])
}

async function onItemClick(item) {
  if (!item.isRead) {
    await markNotificationRead(item.id)
    item.isRead = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    emit('unread-change', unreadCount.value)
  }
  emit('close')
  if (item.linkPath) {
    router.push(item.linkPath)
  }
}

async function markAllRead() {
  await markNotificationRead()
  items.value.forEach(i => { i.isRead = true })
  unreadCount.value = 0
  emit('unread-change', 0)
}

function typeLabel(type) {
  if (type === 1) return '赞了你的视频'
  if (type === 2) return '赞了你的评论'
  if (type === 3) return '发来私信'
  if (type === 4) return '通过了你的视频'
  return '通知'
}

watch(() => props.visible, v => {
  if (v) openPanel()
})

onMounted(() => {
  loadUnread()
  pollTimer = setInterval(loadUnread, 30000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})

defineExpose({ loadUnread, unreadCount })
</script>

<template>
  <div v-if="visible" class="panel">
    <div class="panel-head">
      <span class="title">通知</span>
      <button type="button" class="link-btn" @click="markAllRead">全部已读</button>
    </div>
    <div v-loading="loading" class="panel-body">
      <button
        v-for="item in items"
        :key="item.id"
        type="button"
        class="item"
        :class="{ unread: !item.isRead }"
        @click="onItemClick(item)"
      >
        <AppAvatar :size="36" :src="item.actorAvatar" :name="item.actorNickname" />
        <div class="item-body">
          <div class="item-top">
            <span class="actor">{{ item.actorNickname }}</span>
            <span class="type">{{ typeLabel(item.type) }}</span>
          </div>
          <p class="preview">{{ item.preview }}</p>
          <span class="time">{{ formatRelativeTime(item.createTime) }}</span>
        </div>
      </button>
      <el-empty v-if="!loading && !items.length" description="暂无通知" :image-size="64" />
    </div>
  </div>
</template>

<style scoped>
.panel {
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  width: 360px;
  max-height: 420px;
  background: #fff;
  border: 1px solid var(--doinb-border-light);
  border-radius: var(--doinb-radius-sm);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  z-index: 102;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--doinb-border-light);
}

.title {
  font-weight: 600;
  font-size: 15px;
}

.link-btn {
  border: none;
  background: none;
  color: var(--doinb-primary);
  cursor: pointer;
  font-size: 13px;
}

.panel-body {
  overflow-y: auto;
  flex: 1;
}

.item {
  display: flex;
  gap: 10px;
  width: 100%;
  padding: 12px 16px;
  border: none;
  background: none;
  text-align: left;
  cursor: pointer;
  border-bottom: 1px solid var(--doinb-border-light);
}

.item:hover {
  background: var(--doinb-bg-page);
}

.item.unread {
  background: var(--doinb-primary-bg);
}

.item-body {
  flex: 1;
  min-width: 0;
}

.item-top {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-bottom: 4px;
}

.actor {
  font-weight: 500;
  font-size: 14px;
}

.type {
  font-size: 12px;
  color: var(--doinb-text-secondary);
}

.preview {
  font-size: 13px;
  color: var(--doinb-text-regular);
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time {
  font-size: 12px;
  color: var(--doinb-text-placeholder);
}
</style>
