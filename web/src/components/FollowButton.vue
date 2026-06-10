<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { followUser, unfollowUser } from '@/api/subscription'
import { isLoggedIn } from '@/utils/auth'

const props = defineProps({
  targetId: { type: Number, required: true },
  following: { type: Boolean, default: false },
})

const emit = defineEmits(['update:following'])

const router = useRouter()
const loading = ref(false)
const isFollowing = ref(props.following)

watch(
  () => props.following,
  v => {
    isFollowing.value = v
  },
  { immediate: true }
)

async function toggleFollow() {
  if (!isLoggedIn()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  loading.value = true
  try {
    if (isFollowing.value) {
      const res = await unfollowUser(props.targetId)
      if (res.data.code === 200) {
        isFollowing.value = false
        emit('update:following', false)
        if (res.data.message !== '未关注') {
          ElMessage.success('已取消关注')
        }
      }
    } else {
      const res = await followUser(props.targetId)
      if (res.data.code === 200) {
        isFollowing.value = true
        emit('update:following', true)
        if (res.data.message !== '已关注') {
          ElMessage.success('关注成功')
        }
      }
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <button
    type="button"
    class="follow-btn"
    :class="{ 'follow-btn--active': isFollowing }"
    :disabled="loading"
    @click="toggleFollow"
  >
    {{ isFollowing ? '已关注' : '关注' }}
  </button>
</template>

<style scoped>
.follow-btn {
  min-width: 88px;
  height: 36px;
  padding: 0 16px;
  border: none;
  border-radius: var(--doinb-radius-sm);
  background: var(--doinb-primary);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.follow-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.follow-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.follow-btn--active {
  background: #fff;
  color: var(--doinb-primary);
  border: 1px solid var(--doinb-primary);
}
</style>
