<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import VideoCard from '@/components/VideoCard.vue'
import LiveCard from '@/components/LiveCard.vue'
import AppAvatar from '@/components/AppAvatar.vue'
import { search as searchApi } from '@/api/search'
import { pageLoadErrorMessage } from '@/utils/httpError'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const activeTab = ref('video')
const loading = ref(false)
const loadError = ref('')
const results = ref({ videos: [], liveRooms: [], users: [], notices: [] })

async function doSearch() {
  const q = keyword.value.trim()
  if (!q) return
  loading.value = true
  loadError.value = ''
  try {
    const res = await searchApi(q, { skipErrorHandler: true })
    if (res.data.code === 200) {
      results.value = res.data.data || { videos: [], liveRooms: [], users: [], notices: [] }
    } else {
      loadError.value = res.data.message || '搜索失败'
    }
  } catch (err) {
    loadError.value = pageLoadErrorMessage(err)
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
  if (!q) return
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
      <h1 class="page-title">“{{ keyword }}” 的搜索结果</h1>
      <p class="page-subtitle">
        共 {{ (results.videos?.length || 0) + (results.liveRooms?.length || 0) + (results.users?.length || 0) }} 条
      </p>
      <el-alert
        v-for="(n, i) in results.notices || []"
        :key="i"
        :title="n"
        type="warning"
        show-icon
        :closable="false"
        class="degrade-alert"
      />

      <el-result
        v-if="loadError"
        icon="warning"
        title="搜索失败"
        :sub-title="loadError"
        class="state-panel"
      >
        <template #extra>
          <el-button type="primary" @click="doSearch">重试</el-button>
        </template>
      </el-result>

      <el-tabs v-else v-model="activeTab" v-loading="loading">
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
              :cover-url="r.coverUrl"
              :anchor-nickname="r.anchorNickname"
              :is-live="r.isLive"
            />
          </div>
          <el-empty v-else description="没有找到相关直播" />
        </el-tab-pane>
        <el-tab-pane :label="`用户 (${results.users?.length || 0})`" name="user">
            <div v-if="results.users?.length" class="user-list">
            <router-link
              v-for="u in results.users"
              :key="u.id"
              :to="`/user/${u.id}`"
              class="user-item"
            >
              <AppAvatar :size="48" :src="u.avatar" :name="u.nickname || u.username" :clickable="false" />
              <div>
                <div class="user-name">{{ u.nickname || u.username }}</div>
                <div class="user-id">@{{ u.username }}</div>
              </div>
            </router-link>
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

.degrade-alert {
  margin-bottom: 12px;
}

.state-panel {
  background: #fff;
  border: 1px solid var(--doinb-border-light);
  border-radius: var(--doinb-radius);
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
  color: inherit;
  text-decoration: none;
}

.user-name {
  font-weight: 500;
}

.user-id {
  font-size: 13px;
  color: var(--doinb-text-secondary);
}
</style>
