<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchVideoReports } from '@/api/admin'

const props = defineProps({
  visible: { type: Boolean, default: false },
  video: { type: Object, default: null },
})

const emit = defineEmits(['update:visible'])

const loading = ref(false)
const reports = ref([])

watch(
  () => [props.visible, props.video?.id],
  async ([open, id]) => {
    if (!open || !id) return
    loading.value = true
    reports.value = []
    try {
      const res = await fetchVideoReports(id)
      if (res.data.code === 200) {
        reports.value = res.data.data || []
      } else {
        ElMessage.error(res.data.message || '加载举报记录失败')
      }
    } finally {
      loading.value = false
    }
  },
)

function close() {
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="video ? `举报详情 · ${video.title}` : '举报详情'"
    width="560px"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <div v-loading="loading">
      <p v-if="video" class="meta">
        作者：{{ video.authorNickname || '—' }} · 累计举报 {{ video.reportCount ?? 0 }} 次
      </p>
      <el-table v-if="reports.length" :data="reports" stripe size="small">
        <el-table-column prop="reporterNickname" label="举报人" width="120">
          <template #default="{ row }">{{ row.reporterNickname || `用户#${row.reporterId}` }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="180">
          <template #default="{ row }">{{ row.reason || '（未填写）' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>
      <el-empty v-else-if="!loading" description="暂无举报记录" />
    </div>
    <template #footer>
      <el-button @click="close">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.meta {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--doinb-text-secondary);
}
</style>
