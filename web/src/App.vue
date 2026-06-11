<script setup>
import { computed, watch, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import TopNavigation from '@/components/layout/TopNavigation.vue'
import MacaronBackdrop from '@/components/visual/MacaronBackdrop.vue'
import { applyMacaronTheme } from '@/utils/macaronTheme'
import { MACARON_THEMES } from '@/constants/macaronThemes'

const route = useRoute()

const hideNav = computed(() => ['/login', '/register'].includes(route.path))
const showMacaronVisual = computed(() => !route.path.startsWith('/admin'))

watch(() => route.path, path => applyMacaronTheme(path), { immediate: true })

onUnmounted(() => {
  const root = document.documentElement
  MACARON_THEMES.forEach(t => root.classList.remove(`theme-macaron-${t.id}`))
})
</script>

<template>
  <div class="app-layout">
    <MacaronBackdrop v-if="showMacaronVisual" />
    <TopNavigation v-if="!hideNav" />
    <main :class="['main', { 'main--auth': hideNav }]">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-layout {
  min-height: 100vh;
  background: var(--doinb-bg-page);
}

.main {
  min-height: 100vh;
}

.main--auth {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
