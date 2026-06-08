<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppAvatar from '@/components/AppAvatar.vue'
import CommentItem from '@/components/CommentItem.vue'
import { fetchVideoDetail, saveProgress } from '@/api/video'
import { fetchComments, addComment } from '@/api/comment'
import { followUser } from '@/api/subscription'
import { resolveMediaUrl } from '@/utils/media'
import { isLoggedIn } from '@/utils/auth'

const route = useRoute()
const videoId = computed(() => Number(route.params.id))

const loading = ref(true)
const video = ref(null)
const comments = ref([])
const commentText = ref('')
const commentLoading = ref(false)
const loggedIn = ref(isLoggedIn())

const videoSrc = computed(() => resolveMediaUrl(video.value?.videoUrl))
const coverSrc = computed(() => resolveMediaUrl(video.value?.coverUrl))

async function loadVideo() {
  loading.value = true
  try {
    const res = await fetchVideoDetail(videoId.value)
    if (res.data.code === 200) {
      video.value = res.data.data
    }
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  const res = await fetchComments(videoId.value, 1)
  if (res.data.code === 200) {
    comments.value = res.data.data?.records || []
  }
}

async function onTimeUpdate(e) {
  if (!loggedIn.value) return
  const progress = Math.floor(e.target.currentTime)
  if (progress > 0 && progress % 10 === 0) {
    saveProgress(videoId.value, progress).catch(() => {})
  }
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
      ElMessage.success('评论成功')
      loadComments()
    }
  } finally {
    commentLoading.value = false
  }
}

async function onFollow() {
  if (!loggedIn.value) {
    ElMessage.warning('请先登录')
    return
  }
  const res = await followUser(video.value.authorId)
  if (res.data.code === 200) {
    ElMessage.success(res.data.message || '关注成功')
  }
}

onMounted(() => {
  loadVideo()
  loadComments()
})

watch(videoId, () => {
  loadVideo()
  loadComments()
})
</script>

<template>
  <div v-loading="loading" class="page-container">
    <template v-if="video">
      <div class="layout">
        <div class="main-col">
          <div class="player-wrap">
            <video
              v-if="videoSrc"
              class="player"
              :src="videoSrc"
              :poster="coverSrc"
              controls
              @timeupdate="onTimeUpdate"
            />
            <div v-else class="player player--empty">暂无播放地址</div>
          </div>

          <el-card class="info-card" shadow="never">
            <h1 class="video-title">{{ video.title }}</h1>
            <div class="author-row">
              <div class="author-info">
                <AppAvatar :size="48" :name="video.authorNickname" />
                <div>
                  <div class="author-name">{{ video.authorNickname }}</div>
                </div>
              </div>
              <el-button type="primary" @click="onFollow">订阅</el-button>
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
              <el-button type="primary" :loading="commentLoading" class="submit-btn" @click="submitComment">
                发表评论
              </el-button>
            </div>
            <el-alert v-else type="info" :closable="false" show-icon title="登录后可发表评论" class="login-tip" />

            <div class="comment-list">
              <CommentItem
                v-for="c in comments"
                :key="c.id"
                :user-nickname="c.userNickname"
                :user-avatar="c.userAvatar"
                :content="c.content"
                :create-time="c.createTime"
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
  margin-bottom: 16px;
}

.author-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--doinb-border-light);
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

.submit-btn {
  margin-top: 12px;
}

.login-tip {
  margin-bottom: 16px;
}
</style>
