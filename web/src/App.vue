<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import TopNavigation from '@/components/layout/TopNavigation.vue'

const route = useRoute()

const hideNav = computed(() => ['/login', '/register'].includes(route.path))
</script>

<template>
  <div class="app-layout">
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
