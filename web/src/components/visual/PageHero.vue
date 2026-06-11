<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
})

const orbitTiles = Array.from({ length: 16 }, (_, index) => index + 1)
</script>

<template>
  <header class="page-hero">
    <div class="page-hero__art" aria-hidden="true">
      <div class="page-hero__scene">
        <span class="page-hero__slab page-hero__slab--a" />
        <span class="page-hero__slab page-hero__slab--b" />
        <span class="page-hero__slab page-hero__slab--c" />
        <span class="page-hero__center-line" />
        <span
          v-for="tile in orbitTiles"
          :key="tile"
          :class="['page-hero__diamond', `page-hero__diamond--${tile}`]"
        />
      </div>
      <div class="page-hero__brand">
        <strong>DOINB</strong>
        <span>VIDEO</span>
      </div>
    </div>
    <div class="page-hero__panel">
      <p class="page-hero__eyebrow">Featured Gallery</p>
      <h1 class="page-hero__title">{{ title }}</h1>
      <p v-if="subtitle" class="page-hero__subtitle">{{ subtitle }}</p>
      <div class="page-hero__palette" aria-hidden="true">
        <span class="page-hero__swatch page-hero__swatch--a" />
        <span class="page-hero__swatch page-hero__swatch--b" />
        <span class="page-hero__swatch page-hero__swatch--c" />
      </div>
      <div class="page-hero__line" aria-hidden="true" />
    </div>
  </header>
</template>

<style scoped>
.page-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(280px, 0.8fr);
  min-height: 270px;
  margin-bottom: 28px;
  overflow: hidden;
  border-radius: 4px;
  background: #fff;
  box-shadow: 0 18px 48px color-mix(in srgb, var(--doinb-macaron-c) 14%, transparent);
}

.page-hero__art {
  position: relative;
  min-height: 270px;
  overflow: hidden;
  background: #f8f8f6;
}

.page-hero__scene {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--doinb-macaron-a) 20%, white) 0 24%, #fff 24% 100%);
  filter: saturate(1.42) contrast(1.04);
}

.page-hero__center-line {
  position: absolute;
  left: 16%;
  right: 16%;
  top: 50%;
  height: 7px;
  display: block;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow:
    0 24px 42px rgba(36, 38, 56, 0.3),
    0 10px 18px rgba(36, 38, 56, 0.16),
    0 0 0 1px rgba(255, 255, 255, 0.5);
  z-index: 3;
}

.page-hero__slab {
  position: absolute;
  display: block;
  height: 24px;
  border-radius: 8px;
  transform: rotate(-31deg) skewX(-14deg);
  transform-origin: left center;
  box-shadow:
    0 26px 42px rgba(36, 38, 56, 0.2),
    0 10px 18px rgba(36, 38, 56, 0.14),
    inset 0 8px 12px rgba(255, 255, 255, 0.2);
  opacity: 0.82;
  z-index: 1;
}

.page-hero__slab--a {
  left: 24%;
  top: 68%;
  width: 64%;
  background: var(--doinb-macaron-c);
}

.page-hero__slab--b {
  left: 32%;
  top: 63%;
  width: 56%;
  background: var(--doinb-macaron-b);
}

.page-hero__slab--c {
  left: 28%;
  top: 92%;
  width: 52%;
  background: var(--doinb-macaron-a);
}

.page-hero__diamond {
  position: absolute;
  display: block;
  width: 38px;
  height: 24px;
  border-radius: 5px;
  transform: skewX(-18deg) rotate(-18deg);
  box-shadow:
    0 22px 34px rgba(36, 38, 56, 0.24),
    0 10px 18px rgba(36, 38, 56, 0.16),
    inset 0 7px 11px rgba(255, 255, 255, 0.3);
}

.page-hero__diamond--1,
.page-hero__diamond--4,
.page-hero__diamond--7,
.page-hero__diamond--10,
.page-hero__diamond--13,
.page-hero__diamond--16,
.page-hero__diamond--19,
.page-hero__diamond--22 {
  background: var(--doinb-macaron-c);
}

.page-hero__diamond--2,
.page-hero__diamond--5,
.page-hero__diamond--8,
.page-hero__diamond--11,
.page-hero__diamond--14,
.page-hero__diamond--17,
.page-hero__diamond--20,
.page-hero__diamond--23 {
  background: var(--doinb-macaron-b);
}

.page-hero__diamond--3,
.page-hero__diamond--6,
.page-hero__diamond--9,
.page-hero__diamond--12,
.page-hero__diamond--15,
.page-hero__diamond--18,
.page-hero__diamond--21,
.page-hero__diamond--24 {
  background: var(--doinb-macaron-a);
}

.page-hero__diamond--1 { left: 11%; top: 64%; transform: skewX(-20deg) rotate(-33deg) scale(0.42); z-index: 7; opacity: 0.78; }
.page-hero__diamond--2 { left: 16%; top: 55%; transform: skewX(-20deg) rotate(-35deg) scale(0.56); z-index: 8; }
.page-hero__diamond--3 { left: 21%; top: 45%; transform: skewX(-20deg) rotate(-36deg) scale(0.74); z-index: 8; }
.page-hero__diamond--4 { left: 27%; top: 34%; transform: skewX(-20deg) rotate(-34deg) scale(0.94); z-index: 8; }
.page-hero__diamond--5 { left: 34%; top: 25%; transform: skewX(-20deg) rotate(-30deg) scale(1.1); z-index: 8; }
.page-hero__diamond--6 { left: 42%; top: 21%; transform: skewX(-18deg) rotate(-20deg) scale(1.04); z-index: 8; }
.page-hero__diamond--7 { left: 50%; top: 27%; transform: skewX(-18deg) rotate(-8deg) scale(0.86); z-index: 7; }
.page-hero__diamond--8 { left: 55%; top: 39%; transform: skewX(-18deg) rotate(5deg) scale(0.66); z-index: 2; opacity: 0.48; }
.page-hero__diamond--9 { left: 59%; top: 52%; transform: skewX(-18deg) rotate(18deg) scale(0.7); z-index: 2; opacity: 0.52; }
.page-hero__diamond--10 { left: 64%; top: 62%; transform: skewX(-18deg) rotate(29deg) scale(0.84); z-index: 2; opacity: 0.58; }
.page-hero__diamond--11 { left: 70%; top: 62%; transform: skewX(-18deg) rotate(36deg) scale(1); z-index: 2; opacity: 0.62; }
.page-hero__diamond--12 { left: 76%; top: 52%; transform: skewX(-18deg) rotate(39deg) scale(0.92); z-index: 7; opacity: 0.9; }
.page-hero__diamond--13 { left: 80%; top: 39%; transform: skewX(-18deg) rotate(36deg) scale(0.78); z-index: 7; }
.page-hero__diamond--14 { left: 82%; top: 27%; transform: skewX(-18deg) rotate(32deg) scale(0.66); z-index: 7; }
.page-hero__diamond--15 { left: 81%; top: 17%; transform: skewX(-18deg) rotate(28deg) scale(0.54); z-index: 7; opacity: 0.88; }
.page-hero__diamond--16 { left: 78%; top: 10%; transform: skewX(-18deg) rotate(24deg) scale(0.46); z-index: 7; opacity: 0.78; }

.page-hero__diamond--7,
.page-hero__diamond--8,
.page-hero__diamond--9,
.page-hero__diamond--10,
.page-hero__diamond--11,
.page-hero__diamond--12 {
  filter: saturate(0.8);
}

.page-hero__diamond--1,
.page-hero__diamond--2,
.page-hero__diamond--3,
.page-hero__diamond--4,
.page-hero__diamond--5,
.page-hero__diamond--6 {
  box-shadow:
    0 30px 44px rgba(36, 38, 56, 0.3),
    0 12px 22px rgba(36, 38, 56, 0.18),
    inset 0 8px 12px rgba(255, 255, 255, 0.35);
}

.page-hero__diamond--13,
.page-hero__diamond--14,
.page-hero__diamond--15,
.page-hero__diamond--16 {
  box-shadow:
    0 24px 36px rgba(36, 38, 56, 0.24),
    0 10px 18px rgba(36, 38, 56, 0.15),
    inset 0 8px 12px rgba(255, 255, 255, 0.28);
}

.page-hero__brand {
  position: absolute;
  left: 28px;
  bottom: 24px;
  display: grid;
  color: #fff;
  text-shadow:
    0 16px 28px rgba(36, 38, 56, 0.32),
    0 6px 12px rgba(36, 38, 56, 0.22),
    0 1px 0 rgba(255, 255, 255, 0.25);
}

.page-hero__brand strong {
  font-size: 30px;
  line-height: 0.9;
}

.page-hero__brand span {
  font-size: 11px;
  letter-spacing: 0;
}

.page-hero__panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 34px 36px;
  text-align: center;
}

.page-hero__eyebrow {
  margin: 0 0 14px;
  font-size: 12px;
  font-weight: 700;
  color: var(--doinb-text-secondary);
}

.page-hero__title {
  font-size: clamp(26px, 4vw, 42px);
  font-weight: 800;
  color: var(--doinb-text-primary);
  letter-spacing: 0;
  margin: 0 0 10px;
  line-height: 1.08;
}

.page-hero__subtitle {
  margin: 0 auto;
  font-size: 15px;
  color: var(--doinb-text-secondary);
  max-width: 30em;
  line-height: 1.6;
}

.page-hero__palette {
  display: grid;
  grid-template-columns: repeat(3, 42px);
  justify-content: center;
  gap: 24px;
  margin: 26px 0 0;
}

.page-hero__swatch {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  position: relative;
  box-shadow:
    0 8px 18px rgba(36, 38, 56, 0.14),
    0 0 0 6px rgba(255, 255, 255, 0.55);
  filter: saturate(1.38) contrast(1.04);
}

.page-hero__swatch::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: -15px;
  width: 1px;
  height: 9px;
  background: var(--doinb-border);
}

.page-hero__swatch--a {
  background: var(--doinb-macaron-a);
}

.page-hero__swatch--b {
  background: var(--doinb-macaron-b);
}

.page-hero__swatch--c {
  background: var(--doinb-macaron-c);
}

.page-hero__line {
  margin: 28px auto 0;
  height: 4px;
  width: min(170px, 56%);
  border-radius: 3px;
  background: linear-gradient(90deg, var(--doinb-macaron-a) 0 33%, var(--doinb-macaron-b) 33% 66%, var(--doinb-macaron-c) 66% 100%);
  opacity: 0.85;
}

@media (max-width: 820px) {
  .page-hero {
    grid-template-columns: 1fr;
  }

  .page-hero__art {
    min-height: 190px;
  }

  .page-hero__panel {
    padding: 28px 20px 30px;
  }
}
</style>
