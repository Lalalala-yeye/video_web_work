<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import AppAvatar from '@/components/AppAvatar.vue'
import VideoCard from '@/components/VideoCard.vue'
import { getUser, isLoggedIn } from '@/utils/auth'
import { fetchPersonalInfo } from '@/api/user'
import { fetchHistoryList, fetchMyVideos } from '@/api/video'
import { useRouter } from 'vue-router'
import { postParams } from '@/network/request'

const router = useRouter()
const user = ref(getUser())
const activeTab = ref('history')
const history = ref([])
const myVideos = ref([])
const loading = ref(false)

const editForm = reactive({
  nickname: '',
  avatar: ''
})

const canUpload = computed(() => user.value && (user.value.role === 1 || user.value.role === 2))

async function loadProfile() {
  if (!isLoggedIn()) {
    router.push('/login')
    return
  }
  const res = await fetchPersonalInfo()
  if (res.data.code === 200) {
    user.value = res.data.data
    editForm.nickname = user.value.nickname || ''
    editForm.avatar = user.value.avatar || ''
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
  if (!canUpload.value) return
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

async function saveProfile() {
  const res = await postParams('/user/info/update', {
    nickname: editForm.nickname,
    avatar: editForm.avatar || undefined
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
        <AppAvatar :size="80" :src="user?.avatar" :name="user?.nickname || user?.username" />
        <div class="profile-info">
          <h1>{{ user?.nickname || user?.username }}</h1>
          <p>@{{ user?.username }}</p>
          <el-tag v-if="user?.role === 1" type="success" size="small">发布者</el-tag>
          <el-tag v-else-if="user?.role === 2" type="danger" size="small">管理员</el-tag>
        </div>
        <router-link v-if="canUpload" to="/upload">
          <el-button type="primary">上传视频</el-button>
        </router-link>
      </div>

      <el-divider />

      <el-form :model="editForm" label-width="80px" class="edit-form">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" />
        </el-form-item>
        <el-form-item label="头像 URL">
          <el-input v-model="editForm.avatar" placeholder="头像图片地址" />
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
              <span>{{ h.title || `视频 #${h.videoId}` }}</span>
              <span class="progress">进度 {{ h.progress }} 秒</span>
            </router-link>
          </div>
          <el-empty v-else description="暂无播放历史" />
        </div>
      </el-tab-pane>
      <el-tab-pane v-if="canUpload" label="我的视频" name="videos">
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

.edit-form {
  max-width: 480px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-item {
  display: flex;
  justify-content: space-between;
  padding: 14px 16px;
  background: #fff;
  border-radius: var(--doinb-radius-sm);
  border: 1px solid var(--doinb-border-light);
  transition: border-color 0.2s;
}

.history-item:hover {
  border-color: var(--doinb-primary);
}

.progress {
  font-size: 13px;
  color: var(--doinb-text-secondary);
}
</style>
