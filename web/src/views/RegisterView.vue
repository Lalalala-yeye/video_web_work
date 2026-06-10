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
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ],
  confirmedPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
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
      ElMessage.error(res.data.message || '注册失败')
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
    <el-card class="auth-card" shadow="never">
      <div class="auth-header">
        <div class="logo-box">D</div>
        <h1>创建账号</h1>
        <p>加入 doinb 视频平台</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="onSubmit">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" size="large" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="至少6个字符" size="large" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmedPassword">
          <el-input v-model="form.confirmedPassword" type="password" placeholder="请再次输入密码" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" native-type="submit" style="width: 100%">
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <p class="footer-link">
        已有账号？
        <router-link to="/login">立即登录</router-link>
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
