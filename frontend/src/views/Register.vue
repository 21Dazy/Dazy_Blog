<template>
  <div class="register-container">
    <div class="register-box">
      <h2>用户注册</h2>
      
      <el-form 
        ref="registerFormRef" 
        :model="formData" 
        :rules="rules" 
        label-width="100px"
        @submit.prevent="handleRegister"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱"></el-input>
        </el-form-item>
        
        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="formData.password" 
            type="password" 
            placeholder="请输入密码"
            show-password
          ></el-input>
        </el-form-item>
        
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input 
            v-model="formData.confirmPassword" 
            type="password" 
            placeholder="请再次输入密码"
            show-password
          ></el-input>
        </el-form-item>
        
        <el-form-item>
          <el-checkbox v-model="formData.agreement" required>
            我已阅读并同意<a href="/terms" target="_blank">服务条款</a>和<a href="/privacy" target="_blank">隐私政策</a>
          </el-checkbox>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" class="register-button">
            注册
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="register-links">
        <span>已有账号？</span>
        <router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

export default {
  name: 'Register',
  setup() {
    const userStore = useUserStore()
    const router = useRouter()
    const registerFormRef = ref(null)
    const loading = computed(() => userStore.loading)
    
    const formData = ref({
      username: '',
      email: '',
      password: '',
      confirmPassword: '',
      agreement: false
    })
    
    const validatePass = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请输入密码'))
      } else {
        if (formData.value.confirmPassword !== '') {
          registerFormRef.value.validateField('confirmPassword')
        }
        callback()
      }
    }
    
    const validatePass2 = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请再次输入密码'))
      } else if (value !== formData.value.password) {
        callback(new Error('两次输入密码不一致'))
      } else {
        callback()
      }
    }
    
    const validateAgreement = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请阅读并同意服务条款和隐私政策'))
      } else {
        callback()
      }
    }
    
    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '用户名长度应为3-20个字符', trigger: 'blur' }
      ],
      email: [
        { required: true, message: '请输入邮箱地址', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '密码长度应为6-20个字符', trigger: 'blur' },
        { validator: validatePass, trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: '请再次输入密码', trigger: 'blur' },
        { validator: validatePass2, trigger: 'blur' }
      ],
      agreement: [
        { validator: validateAgreement, trigger: 'change' }
      ]
    }
    
    const handleRegister = async () => {
      try {
        await registerFormRef.value.validate()
        
        await userStore.register({
          username: formData.value.username,
          email: formData.value.email,
          password: formData.value.password
        })
        
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } catch (error) {
        console.error('注册失败:', error)
        ElMessage.error(error.response?.data?.message || '注册失败，请稍后再试')
      }
    }
    
    return {
      registerFormRef,
      formData,
      rules,
      loading,
      handleRegister
    }
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 80vh;
  background-color: #f5f7fa;
}

.register-box {
  width: 500px;
  padding: 30px;
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.register-box h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.register-button {
  width: 100%;
}

.register-links {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
}

.register-links a {
  color: #409EFF;
  text-decoration: none;
  margin-left: 5px;
}

.register-links a:hover {
  text-decoration: underline;
}
</style> 