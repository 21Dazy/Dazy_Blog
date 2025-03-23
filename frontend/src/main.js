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
  
  // 添加请求日志
  console.log('发送请求:', config.method.toUpperCase(), config.url)
  console.log('请求头:', config.headers)
  if (config.data) {
    console.log('请求数据:', config.data)
  }
  
  return config
}, error => {
  console.error('请求错误:', error)
  return Promise.reject(error)
})

// 添加响应拦截器
axios.interceptors.response.use(
  response => {
    console.log('响应成功:', response.status, response.config.url)
    return response
  },
  error => {
    console.error('响应错误:', error.config?.url || '未知URL')
    if (error.response) {
      console.error('错误状态:', error.response.status)
      console.error('错误数据:', error.response.data)
    } else if (error.request) {
      console.error('未收到响应，请求信息:', error.request)
    } else {
      console.error('请求配置有误:', error.message)
    }
    return Promise.reject(error)
  }
)

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