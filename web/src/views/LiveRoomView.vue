<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppAvatar from '@/components/AppAvatar.vue'
import { fetchLiveDetail } from '@/api/live'
import { fetchComments, addComment } from '@/api/comment'
import CommentItem from '@/components/CommentItem.vue'
import EmojiPicker from '@/components/EmojiPicker.vue'
import { isLoggedIn } from '@/utils/auth'
import { ElMessage } from 'element-plus'

const route = useRoute()
const roomId = computed(() => Number(route.params.id))

const loading = ref(true)
const room = ref(null)
const comments = ref([])
const commentText = ref('')
const showEmojiPicker = ref(false)
const loggedIn = ref(isLoggedIn())
const playError = ref(false)

let pollTimer = null

async function load() {
  loading.value = true
  try {
    const res = await fetchLiveDetail(roomId.value)
    if (res.data.code === 200) {
      room.value = res.data.data
      playError.value = false
    } else {
      room.value = null
    }
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  if (!room.value?.isLive) {
    comments.value = []
    return
  }
  const res = await fetchComments(roomId.value, 2)
  if (res.data.code === 200) {
    comments.value = res.data.data?.records || []
  }
}

function startPolling() {
  stopPolling()
  if (!room.value?.isLive) return
  pollTimer = setInterval(loadComments, 3000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function submitComment() {
  if (!loggedIn.value) {
    ElMessage.warning('请先登录')
    return
  }
  if (!room.value?.isLive) {
    ElMessage.warning('直播未开始')
    return
  }
  const text = commentText.value.trim()
  if (!text) return
  const res = await addComment(roomId.value, 2, text)
  if (res.data.code === 200) {
    commentText.value = ''
    showEmojiPicker.value = false
    loadComments()
  }
}

function appendEmoji(payload) {
  if (typeof payload === 'string') {
    commentText.value += payload
    return
  }
  if (payload?.type === 'unicode') {
    commentText.value += payload.code
  } else if (payload?.type === 'image') {
    commentText.value += payload.name
  }
}

function onVideoError() {
  playError.value = true
}

onMounted(async () => {
  await load()
  await loadComments()
  startPolling()
})

onUnmounted(stopPolling)

watch(roomId, async () => {
  stopPolling()
  await load()
  await loadComments()
  startPolling()
})

watch(() => room.value?.isLive, isLive => {
  if (isLive) {
    loadComments()
    startPolling()
  } else {
    comments.value = []
    stopPolling()
  }
})
</script>

<template>
  <div v-loading="loading" class="page-container">
    <template v-if="room">
      <div class="layout">
        <div class="player-area">
          <div class="player">
            <span v-if="room.isLive" class="live-badge">直播中</span>
            <template v-if="room.isLive && room.playUrl && !playError">
              <video
                class="player-video"
                :src="room.playUrl"
                controls
                autoplay
                muted
                playsinline
                @error="onVideoError"
              />
            </template>
            <div v-else class="player-placeholder">
              <template v-if="room.isLive">
                <p>直播画面加载失败</p>
                <p class="hint">当前为演示架构，尚未接入流媒体服务（SRS / 云直播）。</p>
                <p class="play-url">拉流地址：{{ room.playUrl || '未配置' }}</p>
              </template>
              <template v-else>
                <p>直播间未开播或已结束</p>
                <p class="hint">请从直播列表进入正在直播中的房间</p>
              </template>
            </div>
          </div>
          <el-card shadow="never" class="info-card">
            <div class="author-row">
              <AppAvatar :size="40" :name="room.anchorNickname" :user-id="room.anchorId" />
              <div>
                <h1>{{ room.title }}</h1>
                <p class="anchor">{{ room.anchorNickname }}</p>
              </div>
            </div>
          </el-card>
        </div>

        <el-card shadow="never" class="chat-card">
          <h2>本场弹幕</h2>
          <p v-if="room.isLive" class="chat-hint">仅显示本场直播弹幕，每日 0 点清零</p>
          <div class="comments">
            <CommentItem
              v-for="c in comments"
              :key="c.id"
              :id="c.id"
              :user-id="c.userId"
              :user-nickname="c.userNickname"
              :user-avatar="c.userAvatar"
              :content="c.content"
              :create-time="c.createTime"
              :reactions="c.reactions"
              @reaction-updated="(id, data) => { const i = comments.findIndex(x => x.id === id); if (i >= 0) comments[i].reactions = data }"
            />
            <el-empty
              v-if="!comments.length"
              :description="room.isLive ? '暂无弹幕，来发第一条吧' : '直播未开始'"
              :image-size="80"
            />
          </div>
          <div v-if="loggedIn && room.isLive" class="chat-input">
            <el-input v-model="commentText" placeholder="说点什么..." @keyup.enter="submitComment" />
            <el-button text @click="showEmojiPicker = !showEmojiPicker">表情</el-button>
            <el-button type="primary" @click="submitComment">发送</el-button>
          </div>
          <el-alert
            v-else-if="!room.isLive"
            type="info"
            :closable="false"
            show-icon
            title="直播结束后弹幕不保留，下场开播重新计数"
            class="offline-tip"
          />
          <EmojiPicker :visible="showEmojiPicker" @select="appendEmoji" @close="showEmojiPicker = false" />
        </el-card>
      </div>
    </template>
    <el-empty v-else-if="!loading" description="直播间不存在或未开播" />
  </div>
</template>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 16px;
  align-items: start;
}

@media (max-width: 960px) {
  .layout {
    grid-template-columns: 1fr;
  }
}

.player {
  position: relative;
  aspect-ratio: 16 / 9;
  background: #1a1a2e;
  border-radius: var(--doinb-radius);
  overflow: hidden;
  margin-bottom: 16px;
}

.player-video {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: contain;
}

.live-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  background: var(--doinb-danger);
  color: #fff;
  padding: 4px 10px;
  border-radius: var(--doinb-radius-sm);
  font-size: 12px;
  z-index: 1;
}

.player-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.85);
  padding: 24px;
  text-align: center;
}

.hint,
.play-url {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 8px;
}

.info-card {
  border-radius: var(--doinb-radius);
}

.author-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.author-row h1 {
  font-size: 18px;
  margin-bottom: 4px;
}

.anchor {
  font-size: 13px;
  color: var(--doinb-text-secondary);
}

.chat-card {
  border-radius: var(--doinb-radius);
  height: calc(100vh - var(--doinb-header-height) - 80px);
  display: flex;
  flex-direction: column;
}

.chat-card h2 {
  font-size: 16px;
  margin-bottom: 4px;
}

.chat-hint {
  font-size: 12px;
  color: var(--doinb-text-secondary);
  margin-bottom: 12px;
}

.comments {
  flex: 1;
  overflow-y: auto;
  min-height: 200px;
}

.chat-input {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
  align-items: center;
}

.offline-tip {
  margin-top: 12px;
}
</style>
