<script setup>
import { fetchPendingVideos } from '@/api/admin'
import AdminVideoReviewTable from '@/components/admin/AdminVideoReviewTable.vue'
</script>

<template>
  <div class="admin-panel">
    <h1 class="page-title">待审视频</h1>
    <p class="page-subtitle">新上传与修改后待审核的稿件</p>
    <AdminVideoReviewTable
      mode="pending"
      :fetch-list="fetchPendingVideos"
      empty-text="暂无待审视频"
    />
  </div>
</template>
