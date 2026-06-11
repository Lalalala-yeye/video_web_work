<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchMyLiveList,
  createLiveRoom,
  startLive,
  stopLive,
} from '@/api/live'
import { getPushRtmpServer } from '@/utils/lanUrl'
import { copyText } from '@/utils/copyText'

const router = useRouter()
const loading = ref(false)
const rooms = ref([])
const creating = ref(false)
const obsCollapse = ref([])

const createForm = reactive({
  title: '',
})

const obsPushServer = computed(() => getPushRtmpServer())

function showScreenShareNotReady() {
  ElMessageBox.alert(
    '浏览器屏幕分享功能尚未完善，请使用 OBS 推流。',
    '功能未完善',
    { confirmButtonText: '知道了', type: 'info' }
  )
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

function copyStreamKey(key) {
  copyText(key, '推流码已复制')
}

function copyObsServer() {
  copyText(obsPushServer.value, 'OBS 服务器地址已复制')
}

async function onStop(room) {
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
    <p class="page-subtitle">推荐使用 OBS 推流；浏览器屏幕分享功能尚未完善</p>

    <el-alert
      type="success"
      :closable="false"
      show-icon
      title="推荐：OBS 推流"
      class="tip-block"
    >
      <p>点「OBS 开播」→ 在 OBS 中配置推流 → 开始推流 → 观众进入直播间观看。</p>
      <p>展开下方可查看服务器与推流码填写方式。</p>
    </el-alert>

    <el-collapse v-model="obsCollapse" class="obs-collapse">
      <el-collapse-item title="可选：使用 OBS 推流" name="obs">
        <p>
          服务器：
          <el-button link type="primary" size="small" @click="copyObsServer">复制服务器</el-button>
        </p>
        <p>推流码：表格中点「复制推流码」。须先「OBS 开播」再在 OBS 点开始推流。</p>
      </el-collapse-item>
    </el-collapse>

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
                @click="showScreenShareNotReady"
              >
                屏幕分享开播
              </el-button>
              <el-button size="small" @click="onObsStart(row)">OBS 开播</el-button>
            </template>
            <template v-else>
              <el-button
                type="primary"
                size="small"
                @click="showScreenShareNotReady"
              >
                开始屏幕分享
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

.stream-key-text {
  font-family: ui-monospace, monospace;
  font-size: 12px;
  margin-right: 4px;
}
</style>
