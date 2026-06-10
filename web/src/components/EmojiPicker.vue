<script setup>
import { UNICODE_EMOJIS, IMAGE_EMOJIS } from '@/constants/emojis'

defineProps({
  visible: { type: Boolean, default: false }
})

const emit = defineEmits(['select', 'close'])

function pickUnicode(item) {
  emit('select', { type: 'unicode', code: item.code })
  emit('close')
}

function pickImage(item) {
  emit('select', { type: 'image', name: item.name })
  emit('close')
}
</script>

<template>
  <div v-if="visible" class="emoji-picker">
    <div class="section">
      <div class="section-title">系统表情</div>
      <div class="emoji-grid">
        <button
          v-for="item in UNICODE_EMOJIS"
          :key="item.code"
          type="button"
          class="emoji-btn emoji-btn--unicode"
          :title="item.label"
          @click="pickUnicode(item)"
        >
          {{ item.code }}
        </button>
      </div>
    </div>

    <div class="section">
      <div class="section-title">图片表情（放入 public/emojis/）</div>
      <div class="emoji-grid">
        <button
          v-for="item in IMAGE_EMOJIS"
          :key="item.name"
          type="button"
          class="emoji-btn emoji-btn--image"
          :title="`${item.label} ${item.name}`"
          @click="pickImage(item)"
        >
          <img :src="item.file" :alt="item.label" class="emoji-thumb" />
        </button>
      </div>
      <p class="hint">点击插入 {{ '[名称]' }}，评论中会显示为图片；无 png 时浏览器显示裂图，请按 README 放图。</p>
    </div>
  </div>
</template>

<style scoped>
.emoji-picker {
  padding: 10px;
  margin-top: 8px;
  background: var(--doinb-bg-page);
  border-radius: var(--doinb-radius-sm);
  border: 1px solid var(--doinb-border-light);
}

.section + .section {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--doinb-border-light);
}

.section-title {
  font-size: 12px;
  color: var(--doinb-text-secondary);
  margin-bottom: 8px;
}

.emoji-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.emoji-btn {
  border: none;
  background: #fff;
  border-radius: var(--doinb-radius-sm);
  cursor: pointer;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.emoji-btn--unicode {
  width: 36px;
  height: 36px;
  font-size: 20px;
  line-height: 1;
}

.emoji-btn--image {
  width: 40px;
  height: 40px;
}

.emoji-btn:hover {
  background: #ecf5ff;
}

.emoji-thumb {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.hint {
  margin: 8px 0 0;
  font-size: 11px;
  color: var(--doinb-text-placeholder);
  line-height: 1.5;
}
</style>
