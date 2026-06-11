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

<template>
  <div class="macaron-backdrop" aria-hidden="true">
    <svg class="macaron-backdrop__svg" viewBox="0 0 1440 900" preserveAspectRatio="xMidYMid slice">
      <defs>
        <filter id="macaron-blur" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur in="SourceGraphic" stdDeviation="48" />
        </filter>
      </defs>
      <ellipse
        class="blob blob--a"
        cx="180"
        cy="120"
        rx="280"
        ry="220"
        :fill="theme.colors[0]"
        filter="url(#macaron-blur)"
        opacity="0.55"
      />
      <ellipse
        class="blob blob--b"
        cx="1280"
        cy="200"
        rx="320"
        ry="260"
        :fill="theme.colors[1]"
        filter="url(#macaron-blur)"
        opacity="0.45"
      />
      <ellipse
        class="blob blob--c"
        cx="720"
        cy="780"
        rx="400"
        ry="280"
        :fill="theme.colors[2]"
        filter="url(#macaron-blur)"
        opacity="0.2"
      />
      <ellipse
        class="blob blob--b2"
        cx="1100"
        cy="680"
        rx="200"
        ry="180"
        :fill="theme.colors[1]"
        filter="url(#macaron-blur)"
        opacity="0.35"
      />
    </svg>
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

.macaron-backdrop__svg {
  width: 100%;
  height: 100%;
}

.blob {
  animation: macaron-drift 28s ease-in-out infinite alternate;
}

.blob--b {
  animation-duration: 34s;
  animation-delay: -8s;
}

.blob--c {
  animation-duration: 40s;
  animation-delay: -14s;
}

.blob--b2 {
  animation-duration: 26s;
  animation-delay: -4s;
}

.macaron-backdrop__grain {
  position: absolute;
  inset: 0;
  opacity: 0.03;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
}

@keyframes macaron-drift {
  from {
    transform: translate(0, 0) scale(1);
  }
  to {
    transform: translate(24px, -18px) scale(1.04);
  }
}
</style>
