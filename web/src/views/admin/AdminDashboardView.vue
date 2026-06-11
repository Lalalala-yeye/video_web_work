<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchPendingVideos, fetchReportReviewVideos } from '@/api/admin'

const router = useRouter()
const loading = ref(true)
const pendingCount = ref(0)
const reportCount = ref(0)

async function loadStats() {
  loading.value = true
  try {
    const [pendingRes, reportRes] = await Promise.all([
      fetchPendingVideos(1, 1),
      fetchReportReviewVideos(1, 1),
    ])
    if (pendingRes.data.code === 200) {
      pendingCount.value = pendingRes.data.data?.total || 0
    }
    if (reportRes.data.code === 200) {
      reportCount.value = reportRes.data.data?.total || 0
    }
  } finally {
    loading.value = false
  }
}

function go(path) {
  router.push(path)
}

onMounted(loadStats)
</script>

<template>
  <div class="admin-panel">
    <h1 class="page-title">管理概览</h1>
    <p class="page-subtitle">视频审核与举报复审工作台</p>

    <div v-loading="loading" class="stat-grid">
      <button type="button" class="stat-card" @click="go('/admin/pending')">
        <span class="stat-value">{{ pendingCount }}</span>
        <span class="stat-label">待审视频</span>
        <span class="stat-hint">新上传与修改后待审核</span>
      </button>
      <button type="button" class="stat-card stat-card--warn" @click="go('/admin/report')">
        <span class="stat-value">{{ reportCount }}</span>
        <span class="stat-label">举报复审</span>
        <span class="stat-hint">举报达 3 次进入复审队列</span>
      </button>
    </div>

    <el-card shadow="never" class="guide-card">
      <h3>审核说明</h3>
      <ul>
        <li><strong>通过</strong>：视频公开上架，举报计数清零</li>
        <li><strong>驳回</strong>：设为仅作者可见，不删除文件</li>
        <li><strong>删除</strong>：永久删除视频与本地文件，不可恢复</li>
        <li>举报复审页可查看每条举报的举报人与原因</li>
      </ul>
    </el-card>
  </div>
</template>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

@media (max-width: 640px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
}

.stat-card {
  border: 1px solid var(--doinb-border-light);
  border-radius: var(--doinb-radius);
  background: #fff;
  padding: 20px;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  border-color: var(--doinb-primary);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.12);
}

.stat-card--warn:hover {
  border-color: #e6a23c;
  box-shadow: 0 4px 12px rgba(230, 162, 60, 0.12);
}

.stat-value {
  display: block;
  font-size: 36px;
  font-weight: 700;
  color: var(--doinb-primary);
  line-height: 1.2;
}

.stat-card--warn .stat-value {
  color: #e6a23c;
}

.stat-label {
  display: block;
  margin-top: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--doinb-text-primary);
}

.stat-hint {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: var(--doinb-text-secondary);
}

.guide-card h3 {
  margin: 0 0 12px;
  font-size: 15px;
}

.guide-card ul {
  margin: 0;
  padding-left: 20px;
  color: var(--doinb-text-regular);
  line-height: 1.8;
}
</style>
