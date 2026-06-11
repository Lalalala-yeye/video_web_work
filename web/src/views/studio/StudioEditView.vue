<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchMyVideos,
  updateVideo,
  deleteVideo,
  videoStatusLabel,
  statusToVisibility,
} from '@/api/video'
import { resolveMediaUrl } from '@/utils/media'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const videos = ref([])
const selectedId = ref(null)
const currentCover = ref('')

const form = reactive({
  title: '',
  description: '',
  visibility: 'public',
})

const videoFile = ref(null)
const coverFile = ref(null)

const editId = computed(() => {
  const id = route.params.id
  return id ? Number(id) : null
})

const visibilityOptions = [
  { value: 'public', label: '他人可见', hint: '保存后进入待审核，管理员通过后公开展示' },
  { value: 'private', label: '仅自己可见', hint: '不会出现在公开列表，仅本人可预览' },
]

async function loadList() {
  const res = await fetchMyVideos(1, 100)
  if (res.data.code === 200) {
    videos.value = res.data.data?.records || []
  }
}

function applyVideo(v) {
  if (!v) return false
  selectedId.value = v.id
  form.title = v.title || ''
  form.description = v.description || ''
  form.visibility = statusToVisibility(v.status)
  currentCover.value = v.coverUrl || ''
  videoFile.value = null
  coverFile.value = null
  return true
}

function findVideo(id) {
  return videos.value.find(item => Number(item.id) === Number(id))
}

async function loadVideo(id) {
  if (!id) return
  loading.value = true
  try {
    let v = findVideo(id)
    if (!v) {
      await loadList()
      v = findVideo(id)
    }
    if (!applyVideo(v)) {
      ElMessage.warning('未找到该视频')
    }
  } finally {
    loading.value = false
  }
}

function onVideoChange(_uploadFile, fileList) {
  videoFile.value = fileList[fileList.length - 1]?.raw ?? null
}

function onCoverChange(_uploadFile, fileList) {
  coverFile.value = fileList[fileList.length - 1]?.raw ?? null
}

function selectVideo(id) {
  router.push(`/studio/edit/${id}`)
}

async function onSave() {
  if (!selectedId.value) {
    ElMessage.warning('请先选择要修改的视频')
    return
  }
  if (!form.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  saving.value = true
  try {
    const res = await updateVideo({
      id: selectedId.value,
      title: form.title.trim(),
      description: form.description.trim(),
      visibility: form.visibility,
      file: videoFile.value,
      cover: coverFile.value,
    })
    if (res.data.code !== 200) {
      ElMessage.error(res.data.message || '保存失败')
      return
    }
    ElMessage.success(res.data.message || '保存成功')
    await loadList()
    await loadVideo(selectedId.value)
  } finally {
    saving.value = false
  }
}

async function onDelete() {
  if (!selectedId.value) return
  try {
    await ElMessageBox.confirm('确定删除该稿件？此操作不可恢复', '删除确认', { type: 'warning' })
  } catch {
    return
  }
  const res = await deleteVideo(selectedId.value)
  if (res.data.code === 200) {
    ElMessage.success('已删除')
    router.push('/studio/edit')
    selectedId.value = null
    await loadList()
  } else {
    ElMessage.error(res.data.message || '删除失败')
  }
}

onMounted(async () => {
  await loadList()
  if (editId.value) await loadVideo(editId.value)
})

watch(editId, id => {
  if (id) loadVideo(id)
  else {
    selectedId.value = null
    form.title = ''
    form.description = ''
    form.visibility = 'public'
    currentCover.value = ''
  }
})
</script>

<template>
  <div class="studio-panel">
    <h1 class="page-title">修改视频</h1>
    <p class="page-subtitle">编辑内容并选择可见范围；删除稿件请使用下方按钮</p>

    <div class="edit-layout">
      <el-card shadow="never" class="list-card">
        <h3 class="card-title">我的视频</h3>
        <div v-if="videos.length" class="video-list">
          <button
            v-for="v in videos"
            :key="v.id"
            type="button"
            class="video-item"
            :class="{ active: selectedId === v.id }"
            @click="selectVideo(v.id)"
          >
            <img v-if="v.coverUrl" :src="resolveMediaUrl(v.coverUrl)" alt="" class="thumb" />
            <div v-else class="thumb thumb--empty">无封面</div>
            <div class="meta">
              <span class="name">{{ v.title }}</span>
              <span class="status">{{ videoStatusLabel(v.status) }}</span>
            </div>
          </button>
        </div>
        <el-empty v-else description="还没有上传视频" />
      </el-card>

      <el-card v-loading="loading" shadow="never" class="form-card">
        <template v-if="selectedId">
          <el-form label-position="top">
            <el-form-item v-if="currentCover && !coverFile" label="当前封面">
              <img :src="resolveMediaUrl(currentCover)" alt="" class="current-cover" />
            </el-form-item>
            <el-form-item label="更换封面（可选）">
              <el-upload :auto-upload="false" :limit="1" accept="image/*" list-type="picture" @change="onCoverChange">
                <el-button>选择新封面</el-button>
              </el-upload>
            </el-form-item>
            <el-form-item label="更换视频文件（可选）">
              <el-upload
                :auto-upload="false"
                :limit="1"
                accept="video/mp4,video/webm,video/quicktime,.mp4,.webm,.mov"
                @change="onVideoChange"
              >
                <el-button>选择新视频</el-button>
              </el-upload>
            </el-form-item>
            <el-form-item label="标题" required>
              <el-input v-model="form.title" maxlength="100" show-word-limit />
            </el-form-item>
            <el-form-item label="简介">
              <el-input v-model="form.description" type="textarea" :rows="4" />
            </el-form-item>
            <el-form-item label="可见范围">
              <el-radio-group v-model="form.visibility" class="visibility-group">
                <div v-for="opt in visibilityOptions" :key="opt.value" class="visibility-option">
                  <el-radio :value="opt.value">{{ opt.label }}</el-radio>
                  <span class="hint">{{ opt.hint }}</span>
                </div>
              </el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="onSave">保存修改</el-button>
              <el-button type="danger" plain @click="onDelete">删除稿件</el-button>
              <router-link :to="`/video/${selectedId}`">
                <el-button>预览</el-button>
              </router-link>
            </el-form-item>
          </el-form>
        </template>
        <el-empty v-else description="从左侧选择要修改的视频" />
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.edit-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 16px;
  align-items: start;
}

.list-card,
.form-card {
  border-radius: var(--doinb-radius);
}

.card-title {
  font-size: 15px;
  margin: 0 0 12px;
}

.video-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 520px;
  overflow-y: auto;
}

.video-item {
  display: flex;
  gap: 10px;
  align-items: center;
  width: 100%;
  padding: 8px;
  border: 1px solid var(--doinb-border-light);
  border-radius: var(--doinb-radius-sm);
  background: #fff;
  cursor: pointer;
  text-align: left;
}

.video-item.active {
  border-color: var(--doinb-primary);
  background: var(--doinb-primary-bg);
}

.thumb {
  width: 72px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
  background: #000;
}

.thumb--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #fff;
  background: #333;
}

.meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.name {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status {
  font-size: 12px;
  color: var(--doinb-text-secondary);
}

.current-cover {
  max-width: 240px;
  border-radius: var(--doinb-radius-sm);
}

.visibility-group {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
}

.visibility-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.hint {
  font-size: 12px;
  color: var(--doinb-text-secondary);
  margin-left: 22px;
}

@media (max-width: 900px) {
  .edit-layout {
    grid-template-columns: 1fr;
  }
}
</style>
