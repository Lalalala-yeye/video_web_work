<script setup>
defineProps({
  src: { type: String, default: '' },
  name: { type: String, default: '' },
  size: { type: Number, default: 40 }
})

function initials(name) {
  if (!name) return '?'
  const parts = name.trim().split(/\s+/)
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase()
  return name[0].toUpperCase()
}
</script>

<template>
  <div class="app-avatar" :style="{ width: `${size}px`, height: `${size}px`, fontSize: `${size * 0.4}px` }">
    <img v-if="src" :src="src" :alt="name" />
    <span v-else>{{ initials(name) }}</span>
  </div>
</template>

<style scoped>
.app-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  overflow: hidden;
  background: #ecf5ff;
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
