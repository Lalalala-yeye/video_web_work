import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

/** SRS 直播 FLV/HLS 长连接代理（避免缓冲截断） */
function configureLiveMediaProxy(proxy) {
  proxy.on('proxyReq', (proxyReq, req) => {
    const url = req.url || ''
    if (url.includes('.flv') || url.includes('.m3u8') || url.includes('.ts')) {
      proxyReq.setHeader('Accept-Encoding', 'identity')
      proxyReq.setHeader('Connection', 'keep-alive')
    }
  })
  proxy.on('proxyRes', (proxyRes, req) => {
    const url = req.url || ''
    if (url.includes('.flv')) {
      delete proxyRes.headers['content-length']
    }
  })
}

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': path.resolve(__dirname, 'src') }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          if (id.includes('element-plus') || id.includes('@element-plus')) return 'element-plus'
          if (id.includes('hls.js') || id.includes('flv.js')) return 'live-player'
          if (id.includes('vue') || id.includes('vue-router')) return 'vue-vendor'
          if (id.includes('axios')) return 'http-vendor'
          return 'vendor'
        },
      },
    },
  },
  server: {
    host: true,
    port: 8787,
    open: process.env.CI !== 'true',
    proxy: {
      '/api': {
        target: process.env.E2E_API || 'http://127.0.0.1:8081',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/api/, ''),
        timeout: 120000,
      },
      '/srs-api': {
        target: 'http://127.0.0.1:1985',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/srs-api/, '')
      },
      '/live-media': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/live-media/, '/live'),
        timeout: 0,
        proxyTimeout: 0,
        configure: configureLiveMediaProxy,
      }
    }
  }
})
