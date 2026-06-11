import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { get, post } from './network/request'
import { setupAuthSync } from './utils/auth'
import { applyMacaronTheme } from './utils/macaronTheme'
import './assets/main.css'
import './assets/macaron-visual.css'

applyMacaronTheme(window.location.pathname)

const app = createApp(App)
app.config.globalProperties.$get = get
app.config.globalProperties.$post = post
app.use(router)
app.use(ElementPlus, { locale: zhCn })
setupAuthSync()
app.mount('#app')
