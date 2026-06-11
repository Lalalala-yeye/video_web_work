<script setup>
import { computed } from 'vue'
import AppAvatar from './AppAvatar.vue'
import { resolveMediaUrl } from '@/utils/media'

const props = defineProps({
  id: { type: [Number, String], required: true },
  title: { type: String, required: true },
  coverUrl: { type: String, default: '' },
  anchorNickname: { type: String, default: '未知主播' },
  anchorAvatar: { type: String, default: '' },
  isLive: { type: Boolean, default: false }
})

const cover = computed(() => resolveMediaUrl(props.coverUrl))
</script>

<template>
  <router-link :to="`/live/${id}`" class="live-card">
    <div class="cover-wrap">
      <img v-if="cover" :src="cover" :alt="title" class="cover" />
      <div v-else class="cover cover--placeholder">{{ title.slice(0, 2) }}</div>
      <span v-if="isLive" class="live-tag">
        <span class="live-dot" />直播中
      </span>
    </div>
    <div class="info">
      <h3 class="title">{{ title }}</h3>
      <div class="meta">
        <AppAvatar :size="18" :src="anchorAvatar" :name="anchorNickname" />
        <span class="author">{{ anchorNickname }}</span>
      </div>
    </div>
  </router-link>
</template>

<style scoped>
.live-card {
  display: block;
  background: transparent;
  border: none;
  transition: opacity 0.2s;
}

.cover-wrap {
  position: relative;
  aspect-ratio: 16 / 9;
  background: #11111c;
  border-radius: 6px;
  overflow: hidden;
}

.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.42);
  font-size: 16px;
  font-weight: 700;
  background: #11111c;
  text-shadow: 0 8px 22px rgba(0, 0, 0, 0.24);
}

.live-tag {
  position: absolute;
  top: 6px;
  left: 6px;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 1px 6px;
  background: color-mix(in srgb, var(--doinb-macaron-b) 88%, white);
  color: var(--doinb-text-primary);
  font-size: 10px;
  border-radius: 999px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.12);
}

.live-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #fff;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.info {
  padding: 8px 0 0;
}

.title {
  font-size: 12px;
  font-weight: 500;
  color: var(--doinb-text-primary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 4px;
}

.meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--doinb-text-secondary);
}

.author {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
