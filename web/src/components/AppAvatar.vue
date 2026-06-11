<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { resolveMediaUrl } from '@/utils/media'

const props = defineProps({
  src: { type: String, default: '' },
  name: { type: String, default: '' },
  size: { type: Number, default: 40 },
  userId: { type: Number, default: null },
  clickable: { type: Boolean, default: true }
})

const avatarSrc = computed(() => resolveMediaUrl(props.src))

function initials(name) {
  if (!name) return '?'
  const parts = name.trim().split(/\s+/)
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase()
  return name[0].toUpperCase()
}
</script>

<template>
  <RouterLink
    v-if="userId && clickable"
    :to="`/user/${userId}`"
    class="app-avatar-link"
    @click.stop
  >
    <div class="app-avatar" :style="{ width: `${size}px`, height: `${size}px`, fontSize: `${size * 0.4}px` }">
      <img v-if="avatarSrc" :src="avatarSrc" :alt="name" />
      <span v-else>{{ initials(name) }}</span>
    </div>
  </RouterLink>
  <div
    v-else
    class="app-avatar"
    :style="{ width: `${size}px`, height: `${size}px`, fontSize: `${size * 0.4}px` }"
  >
    <img v-if="avatarSrc" :src="avatarSrc" :alt="name" />
    <span v-else>{{ initials(name) }}</span>
  </div>
</template>

<style scoped>
.app-avatar-link {
  display: inline-flex;
  flex-shrink: 0;
  border-radius: 50%;
  transition: opacity 0.2s;
}

.app-avatar-link:hover {
  opacity: 0.85;
}

.app-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  overflow: hidden;
  background: var(--doinb-primary-bg);
  color: var(--doinb-primary);
  font-weight: 500;
  flex-shrink: 0;
}

.app-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
