<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import VideoCard from '@/components/VideoCard.vue'
import LiveCard from '@/components/LiveCard.vue'
import AppAvatar from '@/components/AppAvatar.vue'
import { search as searchApi } from '@/api/search'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const activeTab = ref('video')
const loading = ref(false)
const results = ref({ videos: [], liveRooms: [], users: [] })

async function doSearch() {
  const q = keyword.value.trim()
  if (!q) return
  loading.value = true
  try {
    const res = await searchApi(q)
    if (res.data.code === 200) {
      results.value = res.data.data || { videos: [], liveRooms: [], users: [] }
    }
  } finally {
    loading.value = false
  }
}

function syncFromRoute() {
  keyword.value = route.query.keyword || route.query.q || ''
  if (keyword.value) doSearch()
}

onMounted(syncFromRoute)
watch(() => route.query.keyword, syncFromRoute)

function onSearchSubmit() {
  const q = keyword.value.trim()
  router.push({ path: '/search', query: { keyword: q } })
}
</script>

<template>
  <div class="page-container">
    <el-input
      v-model="keyword"
      placeholder="搜索视频、直播、用户"
      size="large"
      clearable
      class="search-bar"
      @keyup.enter="onSearchSubmit"
    >
      <template #append>
        <el-button @click="onSearchSubmit">搜索</el-button>
      </template>
    </el-input>

    <template v-if="keyword">
      <h1 class="page-title">"{{ keyword }}" 的搜索结果</h1>
      <p class="page-subtitle">
        共 {{ (results.videos?.length || 0) + (results.liveRooms?.length || 0) + (results.users?.length || 0) }} 条
      </p>

      <el-tabs v-model="activeTab" v-loading="loading">
        <el-tab-pane :label="`视频 (${results.videos?.length || 0})`" name="video">
          <div v-if="results.videos?.length" class="card-grid">
            <VideoCard
              v-for="v in results.videos"
              :key="v.id"
              :id="v.id"
              :title="v.title"
              :cover-url="v.coverUrl"
              :author-nickname="v.authorNickname"
              :create-time="v.createTime"
            />
          </div>
          <el-empty v-else description="没有找到相关视频" />
        </el-tab-pane>
        <el-tab-pane :label="`直播 (${results.liveRooms?.length || 0})`" name="live">
          <div v-if="results.liveRooms?.length" class="card-grid">
            <LiveCard
              v-for="r in results.liveRooms"
              :key="r.id"
              :id="r.id"
              :title="r.title"
              :anchor-nickname="r.anchorNickname"
              :is-live="r.isLive"
            />
          </div>
          <el-empty v-else description="没有找到相关直播" />
        </el-tab-pane>
        <el-tab-pane :label="`用户 (${results.users?.length || 0})`" name="user">
          <div v-if="results.users?.length" class="user-list">
            <div v-for="u in results.users" :key="u.id" class="user-item">
              <AppAvatar :size="48" :src="u.avatar" :name="u.nickname || u.username" />
              <div>
                <div class="user-name">{{ u.nickname || u.username }}</div>
                <div class="user-id">@{{ u.username }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else description="没有找到相关用户" />
        </el-tab-pane>
      </el-tabs>
    </template>
    <el-empty v-else description="输入关键词开始搜索" />
  </div>
</template>

<style scoped>
.search-bar {
  max-width: 640px;
  margin-bottom: 24px;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #fff;
  border-radius: var(--doinb-radius);
  border: 1px solid var(--doinb-border-light);
}

.user-name {
  font-weight: 500;
}

.user-id {
  font-size: 13px;
  color: var(--doinb-text-secondary);
}
</style>
