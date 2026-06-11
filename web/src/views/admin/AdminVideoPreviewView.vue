<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchAdminVideoDetail } from '@/api/admin'
import { videoStatusLabel } from '@/api/video'
import { resolveMediaUrl } from '@/utils/media'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const video = ref(null)

const videoId = computed(() => Number(route.params.id))
const videoSrc = computed(() => resolveMediaUrl(video.value?.videoUrl))
const coverSrc = computed(() => resolveMediaUrl(video.value?.coverUrl))

async function loadVideo() {
  loading.value = true
  video.value = null
  try {
    const res = await fetchAdminVideoDetail(videoId.value)
    if (res.data.code === 200) {
      video.value = res.data.data
    } else {
      ElMessage.error(res.data.message || '无法加载视频')
    }
  } finally {
    loading.value = false
  }
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/admin/pending')
  }
}

onMounted(loadVideo)

watch(videoId, loadVideo)
</script>

<template>
  <div v-loading="loading" class="admin-panel preview-page">
    <div class="preview-header">
      <el-button @click="goBack">返回审核列表</el-button>
      <el-tag v-if="video" type="warning" size="small">{{ videoStatusLabel(video.status) }}</el-tag>
    </div>

    <template v-if="video">
      <div class="player-wrap">
        <video
          v-if="videoSrc"
          class="player"
          :src="videoSrc"
          :poster="coverSrc"
          controls
          preload="metadata"
          playsinline
        />
        <div v-else class="player player--empty">暂无播放地址</div>
      </div>

      <el-card shadow="never" class="info-card">
        <h1 class="video-title">{{ video.title }}</h1>
        <p class="meta">
          作者：{{ video.authorNickname || '—' }} · 提交于 {{ video.createTime || '—' }}
        </p>
        <p v-if="video.description" class="desc">{{ video.description }}</p>
        <p v-else class="desc desc--empty">暂无简介</p>
        <p class="hint">仅管理员可见此预览，全站用户尚无法观看未过审内容。</p>
      </el-card>
    </template>

    <el-empty v-else-if="!loading" description="视频不存在或无权预览" />
  </div>
</template>

<style scoped>
.preview-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.player-wrap {
  background: #000;
  border-radius: var(--doinb-radius);
  overflow: hidden;
  margin-bottom: 16px;
}

.player {
  display: block;
  width: 100%;
  max-height: 70vh;
  background: #000;
}

.player--empty {
  color: #999;
  text-align: center;
  padding: 80px 16px;
}

.video-title {
  margin: 0 0 8px;
  font-size: 20px;
}

.meta {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--doinb-text-secondary);
}

.desc {
  margin: 0;
  line-height: 1.7;
  white-space: pre-wrap;
}

.desc--empty {
  color: var(--doinb-text-secondary);
}

.hint {
  margin: 16px 0 0;
  padding: 10px 12px;
  font-size: 12px;
  color: #e6a23c;
  background: #fdf6ec;
  border-radius: var(--doinb-radius-sm);
}
</style>
