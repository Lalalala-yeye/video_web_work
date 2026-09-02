<script setup>
import { ref, onMounted, watch } from 'vue'
import LiveCard from '@/components/LiveCard.vue'
import PageHero from '@/components/visual/PageHero.vue'
import { fetchLiveList } from '@/api/live'
import { pageLoadErrorMessage } from '@/utils/httpError'

const loading = ref(false)
const loadError = ref('')
const rooms = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 12

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await fetchLiveList(page.value, pageSize, { skipErrorHandler: true })
    if (res.data.code === 200) {
      rooms.value = res.data.data?.records || []
      total.value = res.data.data?.total || 0
    } else {
      loadError.value = res.data.message || '直播列表加载失败'
    }
  } catch (err) {
    loadError.value = pageLoadErrorMessage(err)
  } finally {
    loading.value = false
  }
}

function onPageChange(p) {
  page.value = p
}

onMounted(load)
watch(page, load)
</script>

<template>
  <div class="page-container">
    <PageHero title="直播" subtitle="发现正在进行的精彩直播" />

    <div v-loading="loading">
      <el-result
        v-if="loadError"
        icon="warning"
        title="加载失败"
        :sub-title="loadError"
        class="state-panel"
      >
        <template #extra>
          <el-button type="primary" @click="load">重试</el-button>
        </template>
      </el-result>
      <div v-else-if="rooms.length" class="card-grid">
        <LiveCard
          v-for="room in rooms"
          :key="room.id"
          :id="room.id"
          :title="room.title"
          :cover-url="room.coverUrl"
          :anchor-nickname="room.anchorNickname"
          :is-live="room.isLive"
        />
      </div>
      <el-empty v-else-if="!loading" description="暂无直播间" />
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
