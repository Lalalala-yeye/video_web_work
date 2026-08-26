<script setup>
import { reactive, ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/user'
import { saveLoginResult, refreshStoredUser } from '@/utils/auth'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const isAddMode = computed(() => route.query.add === '1')

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
      ElMessage.error(res.data.message || '登录失败')
      return
    }
    saveLoginResult(res.data.data)
    await refreshStoredUser(true)
    ElMessage.success(res.data.message || '登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    router.push(redirect && redirect.startsWith('/') ? redirect : '/')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <el-card class="auth-card" shadow="never">
      <div class="auth-header">
        <div class="logo-box">D</div>
        <h1>{{ isAddMode ? '新标签页登录' : '欢迎回来' }}</h1>
        <p>{{ isAddMode ? '本标签页独立登录，不影响其他已打开的标签页' : '登录到 doinb 视频平台' }}</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="onSubmit">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" size="large" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" native-type="submit" style="width: 100%">
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <p class="footer-link">
        还没有账号？
        <router-link to="/register">立即注册</router-link>
      </p>
    </el-card>
  </div>
</template>

<style scoped>
.auth-page {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--doinb-bg-page);
}

.auth-card {
  width: 100%;
  max-width: 420px;
  padding: 8px;
  border-radius: var(--doinb-radius);
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo-box {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  background: var(--doinb-primary);
  color: #fff;
  border-radius: var(--doinb-radius);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
}

.auth-header h1 {
  font-size: 24px;
  margin-bottom: 8px;
  color: var(--doinb-text-primary);
}

.auth-header p {
  font-size: 14px;
  color: var(--doinb-text-secondary);
}

.footer-link {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--doinb-text-secondary);
}

.footer-link a {
  color: var(--doinb-primary);
  margin-left: 4px;
}
</style>
