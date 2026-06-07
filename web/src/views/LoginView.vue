<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/user'
import { saveLoginResult } from '@/utils/auth'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const formRef = ref()

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await login(form.username.trim(), form.password)
    if (res.data.code !== 200) {
      return
    }
    saveLoginResult(res.data.data)
    ElMessage.success(res.data.message || '登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <el-card class="auth-card" shadow="hover">
      <h2 class="title">登录 doinb</h2>
      <p class="subtitle">登录后可上传视频、记录播放历史</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" @submit.prevent="onSubmit">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="用户名" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit" style="width: 100%">
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <p class="footer-link">
        还没有账号？
        <router-link to="/register">去注册</router-link>
      </p>
    </el-card>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.auth-card {
  width: 100%;
  max-width: 420px;
}

.title {
  margin: 0 0 8px;
  text-align: center;
  font-size: 1.5rem;
}

.subtitle {
  margin: 0 0 24px;
  text-align: center;
  color: #666;
  font-size: 14px;
}

.footer-link {
  text-align: center;
  margin: 16px 0 0;
  font-size: 14px;
  color: #666;
}

.footer-link a {
  color: var(--el-color-primary);
  text-decoration: none;
}
</style>
