<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppAvatar from '@/components/AppAvatar.vue'
import CommentItem from '@/components/CommentItem.vue'
import EmojiPicker from '@/components/EmojiPicker.vue'
import FollowButton from '@/components/FollowButton.vue'
import { fetchVideoDetail, saveProgress, reportVideo } from '@/api/video'
import { fetchAdminVideoDetail } from '@/api/admin'
import { fetchComments, addComment } from '@/api/comment'
import { reactVideo } from '@/api/reaction'
import { fetchFollowing } from '@/api/subscription'
import { resolveMediaUrl } from '@/utils/media'
import { getUser, isLoggedIn, isAdmin } from '@/utils/auth'
import { LIKE_ICON_URL, DISLIKE_ICON_URL } from '@/constants/staticAssets'

import { parseRouteId } from '@/utils/format'

const route = useRoute()
const videoId = computed(() => parseRouteId(route.params.id))

const loading = ref(true)
const video = ref(null)
const comments = ref([])
const commentText = ref('')
const commentLoading = ref(false)
const showEmojiPicker = ref(false)
const loggedIn = ref(isLoggedIn())
const reactions = ref({ likeCount: 0, dislikeCount: 0, userReaction: 0 })
const authorFollowing = ref(false)

const isSelfAuthor = computed(() => {
  const me = getUser()
  return me && video.value && Number(me.id) === Number(video.value.authorId)
})

const videoSrc = computed(() => resolveMediaUrl(video.value?.videoUrl))
const coverSrc = computed(() => resolveMediaUrl(video.value?.coverUrl))
const shareUrl = computed(() => `${window.location.origin}/video/${videoId.value}`)

const lastSavedProgress = ref(-1)
const adminPreview = ref(false)

async function loadVideo() {
  if (videoId.value == null) {
    video.value = null
    loading.value = false
    return
  }
  loading.value = true
  adminPreview.value = false
  try {
    let res = await fetchVideoDetail(videoId.value, { skipErrorHandler: true })
    if (res.data.code !== 200 && isAdmin()) {
      res = await fetchAdminVideoDetail(videoId.value)
      if (res.data.code === 200) {
        adminPreview.value = true
      }
    }
    if (res.data.code === 200) {
      video.value = res.data.data
      reactions.value = res.data.data?.reactions || { likeCount: 0, dislikeCount: 0, userReaction: 0 }
      await syncAuthorFollow()
    }
  } finally {
    loading.value = false
  }
}

async function syncAuthorFollow() {
  if (!isLoggedIn() || isSelfAuthor.value || !video.value?.authorId) {
    authorFollowing.value = false
    return
  }
  authorFollowing.value = false
  try {
    const res = await fetchFollowing(1, 200)
    if (res.data.code === 200) {
      authorFollowing.value = (res.data.data?.records || []).some(
        u => Number(u.id) === Number(video.value.authorId)
      )
    }
  } catch {
    /* ignore */
  }
}

async function loadComments() {
  if (videoId.value == null) return
  const res = await fetchComments(videoId.value, 1)
  if (res.data.code === 200) {
    comments.value = res.data.data?.records || []
  }
}

function onCommentReactionUpdated(commentId, data) {
  const idx = comments.value.findIndex(c => c.id === commentId)
  if (idx >= 0) {
    comments.value[idx].reactions = data
  }
}

async function onTimeUpdate(e) {
  if (!loggedIn.value) return
  const progress = Math.floor(e.target.currentTime)
  if (progress <= 0 || progress % 10 !== 0) return
  if (progress === lastSavedProgress.value) return
  lastSavedProgress.value = progress
  saveProgress(videoId.value, progress).catch(() => {})
}

async function submitComment() {
  if (!loggedIn.value) {
    ElMessage.warning('请先登录')
    return
  }
  const text = commentText.value.trim()
  if (!text) return
  commentLoading.value = true
  try {
    const res = await addComment(videoId.value, 1, text)
    if (res.data.code === 200) {
      commentText.value = ''
      showEmojiPicker.value = false
      ElMessage.success('评论成功')
      loadComments()
    }
  } finally {
    commentLoading.value = false
  }
}

async function toggleVideoReaction(reaction) {
  if (!loggedIn.value) {
    ElMessage.warning('请先登录')
    return
  }
  const current = reactions.value?.userReaction || 0
  const next = current === reaction ? 0 : reaction
  const res = await reactVideo(videoId.value, next)
  if (res.data.code === 200) {
    reactions.value = res.data.data
    if (next === 1) {
      ElMessage.success('点赞成功')
    } else if (next === -1) {
      ElMessage.success('已点踩')
    } else {
      ElMessage.info('已取消')
    }
  } else {
    ElMessage.error(res.data.message || '操作失败，请重试')
  }
}

async function shareVideo() {
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    ElMessage.success('视频链接已复制，可分享给好友')
  } catch {
    ElMessage.error('复制失败')
  }
}

async function onReport() {
  if (!loggedIn.value) {
    ElMessage.warning('请先登录')
    return
  }
  if (isSelfAuthor.value) return
  try {
    const { value } = await ElMessageBox.prompt('请简要说明举报原因（可选）', '举报视频', {
      confirmButtonText: '提交',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：内容违规',
    })
    const res = await reportVideo(videoId.value, value || '')
    if (res.data.code === 200) {
      ElMessage.success(res.data.message || '举报已提交')
    } else {
      ElMessage.error(res.data.message || '举报失败')
    }
  } catch {
    /* cancelled */
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

onMounted(() => {
  loadVideo()
  loadComments()
})

watch(videoId, () => {
  lastSavedProgress.value = -1
  loadVideo()
  loadComments()
})
</script>

<template>
  <div v-loading="loading" class="page-container">
    <template v-if="video">
      <div class="layout">
        <div class="main-col">
          <p v-if="adminPreview" class="admin-preview-hint">管理员预览：该视频尚未对全站公开</p>
          <div class="player-wrap">
            <video
              v-if="videoSrc"
              class="player"
              :src="videoSrc"
              :poster="coverSrc"
              controls
              preload="metadata"
              playsinline
              @timeupdate="onTimeUpdate"
            />
            <div v-else class="player player--empty">暂无播放地址</div>
          </div>

          <el-card class="info-card" shadow="never">
            <h1 class="video-title">{{ video.title }}</h1>
            <div class="video-actions">
              <button
                type="button"
                class="action-btn"
                :class="{ active: reactions.userReaction === 1 }"
                @click="toggleVideoReaction(1)"
              >
                <img :src="LIKE_ICON_URL" alt="赞" class="reaction-icon" />
                {{ reactions.likeCount || 0 }}
              </button>
              <button
                type="button"
                class="action-btn"
                :class="{ active: reactions.userReaction === -1 }"
                @click="toggleVideoReaction(-1)"
              >
                <img :src="DISLIKE_ICON_URL" alt="踩" class="reaction-icon" />
                {{ reactions.dislikeCount || 0 }}
              </button>
              <button type="button" class="action-btn" @click="shareVideo">转发</button>
              <button
                v-if="!isSelfAuthor"
                type="button"
                class="action-btn report-btn"
                @click="onReport"
              >
                举报
              </button>
            </div>
            <div class="author-row">
              <div class="author-info">
                <AppAvatar
                  :size="48"
                  :src="video.authorAvatar"
                  :name="video.authorNickname"
                  :user-id="video.authorId"
                />
                <div>
                  <div class="author-name">{{ video.authorNickname }}</div>
                </div>
              </div>
              <FollowButton
                v-if="!isSelfAuthor && video.authorId"
                :target-id="video.authorId"
                :following="authorFollowing"
                @update:following="authorFollowing = $event"
              />
            </div>
            <p v-if="video.description" class="desc">{{ video.description }}</p>
          </el-card>

          <el-card class="comment-card" shadow="never">
            <h2 class="section-title">评论 {{ comments.length }}</h2>
            <div v-if="loggedIn" class="comment-form">
              <el-input
                v-model="commentText"
                type="textarea"
                :rows="3"
                placeholder="发表你的看法..."
                maxlength="500"
                show-word-limit
              />
              <div class="form-actions">
                <el-button text @click="showEmojiPicker = !showEmojiPicker">表情</el-button>
                <el-button type="primary" :loading="commentLoading" @click="submitComment">
                  发表评论
                </el-button>
              </div>
              <EmojiPicker :visible="showEmojiPicker" @select="appendEmoji" @close="showEmojiPicker = false" />
            </div>
            <el-alert v-else type="info" :closable="false" show-icon title="登录后可发表评论" class="login-tip" />

            <div class="comment-list">
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
                @reaction-updated="onCommentReactionUpdated"
              />
              <el-empty v-if="!comments.length" description="暂无评论，来抢沙发吧" />
            </div>
          </el-card>
        </div>
      </div>
    </template>
    <el-empty v-else-if="!loading" description="视频不存在或已下架" />
  </div>
</template>

<style scoped>
.layout {
  max-width: 900px;
}

.admin-preview-hint {
  margin: 0 0 12px;
  padding: 10px 12px;
  font-size: 13px;
  color: #e6a23c;
  background: #fdf6ec;
  border-radius: var(--doinb-radius-sm);
}

.player-wrap {
  border-radius: var(--doinb-radius);
  overflow: hidden;
  background: #000;
  margin-bottom: 16px;
}

.player {
  width: 100%;
  aspect-ratio: 16 / 9;
  display: block;
}

.player--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: #1a1a1a;
}

.info-card,
.comment-card {
  margin-bottom: 16px;
  border-radius: var(--doinb-radius);
}

.video-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 12px;
}

.video-actions {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--doinb-border-light);
}

.action-btn {
  border: none;
  background: none;
  font-size: 14px;
  color: var(--doinb-text-secondary);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.reaction-icon {
  width: 18px;
  height: 18px;
  object-fit: contain;
}

.action-btn:hover,
.action-btn.active {
  color: var(--doinb-primary);
}

.author-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-name {
  font-weight: 500;
}

.desc {
  font-size: 14px;
  color: var(--doinb-text-regular);
  line-height: 1.7;
  white-space: pre-wrap;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.comment-form {
  margin-bottom: 16px;
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.login-tip {
  margin-bottom: 16px;
}
</style>
