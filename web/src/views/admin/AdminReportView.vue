<script setup>
import { fetchReportReviewVideos } from '@/api/admin'
import AdminVideoReviewTable from '@/components/admin/AdminVideoReviewTable.vue'
</script>

<template>
  <div class="admin-panel">
    <h1 class="page-title">举报复审</h1>
    <p class="page-subtitle">已发布视频被举报达到 3 次后进入复审队列</p>
    <AdminVideoReviewTable
      mode="report"
      :fetch-list="fetchReportReviewVideos"
      approve-message="复审通过，已恢复公开"
      empty-text="暂无举报复审视频"
    />
  </div>
</template>
