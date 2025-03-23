import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/main.css'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import axios from 'axios'
import { useUserStore } from './stores/user'

// 配置axios
axios.defaults.baseURL = '/'  // 使用相对路径，与Vite代理配置匹配
// 设置默认请求头为JSON，并指定UTF-8编码
axios.defaults.headers.common['Content-Type'] = 'application/json;charset=UTF-8'
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 创建应用实例
const app = createApp(App)
const pinia = createPinia()

// 使用插件
app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.config.globalProperties.$axios = axios

// 挂载应用
app.mount('#app')

// 初始化用户状态
const userStore = useUserStore()
if (userStore.token) {
  userStore.fetchCurrentUser().catch(error => {
    console.error('无法获取用户信息:', error)
  })
} 