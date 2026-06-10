<script setup>
import { ElMessage } from 'element-plus'
import AppAvatar from './AppAvatar.vue'
import CommentContent from './CommentContent.vue'
import { formatRelativeTime } from '@/utils/format'
import { reactComment } from '@/api/reaction'
import { isLoggedIn } from '@/utils/auth'
import { LIKE_ICON_URL, DISLIKE_ICON_URL } from '@/constants/staticAssets'

const props = defineProps({
  id: { type: Number, required: true },
  userId: { type: Number, default: null },
  userNickname: { type: String, default: '匿名用户' },
  userAvatar: { type: String, default: '' },
  content: { type: String, required: true },
  createTime: { type: String, default: '' },
  reactions: {
    type: Object,
    default: () => ({ likeCount: 0, dislikeCount: 0, userReaction: 0 })
  }
})

const emit = defineEmits(['reaction-updated'])

async function toggleReaction(reaction) {
  if (!isLoggedIn()) {
    ElMessage.warning('请先登录')
    return
  }
  const current = props.reactions?.userReaction || 0
  const next = current === reaction ? 0 : reaction
  const res = await reactComment(props.id, next)
  if (res.data.code === 200) {
    emit('reaction-updated', props.id, res.data.data)
  }
}

async function copyContent() {
  try {
    await navigator.clipboard.writeText(props.content)
    ElMessage.success('已复制评论')
  } catch {
    ElMessage.error('复制失败')
  }
}
</script>

<template>
  <div class="comment-item">
    <AppAvatar
      :size="40"
      :src="userAvatar"
      :name="userNickname"
      :user-id="userId"
    />
    <div class="body">
      <div class="head">
        <span class="name">{{ userNickname }}</span>
        <span class="time">{{ formatRelativeTime(createTime) }}</span>
      </div>
      <CommentContent :content="content" />
      <div class="actions">
        <button
          type="button"
          class="action-btn"
          :class="{ active: reactions?.userReaction === 1 }"
          @click="toggleReaction(1)"
        >
          <img :src="LIKE_ICON_URL" alt="赞" class="reaction-icon" />
          {{ reactions?.likeCount || 0 }}
        </button>
        <button
          type="button"
          class="action-btn"
          :class="{ active: reactions?.userReaction === -1 }"
          @click="toggleReaction(-1)"
        >
          <img :src="DISLIKE_ICON_URL" alt="踩" class="reaction-icon" />
          {{ reactions?.dislikeCount || 0 }}
        </button>
        <button type="button" class="action-btn" @click="copyContent">复制</button>
      </div>
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

.actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.action-btn {
  border: none;
  background: none;
  font-size: 13px;
  color: var(--doinb-text-secondary);
  cursor: pointer;
  padding: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.reaction-icon {
  width: 16px;
  height: 16px;
  object-fit: contain;
}

.action-btn:hover,
.action-btn.active {
  color: var(--doinb-primary);
}
</style>
