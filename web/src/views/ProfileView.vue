<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppAvatar from '@/components/AppAvatar.vue'
import VideoCard from '@/components/VideoCard.vue'
import { getUser, isLoggedIn, setUser } from '@/utils/auth'
import { fetchPersonalInfo, uploadAvatar } from '@/api/user'
import { fetchHistoryList, fetchMyVideos } from '@/api/video'
import { resolveMediaUrl } from '@/utils/media'
import { useRouter } from 'vue-router'
import { postParams } from '@/network/request'

const router = useRouter()
const user = ref(getUser())
const activeTab = ref('history')
const history = ref([])
const myVideos = ref([])
const loading = ref(false)
const avatarInputRef = ref(null)

const editForm = reactive({
  nickname: '',
  bio: ''
})

const loggedIn = computed(() => isLoggedIn())
const avatarSrc = computed(() => user.value?.avatar || '')

async function loadProfile() {
  if (!isLoggedIn()) {
    router.push('/login')
    return
  }
  const res = await fetchPersonalInfo()
  if (res.data.code === 200) {
    user.value = res.data.data
    setUser(res.data.data)
    editForm.nickname = user.value.nickname || ''
    editForm.bio = user.value.bio || ''
  }
}

async function loadHistory() {
  loading.value = true
  try {
    const res = await fetchHistoryList(1, 12)
    if (res.data.code === 200) {
      history.value = res.data.data?.records || []
    }
  } finally {
    loading.value = false
  }
}

async function loadMyVideos() {
  if (!loggedIn.value) return
  loading.value = true
  try {
    const res = await fetchMyVideos(1, 12)
    if (res.data.code === 200) {
      myVideos.value = res.data.data?.records || []
    }
  } finally {
    loading.value = false
  }
}

async function onAvatarClick() {
  try {
    await ElMessageBox.confirm('更新头像？', '提示', {
      confirmButtonText: '选择图片',
      cancelButtonText: '取消',
      type: 'info'
    })
    avatarInputRef.value?.click()
  } catch {
    /* cancelled */
  }
}

async function onAvatarSelected(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  const res = await uploadAvatar(file)
  if (res.data.code === 200) {
    ElMessage.success('头像已更新')
    user.value = res.data.data
    setUser(res.data.data)
  }
}

async function saveProfile() {
  const res = await postParams('/user/info/update', {
    nickname: editForm.nickname,
    bio: editForm.bio || undefined
  })
  if (res.data.code === 200) {
    ElMessage.success('资料已更新')
    loadProfile()
  }
}

onMounted(async () => {
  await loadProfile()
  loadHistory()
  loadMyVideos()
})
</script>

<template>
  <div class="page-container">
    <el-card shadow="never" class="profile-header">
      <div class="profile-top">
        <button type="button" class="avatar-trigger" title="点击更新头像" @click="onAvatarClick">
          <AppAvatar
            :size="80"
            :src="avatarSrc"
            :name="user?.nickname || user?.username"
            :clickable="false"
          />
          <span class="avatar-hint">点击更新头像</span>
        </button>
        <input
          ref="avatarInputRef"
          type="file"
          accept="image/jpeg,image/png,image/webp,image/gif"
          hidden
          @change="onAvatarSelected"
        />
        <div class="profile-info">
          <h1>{{ user?.nickname || user?.username }}</h1>
          <p>@{{ user?.username }}</p>
          <p v-if="user?.bio" class="bio-preview">{{ user.bio }}</p>
          <el-tag v-if="user?.role === 2" type="danger" size="small">管理员</el-tag>
          <el-tag v-else type="success" size="small">发布者</el-tag>
        </div>
        <div class="actions">
          <router-link v-if="loggedIn" to="/studio">
            <el-button type="primary">发布</el-button>
          </router-link>
          <router-link v-if="user?.id" :to="`/user/${user.id}`">
            <el-button>预览我的主页</el-button>
          </router-link>
        </div>
      </div>

      <el-divider />

      <el-form :model="editForm" label-width="80px" class="edit-form">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input
            v-model="editForm.bio"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="写一段简介，将展示在你的个人主页"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveProfile">保存资料</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-tabs v-model="activeTab" class="tabs">
      <el-tab-pane label="播放历史" name="history">
        <div v-loading="loading">
          <div v-if="history.length" class="history-list">
            <router-link
              v-for="h in history"
              :key="h.videoId"
              :to="`/video/${h.videoId}`"
              class="history-item"
            >
              <div class="history-cover-wrap">
                <img
                  v-if="h.coverUrl"
                  :src="resolveMediaUrl(h.coverUrl)"
                  :alt="h.title"
                  class="history-cover"
                />
                <div v-else class="history-cover history-cover--empty">无封面</div>
              </div>
              <div class="history-meta">
                <span class="history-title">{{ h.title || `视频 #${h.videoId}` }}</span>
                <span class="progress">看到 {{ h.progress }} 秒</span>
              </div>
            </router-link>
          </div>
          <el-empty v-else description="暂无播放历史" />
        </div>
      </el-tab-pane>
      <el-tab-pane label="我的视频" name="videos">
        <div v-loading="loading">
          <div v-if="myVideos.length" class="card-grid">
            <VideoCard
              v-for="v in myVideos"
              :key="v.id"
              :id="v.id"
              :title="v.title"
              :cover-url="v.coverUrl"
              :author-nickname="v.authorNickname"
              :create-time="v.createTime"
            />
          </div>
          <el-empty v-else description="还没有上传视频" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.profile-header {
  border-radius: var(--doinb-radius);
  margin-bottom: 24px;
}

.profile-top {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.avatar-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  border: none;
  background: none;
  cursor: pointer;
  padding: 0;
}

.avatar-hint {
  font-size: 12px;
  color: var(--doinb-text-secondary);
}

.profile-info {
  flex: 1;
}

.profile-info h1 {
  font-size: 22px;
  margin-bottom: 4px;
}

.profile-info p {
  color: var(--doinb-text-secondary);
  margin-bottom: 8px;
}

.bio-preview {
  color: var(--doinb-text-regular) !important;
  line-height: 1.6;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.edit-form {
  max-width: 560px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  background: #fff;
  border-radius: var(--doinb-radius-sm);
  border: 1px solid var(--doinb-border-light);
  transition: border-color 0.2s;
}

.history-cover-wrap {
  flex-shrink: 0;
}

.history-cover {
  width: 120px;
  height: 68px;
  object-fit: cover;
  border-radius: var(--doinb-radius-sm);
  display: block;
  background: #000;
}

.history-cover--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #fff;
  background: #333;
}

.history-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.history-title {
  font-size: 15px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item:hover {
  border-color: var(--doinb-primary);
}

.progress {
  font-size: 13px;
  color: var(--doinb-text-secondary);
}
</style>
