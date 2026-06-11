<script setup>
import { ref, onMounted, watch } from 'vue'
import LiveCard from '@/components/LiveCard.vue'
import PageHero from '@/components/visual/PageHero.vue'
import { fetchLiveList } from '@/api/live'

const loading = ref(false)
const rooms = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 12

async function load() {
  loading.value = true
  try {
    const res = await fetchLiveList(page.value, pageSize)
    if (res.data.code === 200) {
      rooms.value = res.data.data?.records || []
      total.value = res.data.data?.total || 0
    }
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
    <PageHero title="直播" subtitle="发现正在进行的精彩直播，角标使用当前主题中间色点缀" />

    <div v-loading="loading">
      <div v-if="rooms.length" class="card-grid">
        <LiveCard
          v-for="room in rooms"
          :key="room.id"
          :id="room.id"
          :title="room.title"
          :anchor-nickname="room.anchorNickname"
          :is-live="room.isLive"
        />
      </div>
      <el-empty v-else-if="!loading" description="暂无直播间" />
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
