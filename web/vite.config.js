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
  server: {
    host: true,
    port: 8787,
    open: true,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
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
        target: 'http://127.0.0.1:8088',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/live-media/, '/live'),
        timeout: 0,
        proxyTimeout: 0,
        configure: configureLiveMediaProxy,
      }
    }
  }
})
