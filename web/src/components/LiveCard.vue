<script setup>
import AppAvatar from './AppAvatar.vue'

defineProps({
  id: { type: [Number, String], required: true },
  title: { type: String, required: true },
  anchorNickname: { type: String, default: '未知主播' },
  anchorAvatar: { type: String, default: '' },
  isLive: { type: Boolean, default: false }
})
</script>

<template>
  <router-link :to="`/live/${id}`" class="live-card">
    <div class="cover-wrap">
      <div class="cover cover--placeholder">{{ title.slice(0, 2) }}</div>
      <span v-if="isLive" class="live-tag">
        <span class="live-dot" />直播中
      </span>
    </div>
    <div class="info">
      <h3 class="title">{{ title }}</h3>
      <div class="meta">
        <AppAvatar :size="24" :src="anchorAvatar" :name="anchorNickname" />
        <span class="author">{{ anchorNickname }}</span>
      </div>
    </div>
  </router-link>
</template>

<style scoped>
.live-card {
  display: block;
  background: #fff;
  border-radius: var(--doinb-radius);
  overflow: hidden;
  border: 1px solid var(--doinb-border-light);
  transition: box-shadow 0.2s, transform 0.2s;
}

.live-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.cover-wrap {
  position: relative;
  aspect-ratio: 16 / 9;
  background: #1a1a2e;
}

.cover--placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.6);
  font-size: 24px;
  font-weight: 700;
}

.live-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: var(--doinb-macaron-b);
  color: var(--doinb-text-primary);
  font-size: 12px;
  border-radius: var(--doinb-radius-sm);
}

.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #fff;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.info {
  padding: 12px;
}

.title {
  font-size: 14px;
  font-weight: 500;
  color: var(--doinb-text-primary);
  line-height: 1.4;
  min-height: 2.8em;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 8px;
}

.meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--doinb-text-secondary);
}
</style>
