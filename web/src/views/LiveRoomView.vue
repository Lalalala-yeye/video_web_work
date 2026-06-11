<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { attachLivePlayer } from '@/utils/livePlayer'
import AppAvatar from '@/components/AppAvatar.vue'
import { fetchLiveDetail } from '@/api/live'
import { getUser } from '@/utils/auth'
import { fetchComments, addComment } from '@/api/comment'
import CommentItem from '@/components/CommentItem.vue'
import EmojiPicker from '@/components/EmojiPicker.vue'
import { isLoggedIn } from '@/utils/auth'
import { waitStreamPlayable, parseStreamKeyFromPlayUrl } from '@/utils/srsStream'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const roomId = computed(() => Number(route.params.id))

const loading = ref(true)
const room = ref(null)
const comments = ref([])
const commentText = ref('')
const showEmojiPicker = ref(false)
const loggedIn = ref(isLoggedIn())
const playError = ref(false)
const playerLoading = ref(false)
const waitingAnchor = ref(false)
const videoRef = ref(null)

const isAnchor = computed(() => {
  const u = getUser()
  return u?.id != null && room.value?.anchorId === u.id
})

let pollTimer = null
let playerRetryTimer = null
let livePlayer = null

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

function onPlayerClick() {
  const v = videoRef.value
  if (v && v.paused) {
    v.play().catch(() => {})
  }
}

function showScreenShareNotReady() {
  ElMessageBox.alert(
    '浏览器屏幕分享功能尚未完善，请前往创作中心使用 OBS 推流。',
    '功能未完善',
    { confirmButtonText: '知道了', type: 'info' }
  )
}

/*
 * 屏幕分享（暂未启用，实现见 srsScreenPublish.js）
 * async function onAnchorScreenShare() { ... }
 * function onAnchorScreenStop() { ... }
 * async function syncLocalPreview() { ... }
 */

function destroyPlayer() {
  if (livePlayer) {
    livePlayer.destroy()
    livePlayer = null
  }
  playerLoading.value = false
}

function attachPlayer(url) {
  destroyPlayer()
  const video = videoRef.value
  if (!url || !video) return

  playerLoading.value = true
  livePlayer = attachLivePlayer(video, url, {
    onPlaying: () => {
      playError.value = false
      playerLoading.value = false
      stopPlayerRetry()
    },
    onError: () => {
      playError.value = true
      playerLoading.value = false
      startPlayerRetry()
    },
  })
}

function startPlayerRetry() {
  stopPlayerRetry()
  if (!room.value?.isLive) return
  playerRetryTimer = setInterval(async () => {
    if (!room.value?.isLive || !room.value?.playUrl) return
    const v = videoRef.value
    if (v && v.readyState >= 2 && !v.paused && !playError.value) return
    playError.value = false
    playerLoading.value = true
    await syncPlayer()
  }, 8000)
}

function stopPlayerRetry() {
  if (playerRetryTimer) {
    clearInterval(playerRetryTimer)
    playerRetryTimer = null
  }
}

async function syncPlayer() {
  if (!room.value?.isLive || !room.value?.playUrl) {
    waitingAnchor.value = false
    if (!room.value?.isLive) {
      destroyPlayer()
      stopPlayerRetry()
    }
    return
  }
  const streamKey = room.value.streamKey || parseStreamKeyFromPlayUrl(room.value.playUrl)
  if (streamKey) {
    waitingAnchor.value = true
    playerLoading.value = true
    const ready = await waitStreamPlayable(streamKey, 45000)
    waitingAnchor.value = false
    if (!ready) {
      playError.value = true
      playerLoading.value = false
      startPlayerRetry()
      return
    }
  }

  await nextTick()
  attachPlayer(room.value.playUrl)
}

onMounted(async () => {
  await load()
  await loadComments()
  await syncPlayer()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
  stopPlayerRetry()
  destroyPlayer()
})

watch(roomId, async () => {
  stopPolling()
  await load()
  await loadComments()
  await syncPlayer()
  startPolling()
})

watch(() => [room.value?.isLive, room.value?.playUrl], async () => {
  playError.value = false
  await syncPlayer()
})

watch(() => room.value?.isLive, isLive => {
  if (isLive) {
    loadComments()
    startPolling()
  } else {
    comments.value = []
    stopPolling()
    stopPlayerRetry()
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
            <video
              v-show="room.isLive && room.playUrl"
              ref="videoRef"
              class="player-video"
              controls
              autoplay
              muted
              playsinline
              @error="onVideoError"
              @click="onPlayerClick"
            />
            <div
              v-if="room.isLive && room.playUrl && (playerLoading || waitingAnchor) && !playError"
              class="player-loading"
            >
              <p>{{ waitingAnchor ? '等待主播推流进入 SRS…' : '正在连接直播流…' }}</p>
              <p class="hint">请保持本页面打开，稍候即可看到画面</p>
            </div>
            <div
              v-if="!room.isLive || !room.playUrl || playError"
              class="player-placeholder"
            >
              <template v-if="room.isLive">
                <p>等待直播画面</p>
                <p class="hint">主播请用 OBS 推流；若画面黑屏请点一下视频尝试播放。每 8 秒自动重试。</p>
                <p v-if="isAnchor" class="hint">你是主播，请前往创作中心完成 OBS 开播设置。</p>
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
              <div class="author-meta">
                <h1>{{ room.title }}</h1>
                <p class="anchor">{{ room.anchorNickname }}</p>
              </div>
            </div>
            <div v-if="isAnchor" class="anchor-actions">
              <el-button type="primary" size="small" @click="showScreenShareNotReady">
                屏幕分享（未完善）
              </el-button>
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

.player-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.85);
  background: rgba(0, 0, 0, 0.45);
  z-index: 2;
  text-align: center;
  padding: 16px;
}

.player-loading .hint {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
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

.author-meta h1 {
  font-size: 18px;
  margin-bottom: 4px;
}

.anchor-actions {
  margin-top: 12px;
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
