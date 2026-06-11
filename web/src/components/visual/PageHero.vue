<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
})
</script>

<template>
  <header class="page-hero">
    <div class="page-hero__accent" aria-hidden="true">
      <span class="page-hero__dot page-hero__dot--a" />
      <span class="page-hero__dot page-hero__dot--b" />
      <span class="page-hero__dot page-hero__dot--c" />
    </div>
    <h1 class="page-hero__title">{{ title }}</h1>
    <p v-if="subtitle" class="page-hero__subtitle">{{ subtitle }}</p>
    <div class="page-hero__line" aria-hidden="true" />
  </header>
</template>

<style scoped>
.page-hero {
  position: relative;
  margin-bottom: 28px;
  padding-left: 4px;
}

.page-hero__accent {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
}

.page-hero__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.page-hero__dot--a {
  background: var(--doinb-macaron-a);
}

.page-hero__dot--b {
  background: var(--doinb-macaron-b);
  transform: translateY(3px);
}

.page-hero__dot--c {
  background: var(--doinb-macaron-c);
  transform: translateY(-2px);
}

.page-hero__title {
  font-size: 28px;
  font-weight: 700;
  color: var(--doinb-text-primary);
  letter-spacing: -0.02em;
  margin: 0 0 6px;
  line-height: 1.25;
}

.page-hero__subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--doinb-text-secondary);
  max-width: 36em;
  line-height: 1.6;
}

.page-hero__line {
  margin-top: 16px;
  height: 3px;
  width: min(120px, 40%);
  border-radius: 3px;
  background: linear-gradient(
    90deg,
    var(--doinb-macaron-a) 0%,
    var(--doinb-macaron-b) 45%,
    var(--doinb-macaron-c) 100%
  );
  opacity: 0.85;
}
</style>
