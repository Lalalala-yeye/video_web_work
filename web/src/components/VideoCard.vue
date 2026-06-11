<script setup>
import { computed } from 'vue'
import { VideoPlay } from '@element-plus/icons-vue'
import AppAvatar from './AppAvatar.vue'
import { resolveMediaUrl } from '@/utils/media'
import { formatRelativeTime } from '@/utils/format'

const props = defineProps({
  id: { type: [Number, String], required: true },
  title: { type: String, required: true },
  coverUrl: { type: String, default: '' },
  authorNickname: { type: String, default: '未知作者' },
  authorAvatar: { type: String, default: '' },
  createTime: { type: String, default: '' }
})

const cover = computed(() => resolveMediaUrl(props.coverUrl))
</script>

<template>
  <router-link :to="`/video/${id}`" class="video-card">
    <div class="cover-wrap">
      <img v-if="cover" :src="cover" :alt="title" class="cover" />
      <div v-else class="cover cover--placeholder">暂无封面</div>
      <div class="cover-mask">
        <el-icon class="play-icon" :size="28"><VideoPlay /></el-icon>
      </div>
    </div>
    <div class="info">
      <h3 class="title">{{ title }}</h3>
      <div class="meta">
        <AppAvatar :size="24" :src="authorAvatar" :name="authorNickname" />
        <span class="author">{{ authorNickname }}</span>
        <span class="time">{{ formatRelativeTime(createTime) }}</span>
      </div>
    </div>
  </router-link>
</template>

<style scoped>
.video-card {
  display: block;
  background: #fff;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid var(--doinb-border-light);
  transition: box-shadow 0.25s, transform 0.25s, filter 0.25s;
}

.video-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.cover-wrap {
  position: relative;
  aspect-ratio: 16 / 9;
  background: var(--doinb-bg-page);
  overflow: hidden;
}

.cover-wrap::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 22%;
  background: rgba(25, 28, 42, 0.18);
  pointer-events: none;
}

.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.video-card:hover .cover {
  transform: scale(1.035);
}

.cover--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--doinb-text-secondary);
  font-size: 13px;
}

.cover-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0);
  transition: background 0.25s;
}

.video-card:hover .cover-mask {
  background: rgba(0, 0, 0, 0.15);
}

.play-icon {
  color: #fff;
  opacity: 0;
  filter: drop-shadow(0 5px 14px rgba(0, 0, 0, 0.22));
  transition: opacity 0.2s, transform 0.2s;
}

.video-card:hover .play-icon {
  opacity: 1;
  transform: scale(1.08);
}

.info {
  position: relative;
  padding: 14px 14px 15px;
}

.info::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 5px;
  background: var(--card-line, var(--doinb-macaron-c));
}

.title {
  font-size: 15px;
  font-weight: 700;
  color: var(--doinb-text-primary);
  line-height: 1.35;
  min-height: 2.7em;
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

.author {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time {
  flex-shrink: 0;
}
</style>
