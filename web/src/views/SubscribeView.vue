<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import VideoCard from '@/components/VideoCard.vue'
import LiveCard from '@/components/LiveCard.vue'
import { fetchFeed } from '@/api/subscription'
import { isLoggedIn } from '@/utils/auth'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false)
const items = ref([])

async function load() {
  if (!isLoggedIn()) {
    ElMessage.warning('请先登录查看关注动态')
    router.push('/login')
    return
  }
  loading.value = true
  try {
    const res = await fetchFeed(1, 20)
    if (res.data.code === 200) {
      items.value = res.data.data?.records || []
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <h1 class="page-title">关注动态</h1>
    <p class="page-subtitle">你关注的 UP 主最新视频与直播</p>

    <div v-loading="loading">
      <div v-if="items.length" class="feed-list">
        <template v-for="(item, idx) in items" :key="idx">
          <VideoCard
            v-if="item.type === 'video' && item.video"
            :id="item.video.id"
            :title="item.video.title"
            :cover-url="item.video.coverUrl"
            :author-nickname="item.video.authorNickname"
            :create-time="item.video.createTime"
          />
          <LiveCard
            v-else-if="item.type === 'live' && item.liveRoom"
            :id="item.liveRoom.id"
            :title="item.liveRoom.title"
            :anchor-nickname="item.liveRoom.anchorNickname"
            :is-live="item.liveRoom.isLive"
          />
        </template>
      </div>
      <el-empty v-else-if="!loading" description="还没有关注任何人，去首页发现 UP 主吧">
        <router-link to="/">
          <el-button type="primary">去首页</el-button>
        </router-link>
      </el-empty>
    </div>
  </div>
</template>

<style scoped>
.feed-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}
</style>
