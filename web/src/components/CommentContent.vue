<script setup>
import { computed } from 'vue'
import { parseCommentContent } from '@/constants/emojis'

const props = defineProps({
  content: { type: String, required: true }
})

const segments = computed(() => parseCommentContent(props.content))
</script>

<template>
  <span class="comment-content">
    <template v-for="(seg, index) in segments" :key="index">
      <img
        v-if="seg.type === 'image'"
        class="emoji-img"
        :src="seg.emoji.file"
        :alt="seg.emoji.label"
        :title="seg.emoji.name"
      />
      <span v-else>{{ seg.value }}</span>
    </template>
  </span>
</template>

<style scoped>
.comment-content {
  font-size: 14px;
  color: var(--doinb-text-primary);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.emoji-img {
  width: 22px;
  height: 22px;
  vertical-align: text-bottom;
  object-fit: contain;
  margin: 0 1px;
}
</style>
