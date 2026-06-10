<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { uploadVideo } from '@/api/video'

const router = useRouter()
const loading = ref(false)
const videoFile = ref(null)
const coverFile = ref(null)

const form = reactive({
  title: '',
  description: ''
})

function onVideoChange(_uploadFile, fileList) {
  const latest = fileList[fileList.length - 1]
  videoFile.value = latest?.raw ?? null
}

function onCoverChange(_uploadFile, fileList) {
  const latest = fileList[fileList.length - 1]
  coverFile.value = latest?.raw ?? null
}

async function onSubmit() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  if (!videoFile.value) {
    ElMessage.warning('请选择视频文件')
    return
  }
  loading.value = true
  try {
    const res = await uploadVideo({
      title: form.title.trim(),
      description: form.description.trim(),
      file: videoFile.value,
      cover: coverFile.value
    })
    if (res.data.code === 200) {
      ElMessage.success(res.data.message || '上传成功')
      const id = res.data.data?.id
      if (id) router.push(`/video/${id}`)
      else router.push('/profile')
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="studio-panel">
    <h1 class="page-title">上传视频</h1>
    <p class="page-subtitle">分享你的精彩内容</p>

    <el-card shadow="never" class="form-card">
      <el-form label-position="top">
        <el-form-item label="视频文件" required>
          <el-upload
            drag
            :auto-upload="false"
            :limit="1"
            accept="video/mp4,video/webm,video/quicktime,.mp4,.webm,.mov"
            @change="onVideoChange"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽视频到此处，或 <em>点击上传</em></div>
          </el-upload>
        </el-form-item>
        <el-form-item label="封面图（可选）">
          <el-upload :auto-upload="false" :limit="1" accept="image/*" list-type="picture" @change="onCoverChange">
            <el-button>选择封面</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入视频标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="介绍一下视频内容" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" @click="onSubmit">提交上传</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.studio-panel {
  max-width: 720px;
}

.form-card {
  border-radius: var(--doinb-radius);
}
</style>
