<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { getMacaronTheme, MACARON_THEME_EVENT } from '@/utils/macaronTheme'

const theme = ref(getMacaronTheme())

function refresh() {
  theme.value = getMacaronTheme()
}

onMounted(() => {
  window.addEventListener(MACARON_THEME_EVENT, refresh)
})

onUnmounted(() => {
  window.removeEventListener(MACARON_THEME_EVENT, refresh)
})
</script>

<template>
  <div class="macaron-backdrop" aria-hidden="true">
    <div
      class="macaron-backdrop__planes"
      :style="{ '--plane-a': theme.colors[0], '--plane-b': theme.colors[1], '--plane-c': theme.colors[2] }"
    />
    <div class="macaron-backdrop__band macaron-backdrop__band--top" />
    <div class="macaron-backdrop__band macaron-backdrop__band--bottom" />
    <div class="macaron-backdrop__grain" />
  </div>
</template>

<style scoped>
.macaron-backdrop {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.macaron-backdrop__planes {
  position: absolute;
  inset: 0;
  opacity: 0.2;
  background:
    linear-gradient(90deg, transparent 0 9%, var(--plane-a) 9% 18%, transparent 18% 100%),
    linear-gradient(90deg, transparent 0 72%, var(--plane-b) 72% 84%, transparent 84% 100%),
    linear-gradient(180deg, transparent 0 70%, var(--plane-c) 70% 82%, transparent 82% 100%);
}

.macaron-backdrop__band {
  position: absolute;
  left: 0;
  right: 0;
  height: 92px;
  opacity: 0.36;
}

.macaron-backdrop__band--top {
  top: 86px;
  background:
    linear-gradient(90deg, var(--doinb-macaron-a) 0 24%, transparent 24% 54%, var(--doinb-macaron-b) 54% 68%, transparent 68% 100%);
}

.macaron-backdrop__band--bottom {
  bottom: 0;
  height: 116px;
  background:
    linear-gradient(90deg, var(--doinb-macaron-c) 0 32%, transparent 32% 58%, var(--doinb-macaron-b) 58% 72%, var(--doinb-macaron-a) 72% 100%);
}

.macaron-backdrop__grain {
  position: absolute;
  inset: 0;
  opacity: 0.03;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
}
</style>
