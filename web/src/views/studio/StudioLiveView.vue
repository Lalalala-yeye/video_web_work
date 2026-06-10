<script setup>
import { ref, reactive, onMounted, computed, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  fetchMyLiveList,
  createLiveRoom,
  startLive,
  stopLive,
} from '@/api/live'
import {
  startScreenPublish,
  stopScreenPublish,
  isScreenPublishing,
  isPushActive,
  getPublishingStreamKey,
  getLocalPreviewStream,
} from '@/utils/srsScreenPublish'
import { getLanHost } from '@/utils/lanUrl'
import { copyText } from '@/utils/copyText'

const router = useRouter()
const loading = ref(false)
const rooms = ref([])
const creating = ref(false)
const screenLoading = ref(false)
const obsCollapse = ref([])
const previewVideoRef = ref(null)

const createForm = reactive({
  title: '',
})

const sharingKey = computed(() => getPublishingStreamKey())
const obsPushServer = computed(() => `rtmp://${getLanHost()}:1935/live`)

function isRoomSharing(row) {
  return isScreenPublishing() && sharingKey.value === row.streamKey
}

async function loadRooms() {
  loading.value = true
  try {
    const res = await fetchMyLiveList(1, 50)
    if (res.data.code === 200) {
      rooms.value = res.data.data?.records || []
    }
  } finally {
    loading.value = false
  }
}

async function onCreate() {
  if (!createForm.title.trim()) {
    ElMessage.warning('请填写直播间标题')
    return
  }
  creating.value = true
  try {
    const res = await createLiveRoom(createForm.title.trim())
    if (res.data.code === 200) {
      ElMessage.success('创建成功')
      createForm.title = ''
      await loadRooms()
    } else {
      ElMessage.error(res.data.message || '创建失败')
    }
  } finally {
    creating.value = false
  }
}

async function ensureLive(room) {
  if (room.isLive) return true
  const res = await startLive(room.id)
  if (res.data.code === 200) {
    room.isLive = true
    return true
  }
  ElMessage.error(res.data.message || '开播失败')
  return false
}

async function onScreenStart(room) {
  if (!room.streamKey) {
    ElMessage.error('缺少推流密钥')
    return
  }
  if (isScreenPublishing() && sharingKey.value !== room.streamKey) {
    ElMessage.warning('请先停止当前房间的屏幕分享')
    return
  }
  screenLoading.value = true
  try {
    if (!await ensureLive(room)) return
    await startScreenPublish(room.streamKey, { onPreviewReady: syncPreview })
    await syncPreview()
    if (isPushActive()) {
      ElMessage.success('屏幕分享已开始，观众稍后即可看到画面')
    } else {
      ElMessage.warning('本地预览已开启，但推流到 SRS 未完全成功，观众可能看不到')
    }
    await loadRooms()
  } catch (e) {
    if (e?.name === 'NotAllowedError') {
      ElMessage.warning('已取消屏幕分享或未授权')
    } else if (isScreenPublishing()) {
      await syncPreview()
      ElMessage.warning(e.message || '推流异常，但本地预览仍可用')
    } else {
      ElMessage.error(e.message || '屏幕分享失败')
    }
  } finally {
    screenLoading.value = false
  }
}

function onScreenStop() {
  stopScreenPublish()
  syncPreview()
  ElMessage.info('已停止屏幕分享')
}

async function syncPreview() {
  for (let i = 0; i < 15; i += 1) {
    await nextTick()
    const video = previewVideoRef.value
    const stream = getLocalPreviewStream()
    if (!video) {
      await new Promise(r => setTimeout(r, 50))
      continue
    }
    if (!stream) {
      video.srcObject = null
      return
    }
    stream.getVideoTracks().forEach(t => { t.enabled = true })
    if (video.srcObject !== stream) {
      video.srcObject = stream
    }
    try {
      await video.play()
    } catch {
      /* autoplay 重试 */
    }
    if (video.readyState >= 2) return
    await new Promise(r => setTimeout(r, 80))
  }
}

function copyStreamKey(key) {
  copyText(key, '推流码已复制')
}

function copyObsServer() {
  copyText(obsPushServer.value, 'OBS 服务器地址已复制')
}

watch(sharingKey, () => syncPreview())
watch(() => isScreenPublishing(), v => { if (v) syncPreview() })

async function onStop(room) {
  if (isRoomSharing(room)) {
    stopScreenPublish()
  }
  const res = await stopLive(room.id)
  if (res.data.code === 200) {
    ElMessage.success('已停播')
    await loadRooms()
  } else {
    ElMessage.error(res.data.message || '停播失败')
  }
}

async function onObsStart(room) {
  const res = await startLive(room.id)
  if (res.data.code === 200) {
    ElMessage.success('已开播，请在 OBS 开始推流')
    obsCollapse.value = ['obs']
    await loadRooms()
  } else {
    ElMessage.error(res.data.message || '开播失败')
  }
}

function goWatch(room) {
  router.push(`/live/${room.id}`)
}

onMounted(loadRooms)
</script>

<template>
  <div class="studio-panel">
    <h1 class="page-title">我的直播</h1>
    <p class="page-subtitle">默认用浏览器屏幕分享；需要摄像头/专业效果时再使用 OBS</p>

    <el-alert
      type="success"
      :closable="false"
      show-icon
      title="推荐：浏览器屏幕分享"
      class="tip-block"
    >
      <p>点「屏幕分享开播」→ 选择要分享的屏幕或窗口 → 观众即可观看。</p>
      <p>无需安装 OBS；停止分享可点「停止分享」，结束直播点「停播」。</p>
    </el-alert>

    <el-collapse v-model="obsCollapse" class="obs-collapse">
      <el-collapse-item title="可选：使用 OBS 推流" name="obs">
        <p>
          服务器：<code>{{ obsPushServer }}</code>
          <el-button link type="primary" size="small" @click="copyObsServer">复制</el-button>
        </p>
        <p>推流码：表格中点「复制推流码」。须先「OBS 开播」再在 OBS 点开始推流。</p>
      </el-collapse-item>
    </el-collapse>

    <el-card v-show="isScreenPublishing()" shadow="never" class="preview-card">
      <h3 class="card-title preview-title">
        <span>本地预览</span>
        <el-tag v-if="isPushActive()" type="success" size="small">已推流到 SRS</el-tag>
        <el-tag v-else type="warning" size="small">仅本地预览，观众暂不可见</el-tag>
        <el-button
          v-if="sharingKey"
          link
          type="primary"
          size="small"
          @click="copyStreamKey(sharingKey)"
        >
          复制推流码
        </el-button>
      </h3>
      <video
        ref="previewVideoRef"
        class="preview-video"
        autoplay
        muted
        playsinline
        @loadedmetadata="syncPreview"
      />
      <p class="preview-hint">
        若画面全黑：请勿共享「当前浏览器标签页」，改选「整个屏幕」或其它窗口。
      </p>
    </el-card>

    <el-card shadow="never" class="form-card">
      <h3 class="card-title">创建直播间</h3>
      <div class="create-row">
        <el-input v-model="createForm.title" placeholder="直播间标题" maxlength="100" />
        <el-button type="primary" :loading="creating" @click="onCreate">创建</el-button>
      </div>
    </el-card>

    <el-card v-loading="loading" shadow="never" class="list-card">
      <h3 class="card-title">我的直播间</h3>
      <el-table v-if="rooms.length" :data="rooms" stripe>
        <el-table-column prop="title" label="标题" min-width="140" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isLive ? 'danger' : 'info'" size="small">
              {{ row.isLive ? '直播中' : '未开播' }}
            </el-tag>
            <el-tag v-if="isRoomSharing(row)" type="warning" size="small" class="sharing-tag">分享中</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="推流码" min-width="220">
          <template #default="{ row }">
            <span class="stream-key-text">{{ row.streamKey }}</span>
            <el-button link type="primary" size="small" @click="copyStreamKey(row.streamKey)">
              复制
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <template v-if="!row.isLive">
              <el-button
                type="primary"
                size="small"
                :loading="screenLoading"
                @click="onScreenStart(row)"
              >
                屏幕分享开播
              </el-button>
              <el-button size="small" @click="onObsStart(row)">OBS 开播</el-button>
            </template>
            <template v-else>
              <el-button
                v-if="!isRoomSharing(row)"
                type="primary"
                size="small"
                :loading="screenLoading"
                @click="onScreenStart(row)"
              >
                开始屏幕分享
              </el-button>
              <el-button
                v-else
                type="warning"
                size="small"
                @click="onScreenStop"
              >
                停止分享
              </el-button>
              <el-button type="warning" size="small" @click="onStop(row)">停播</el-button>
            </template>
            <el-button size="small" @click="goWatch(row)">进入直播间</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="还没有直播间，先创建一个吧" />
    </el-card>
  </div>
</template>

<style scoped>
.studio-panel {
  max-width: 960px;
}

.tip-block {
  margin-bottom: 12px;
}

.tip-block p {
  margin: 4px 0;
  font-size: 13px;
}

.obs-collapse {
  margin-bottom: 16px;
  border: none;
}

.obs-collapse p {
  margin: 4px 0;
  font-size: 13px;
}

.obs-collapse code {
  font-size: 12px;
}

.sharing-tag {
  margin-left: 4px;
}

.form-card,
.list-card {
  border-radius: var(--doinb-radius);
  margin-bottom: 16px;
}

.card-title {
  font-size: 15px;
  margin: 0 0 12px;
}

.create-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.create-row .el-input {
  flex: 1;
}

.preview-card {
  border-radius: var(--doinb-radius);
  margin-bottom: 16px;
}

.preview-title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.stream-key-text {
  font-family: ui-monospace, monospace;
  font-size: 12px;
  margin-right: 4px;
}

.preview-video {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #1a1a2e;
  border-radius: var(--doinb-radius-sm);
  object-fit: contain;
}

.preview-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--doinb-text-secondary);
}
</style>
