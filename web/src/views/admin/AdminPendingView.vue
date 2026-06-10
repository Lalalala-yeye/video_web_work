<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchPendingVideos, approveVideo, rejectVideo, adminDeleteVideo } from '@/api/admin'
import { videoStatusLabel } from '@/api/video'
import { resolveMediaUrl } from '@/utils/media'

const loading = ref(false)
const videos = ref([])
const page = ref(1)
const total = ref(0)

async function loadList() {
  loading.value = true
  try {
    const res = await fetchPendingVideos(page.value, 10)
    if (res.data.code === 200) {
      videos.value = res.data.data?.records || []
      total.value = res.data.data?.total || 0
    }
  } finally {
    loading.value = false
  }
}

async function handleApprove(row) {
  const res = await approveVideo(row.id)
  if (res.data.code === 200) {
    ElMessage.success('已通过审核')
    await loadList()
  } else {
    ElMessage.error(res.data.message || '操作失败')
  }
}

async function handleReject(row) {
  try {
    await ElMessageBox.confirm(`确定驳回「${row.title}」？将设为仅作者可见`, '驳回确认', { type: 'warning' })
  } catch {
    return
  }
  const res = await rejectVideo(row.id)
  if (res.data.code === 200) {
    ElMessage.success('已驳回')
    await loadList()
  } else {
    ElMessage.error(res.data.message || '操作失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.title}」？不可恢复`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  const res = await adminDeleteVideo(row.id)
  if (res.data.code === 200) {
    ElMessage.success('已删除')
    await loadList()
  } else {
    ElMessage.error(res.data.message || '删除失败')
  }
}

function onPageChange(p) {
  page.value = p
  loadList()
}

onMounted(loadList)
</script>

<template>
  <div class="admin-panel">
    <h1 class="page-title">待审视频</h1>
    <p class="page-subtitle">新上传与修改后待审核的稿件</p>

    <el-card v-loading="loading" shadow="never">
      <el-table :data="videos" stripe>
        <el-table-column label="封面" width="100">
          <template #default="{ row }">
            <img v-if="row.coverUrl" :src="resolveMediaUrl(row.coverUrl)" alt="" class="thumb" />
            <span v-else class="no-cover">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column prop="authorNickname" label="作者" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">{{ videoStatusLabel(row.status) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <router-link :to="`/video/${row.id}`" target="_blank">
              <el-button size="small">预览</el-button>
            </router-link>
            <el-button type="primary" size="small" @click="handleApprove(row)">通过</el-button>
            <el-button type="warning" size="small" @click="handleReject(row)">驳回</el-button>
            <el-button type="danger" size="small" plain @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !videos.length" description="暂无待审视频" />
      <div v-if="total > 10" class="pager">
        <el-pagination
          layout="prev, pager, next"
          :total="total"
          :page-size="10"
          :current-page="page"
          @current-change="onPageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.thumb {
  width: 72px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
}

.no-cover {
  color: var(--doinb-text-secondary);
  font-size: 12px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>
