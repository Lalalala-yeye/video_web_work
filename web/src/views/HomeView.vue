<template>
  <div>
    <h1>doinb 首页</h1>
    <p>后端状态：{{ msg }}</p>
  </div>
</template>

<script>
import { get } from '@/network/request'

export default {
  data() {
    return { msg: '检测中...' }
  },
  async mounted() {
    const res = await get('/health')
    this.msg = res.data.data
  }
}
</script>