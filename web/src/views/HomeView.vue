<script setup>
import { ref, onMounted } from 'vue'
import VideoCard from '@/components/VideoCard.vue'
import { fetchVideoList } from '@/api/video'

const loading = ref(false)
const videos = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 12

async function loadVideos() {
  loading.value = true
  try {
    const res = await fetchVideoList(page.value, pageSize)
    if (res.data.code === 200) {
      const data = res.data.data
      videos.value = data?.records || []
      total.value = data?.total || 0
    }
  } finally {
    loading.value = false
  }
}

function onPageChange(p) {
  page.value = p
  loadVideos()
}

onMounted(loadVideos)
</script>

<template>
  <div class="page-container">
    <h1 class="page-title">推荐视频</h1>
    <p class="page-subtitle">为你精选的优质内容</p>

    <div v-loading="loading">
      <div v-if="videos.length" class="card-grid">
        <VideoCard
          v-for="v in videos"
          :key="v.id"
          :id="v.id"
          :title="v.title"
          :cover-url="v.coverUrl"
          :author-nickname="v.authorNickname"
          :create-time="v.createTime"
        />
      </div>
      <el-empty v-else-if="!loading" description="暂无视频，发布者上传后将展示在这里" />
    </div>

    <div v-if="total > pageSize" class="pagination-wrap">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.pagination-wrap {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}
</style>
