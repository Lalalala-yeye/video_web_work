<template>
  <div>
    <h1>doinb 首页</h1>
    <p>后端状态：{{ msg }}</p>
    <el-alert
      v-if="loggedIn"
      type="success"
      :closable="false"
      show-icon
      title="已登录"
      :description="`欢迎，${user?.nickname || user?.username}`"
      style="margin-top: 16px"
    />
    <el-alert
      v-else
      type="info"
      :closable="false"
      show-icon
      title="未登录"
      description="登录后可上传视频、保存播放历史"
      style="margin-top: 16px"
    />
  </div>
</template>

<script>
import { get } from '@/network/request'
import { getUser, isLoggedIn } from '@/utils/auth'

export default {
  data() {
    return {
      msg: '检测中...',
      loggedIn: isLoggedIn(),
      user: getUser()
    }
  },
  async mounted() {
    const res = await get('/health')
    this.msg = res.data.data
  }
}
</script>
