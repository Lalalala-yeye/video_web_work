<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppAvatar from '@/components/AppAvatar.vue'
import VideoCard from '@/components/VideoCard.vue'
import FollowButton from '@/components/FollowButton.vue'
import { fetchUserShowcase } from '@/api/user'
import { openMessageRoom } from '@/api/message'
import { fetchFollowing } from '@/api/subscription'
import { getUser, isLoggedIn } from '@/utils/auth'
import { SEND_ICON_URL } from '@/constants/staticAssets'

import { parseRouteId } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userId = computed(() => parseRouteId(route.params.id))

const loading = ref(true)
const messaging = ref(false)
const showcase = ref(null)
const following = ref(false)
const useFallbackMsgIcon = ref(false)

const isSelf = computed(() => {
  const me = getUser()
  return me && Number(me.id) === userId.value
})

async function syncFollowState(showcaseData) {
  if (!isLoggedIn() || isSelf.value) {
    following.value = false
    return
  }

  following.value = !!showcaseData?.following

  // 展示页接口是公开的，若未识别登录态则用「我的关注列表」兜底
  if (!following.value) {
    try {
      const res = await fetchFollowing(1, 200)
      if (res.data.code === 200) {
        const followed = (res.data.data?.records || []).some(
          u => Number(u.id) === userId.value
        )
        if (followed) following.value = true
      }
    } catch {
      /* ignore */
    }
  }
}

async function load() {
  if (userId.value == null) {
    showcase.value = null
    loading.value = false
    ElMessage.warning('无效的用户链接')
    return
  }
  loading.value = true
  try {
    const res = await fetchUserShowcase(userId.value)
    if (res.data.code === 200) {
      showcase.value = res.data.data
      await syncFollowState(res.data.data)
    }
  } finally {
    loading.value = false
  }
}

async function onSendMessage() {
  if (!isLoggedIn()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (isSelf.value) return
  messaging.value = true
  try {
    const res = await openMessageRoom(userId.value)
    if (res.data.code === 200) {
      const roomId = res.data.data?.roomId
      if (roomId) router.push(`/messages/${roomId}`)
    }
  } finally {
    messaging.value = false
  }
}

onMounted(load)
watch(userId, load)
</script>

<template>
  <div v-loading="loading" class="page-container">
    <template v-if="showcase?.profile">
      <el-card shadow="never" class="header-card">
        <div class="profile-top">
          <AppAvatar
            :size="88"
            :src="showcase.profile.avatar"
            :name="showcase.profile.nickname"
            :clickable="false"
          />
          <div class="info">
            <div class="title-row">
              <h1>{{ showcase.profile.nickname || '用户' }}</h1>
              <div v-if="!isSelf" class="header-actions">
                <FollowButton
                  v-if="userId != null"
                  :target-id="userId"
                  :following="following"
                  @update:following="following = $event"
                />
                <button
                  type="button"
                  class="msg-btn"
                  :disabled="messaging"
                  title="发私信"
                  @click="onSendMessage"
                >
                <img
                  v-if="!useFallbackMsgIcon"
                  :src="SEND_ICON_URL"
                  alt="发私信"
                  class="msg-icon"
                  @error="useFallbackMsgIcon = true"
                />
                <span v-else class="msg-fallback">✉</span>
                </button>
              </div>
            </div>
            <p class="bio">{{ showcase.profile.bio || '这个人很懒，还没有写简介' }}</p>
          </div>
        </div>
      </el-card>

      <h2 class="section-title">TA 的作品</h2>
      <div v-if="showcase.videos?.length" class="card-grid">
        <VideoCard
          v-for="v in showcase.videos"
          :key="v.id"
          :id="v.id"
          :title="v.title"
          :cover-url="v.coverUrl"
          :author-nickname="v.authorNickname"
          :create-time="v.createTime"
        />
      </div>
      <el-empty v-else description="暂无公开作品" />
    </template>
    <el-empty v-else-if="!loading" description="用户不存在" />
  </div>
</template>

<style scoped>
.header-card {
  border-radius: var(--doinb-radius);
  margin-bottom: 24px;
}

.profile-top {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.info h1 {
  font-size: 22px;
  margin-bottom: 0;
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.msg-btn {
  border: none;
  background: var(--doinb-bg-page);
  border-radius: 50%;
  width: 36px;
  height: 36px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.msg-btn:hover {
  background: #e8f3ff;
}

.msg-icon {
  width: 20px;
  height: 20px;
}

.msg-fallback {
  font-size: 18px;
}

.bio {
  color: var(--doinb-text-regular);
  line-height: 1.7;
  white-space: pre-wrap;
}

.section-title {
  font-size: 18px;
  margin-bottom: 16px;
}
</style>
