<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  fetchMyLiveList,
  createLiveRoom,
  startLive,
  stopLive,
} from '@/api/live'

const router = useRouter()
const loading = ref(false)
const rooms = ref([])
const creating = ref(false)

const createForm = reactive({
  title: '',
})

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

async function onStart(room) {
  const res = await startLive(room.id)
  if (res.data.code === 200) {
    ElMessage.success('已开播')
    await loadRooms()
  } else {
    ElMessage.error(res.data.message || '开播失败')
  }
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

function goWatch(room) {
  router.push(`/live/${room.id}`)
}

onMounted(loadRooms)
</script>

<template>
  <div class="studio-panel">
    <h1 class="page-title">我的直播</h1>
    <p class="page-subtitle">创建直播间、开播/停播；观众可在直播列表进入观看</p>

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
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isLive ? 'danger' : 'info'" size="small">
              {{ row.isLive ? '直播中' : '未开播' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="streamKey" label="推流密钥" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!row.isLive" type="primary" size="small" @click="onStart(row)">开播</el-button>
            <el-button v-else type="warning" size="small" @click="onStop(row)">停播</el-button>
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
  max-width: 900px;
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
</style>
