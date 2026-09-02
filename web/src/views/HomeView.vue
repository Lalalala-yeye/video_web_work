<script setup>
import { ref, onMounted } from 'vue'
import VideoCard from '@/components/VideoCard.vue'
import PageHero from '@/components/visual/PageHero.vue'
import { fetchVideoList } from '@/api/video'
import { pageLoadErrorMessage } from '@/utils/httpError'

const loading = ref(false)
const loadError = ref('')
const videos = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 12

async function loadVideos() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await fetchVideoList(page.value, pageSize, { skipErrorHandler: true })
    if (res.data.code === 200) {
      const data = res.data.data
      videos.value = data?.records || []
      total.value = data?.total || 0
    } else {
      loadError.value = res.data.message || '视频列表加载失败'
    }
  } catch (err) {
    loadError.value = pageLoadErrorMessage(err)
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
    <PageHero title="推荐视频" subtitle="为你精选的优质内容" />

    <div v-loading="loading">
      <el-result
        v-if="loadError"
        icon="warning"
        title="加载失败"
        :sub-title="loadError"
        class="state-panel"
      >
        <template #extra>
          <el-button type="primary" @click="loadVideos">重试</el-button>
        </template>
      </el-result>
      <div v-else-if="videos.length" class="card-grid">
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

    <div v-if="!loadError && total > pageSize" class="pagination-wrap">
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
.state-panel {
  background: #fff;
  border: 1px solid var(--doinb-border-light);
  border-radius: var(--doinb-radius);
}

.pagination-wrap {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}
</style>
