<script setup>
import { ref, onMounted, inject } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveVideo, rejectVideo, adminDeleteVideo } from '@/api/admin'
import { videoStatusLabel } from '@/api/video'
import { resolveMediaUrl } from '@/utils/media'
import AdminReportDetailDialog from '@/components/admin/AdminReportDetailDialog.vue'

const props = defineProps({
  mode: {
    type: String,
    required: true,
    validator: v => ['pending', 'report'].includes(v),
  },
  fetchList: { type: Function, required: true },
  approveMessage: { type: String, default: '已通过审核' },
  emptyText: { type: String, default: '暂无数据' },
})

const emit = defineEmits(['loaded'])
const refreshAdminCounts = inject('refreshAdminCounts', null)

const loading = ref(false)
const videos = ref([])
const page = ref(1)
const total = ref(0)
const reportDialogVisible = ref(false)
const reportVideo = ref(null)

async function loadList() {
  loading.value = true
  try {
    const res = await props.fetchList(page.value, 10)
    if (res.data.code === 200) {
      videos.value = res.data.data?.records || []
      total.value = res.data.data?.total || 0
      emit('loaded', { total: total.value })
    } else {
      ElMessage.error(res.data.message || '加载失败')
    }
  } finally {
    loading.value = false
  }
}

async function afterMutation() {
  await loadList()
  refreshAdminCounts?.()
}

async function handleApprove(row) {
  const res = await approveVideo(row.id)
  if (res.data.code === 200) {
    ElMessage.success(props.approveMessage)
    await afterMutation()
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
    await afterMutation()
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
    await afterMutation()
  } else {
    ElMessage.error(res.data.message || '删除失败')
  }
}

function onPageChange(p) {
  page.value = p
  loadList()
}

function openReports(row) {
  reportVideo.value = row
  reportDialogVisible.value = true
}

onMounted(loadList)

defineExpose({ reload: loadList })
</script>

<template>
  <el-card v-loading="loading" shadow="never">
    <el-table :data="videos" stripe>
      <el-table-column label="封面" width="100">
        <template #default="{ row }">
          <img v-if="row.coverUrl" :src="resolveMediaUrl(row.coverUrl)" alt="" class="thumb" />
          <span v-else class="no-cover">无</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
      <el-table-column prop="authorNickname" label="作者" width="120" />
      <el-table-column v-if="mode === 'report'" label="举报次数" width="100">
        <template #default="{ row }">{{ row.reportCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">{{ videoStatusLabel(row.status) }}</template>
      </el-table-column>
      <el-table-column v-if="mode === 'pending'" prop="createTime" label="提交时间" width="170" />
      <el-table-column label="操作" :width="mode === 'report' ? 360 : 280" fixed="right">
        <template #default="{ row }">
          <router-link :to="`/admin/preview/${row.id}`">
            <el-button size="small">预览</el-button>
          </router-link>
          <el-button v-if="mode === 'report'" size="small" @click="openReports(row)">举报详情</el-button>
          <el-button type="primary" size="small" @click="handleApprove(row)">通过</el-button>
          <el-button type="warning" size="small" @click="handleReject(row)">驳回</el-button>
          <el-button type="danger" size="small" plain @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !videos.length" :description="emptyText" />
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

  <AdminReportDetailDialog v-model:visible="reportDialogVisible" :video="reportVideo" />
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
