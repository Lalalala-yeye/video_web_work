<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/user'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmedPassword: ''
})

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { max: 50, message: '账号不能超过50个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { max: 50, message: '密码不能超过50个字符', trigger: 'blur' }
  ],
  confirmedPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const formRef = ref()

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await register(form.username.trim(), form.password, form.confirmedPassword)
    if (res.data.code !== 200) {
      return
    }
    ElMessage.success(res.data.message || '注册成功')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <el-card class="auth-card" shadow="hover">
      <h2 class="title">注册 doinb</h2>
      <p class="subtitle">创建账号后即可登录观看与互动</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" @submit.prevent="onSubmit">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="用户名" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmedPassword">
          <el-input v-model="form.confirmedPassword" type="password" placeholder="再次输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit" style="width: 100%">
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <p class="footer-link">
        已有账号？
        <router-link to="/login">去登录</router-link>
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
  max-width: 440px;
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
