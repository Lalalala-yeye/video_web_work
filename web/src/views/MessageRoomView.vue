<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppAvatar from '@/components/AppAvatar.vue'
import { fetchMessageRoom, sendMessage } from '@/api/message'
import { formatRelativeTime } from '@/utils/format'
import { isLoggedIn } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const roomId = computed(() => Number(route.params.roomId))
const loading = ref(true)
const sending = ref(false)
const room = ref(null)
const draft = ref('')
const listRef = ref(null)

async function load() {
  if (!isLoggedIn()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  loading.value = true
  try {
    const res = await fetchMessageRoom(roomId.value)
    if (res.data.code === 200) {
      room.value = res.data.data
      await nextTick()
      scrollToBottom()
    }
  } finally {
    loading.value = false
  }
}

function scrollToBottom() {
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}

async function onSend() {
  const text = draft.value.trim()
  if (!text) return
  sending.value = true
  try {
    const res = await sendMessage(roomId.value, text)
    if (res.data.code === 200) {
      draft.value = ''
      room.value?.messages?.push(res.data.data)
      await nextTick()
      scrollToBottom()
    }
  } finally {
    sending.value = false
  }
}

onMounted(load)
watch(roomId, load)
</script>

<template>
  <div v-loading="loading" class="page-container room-page">
    <template v-if="room">
      <header class="room-header">
        <AppAvatar :size="40" :src="room.peerAvatar" :name="room.peerNickname" :user-id="room.peerId" />
        <div>
          <h1>{{ room.peerNickname || '用户' }}</h1>
          <p class="sub">与 TA 的私信</p>
        </div>
      </header>

      <div ref="listRef" class="message-list">
        <div
          v-for="msg in room.messages"
          :key="msg.id"
          class="msg-row"
          :class="{ mine: msg.mine }"
        >
          <AppAvatar
            v-if="!msg.mine"
            :size="32"
            :src="msg.senderAvatar"
            :name="msg.senderNickname"
          />
          <div class="bubble">
            <p>{{ msg.content }}</p>
            <span class="time">{{ formatRelativeTime(msg.createTime) }}</span>
          </div>
        </div>
        <el-empty v-if="!room.messages?.length" description="还没有消息，打个招呼吧" />
      </div>

      <form class="composer" @submit.prevent="onSend">
        <el-input
          v-model="draft"
          type="textarea"
          :rows="2"
          placeholder="输入消息..."
          maxlength="500"
          show-word-limit
        />
        <el-button type="primary" :loading="sending" @click="onSend">发送</el-button>
      </form>
    </template>
    <el-empty v-else-if="!loading" description="会话不存在" />
  </div>
</template>

<style scoped>
.room-page {
  max-width: 720px;
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - var(--doinb-header-height) - 48px);
}

.room-header {
  display: flex;
  gap: 12px;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--doinb-border-light);
  margin-bottom: 16px;
}

.room-header h1 {
  font-size: 18px;
  margin: 0;
}

.sub {
  font-size: 13px;
  color: var(--doinb-text-secondary);
  margin: 4px 0 0;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0 16px;
  min-height: 280px;
}

.msg-row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  align-items: flex-end;
}

.msg-row.mine {
  flex-direction: row-reverse;
}

.bubble {
  max-width: 70%;
  padding: 10px 12px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid var(--doinb-border-light);
}

.msg-row.mine .bubble {
  background: var(--doinb-primary);
  border-color: var(--doinb-primary);
  color: #fff;
}

.bubble p {
  margin: 0 0 4px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble .time {
  font-size: 11px;
  opacity: 0.75;
}

.composer {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid var(--doinb-border-light);
}
</style>
