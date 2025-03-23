<template>
  <div class="login-container">
    <div class="login-box">
      <h2>用户登录</h2>
      
      <el-form
        ref="loginFormRef" 
        :model="formData" 
        :rules="rules" 
        label-width="80px"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        
        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="formData.password" 
            type="password" 
            placeholder="请输入密码"
            show-password
          ></el-input>
        </el-form-item>
        
        <el-form-item>
          <el-checkbox v-model="formData.remember">记住我</el-checkbox>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" class="login-button">
            登录
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-links">
        <router-link to="/register">没有账号？立即注册</router-link>
        <router-link to="/forgot-password">忘记密码？</router-link>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, nextTick } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

export default {
  name: 'Login',
  setup() {
    const userStore = useUserStore()
    const router = useRouter()
    const route = useRoute()
    const loginFormRef = ref(null)
    const loading = computed(() => userStore.loading)
    
    const formData = ref({
      username: '',
      password: '',
      remember: false
    })
    
    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '用户名长度应为3-20个字符', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '密码长度应为6-20个字符', trigger: 'blur' }
      ]
    }
    
    const handleLogin = async () => {
      if (!loginFormRef.value) return
      
      try {
        await loginFormRef.value.validate()
        
        await userStore.login({
          username: formData.value.username,
          password: formData.value.password,
          remember: formData.value.remember
        })
        
        ElMessage.success('登录成功')
        
        // 使用 nextTick 确保 DOM 更新完成后再进行路由跳转
        await nextTick()

        // 登录成功后获取最新的用户信息
        await userStore.fetchCurrentUser()
        
        // 如果有重定向，则跳转到重定向页面，否则跳转到首页
        const redirectPath = route.query.redirect || '/'
        await router.push(redirectPath)
      } catch (error) {
        console.error('登录失败:', error)
        ElMessage.error(error.response?.data?.message || '登录失败，请检查用户名和密码')
      }
    }
    
    return {
      loginFormRef,
      formData,
      rules,
      loading,
      handleLogin
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 80vh;
  background-color: #f5f7fa;
}

.login-box {
  width: 400px;
  padding: 30px;
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.login-box h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.login-button {
  width: 100%;
}

.login-links {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
  font-size: 14px;
}

.login-links a {
  color: #409EFF;
  text-decoration: none;
}

.login-links a:hover {
  text-decoration: underline;
}
</style> 