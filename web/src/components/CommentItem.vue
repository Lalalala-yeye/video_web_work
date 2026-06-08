<script setup>
import AppAvatar from './AppAvatar.vue'
import { formatRelativeTime } from '@/utils/format'

defineProps({
  userNickname: { type: String, default: '匿名用户' },
  userAvatar: { type: String, default: '' },
  content: { type: String, required: true },
  createTime: { type: String, default: '' }
})
</script>

<template>
  <div class="comment-item">
    <AppAvatar :size="40" :src="userAvatar" :name="userNickname" />
    <div class="body">
      <div class="head">
        <span class="name">{{ userNickname }}</span>
        <span class="time">{{ formatRelativeTime(createTime) }}</span>
      </div>
      <p class="content">{{ content }}</p>
    </div>
  </div>
</template>

<style scoped>
.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid var(--doinb-border-light);
}

.comment-item:last-child {
  border-bottom: none;
}

.head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.name {
  font-size: 14px;
  font-weight: 500;
  color: var(--doinb-text-regular);
}

.time {
  font-size: 12px;
  color: var(--doinb-text-placeholder);
}

.content {
  font-size: 14px;
  color: var(--doinb-text-primary);
  line-height: 1.6;
}
</style>
