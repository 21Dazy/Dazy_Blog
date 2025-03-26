<template>
  <header class="header">
    <div class="header-container">
      <div class="logo">
        <router-link to="/">Dazy Blog</router-link>
      </div>
      <div class="nav-menu">
        <el-menu mode="horizontal" :router="true" :default-active="activeIndex">
          <el-menu-item index="/">首页</el-menu-item>
          <el-sub-menu index="categories">
            <template #title>分类</template>
            <el-menu-item v-for="category in categories" :key="category.id" :index="'/category/' + category.id">
              {{ category.name }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/about">关于</el-menu-item>
        </el-menu>
      </div>
      <div class="user-actions">
        <template v-if="isAuthenticated && currentUser">
          <el-dropdown>
            <span class="el-dropdown-link">
              <img v-if="currentUser.avatar" :src="getImageUrl(currentUser.avatar)" class="avatar" />
              <el-avatar v-else :size="30" class="avatar">{{ currentUser?.username?.charAt(0).toUpperCase() }}</el-avatar>
              {{ currentUser?.username || '用户' }}
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>
                  <router-link to="/profile">个人中心</router-link>
                </el-dropdown-item>
                <el-dropdown-item>
                  <router-link to="/write">写博客</router-link>
                </el-dropdown-item>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" size="small" @click="$router.push('/login')">登录</el-button>
          <el-button size="small" @click="$router.push('/register')">注册</el-button>
        </template>
      </div>
    </div>
  </header>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useCategoryStore } from '@/stores/category'
import { ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'Header',
  components: {
    ArrowDown
  },
  setup() {
    const router = useRouter()
    const userStore = useUserStore()
    const categoryStore = useCategoryStore()
    
    const activeIndex = ref('/')
    const categories = computed(() => categoryStore.allCategories)
    const isAuthenticated = computed(() => userStore.isAuthenticated)
    const currentUser = computed(() => userStore.currentUser)

    onMounted(async () => {
      try {
        await categoryStore.fetchCategories()
        
        // 页面加载时检查用户登录状态
        if (isAuthenticated.value && !currentUser.value) {
          await userStore.fetchCurrentUser()
        }
      } catch (error) {
        console.error('初始化失败:', error)
      }
    })

    // 监听身份验证状态变化
    watch(isAuthenticated, async (newValue) => {
      if (newValue && !currentUser.value) {
        try {
          await userStore.fetchCurrentUser()
        } catch (error) {
          console.error('获取用户信息失败:', error)
        }
      }
    })

    const handleLogout = async () => {
      try {
        userStore.logout()
        ElMessage.success('已退出登录')
        router.push('/login')
      } catch (error) {
        console.error('退出登录失败:', error)
      }
    }
    
    // 处理图片URL
    const getImageUrl = (url) => {
      if (!url) return ''
      
      // 如果URL已经是完整路径，直接返回
      if (url.startsWith('http')) {
        return url
      }
      
      // 检查url是否不以/开头，则添加/
      if (!url.startsWith('/')) {
        url = '/' + url
      }
      
      // 确保URL包含/api前缀
      if (url.startsWith('/uploads')) {
        url = '/api' + url
      }
      
      // 拼接完整URL
      return `http://localhost:8080${url}`
    }

    return {
      activeIndex,
      categories,
      isAuthenticated,
      currentUser,
      handleLogout,
      getImageUrl
    }
  }
}
</script>

<style scoped>
.header {
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.header-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
}

.logo {
  font-size: 24px;
  font-weight: bold;
}

.logo a {
  color: #333;
  text-decoration: none;
}

.nav-menu {
  flex: 1;
  margin: 0 20px;
}

.user-actions {
  display: flex;
  align-items: center;
}

.user-actions .el-button {
  margin-left: 10px;
}

.el-dropdown-link {
  cursor: pointer;
  color: #409EFF;
  display: flex;
  align-items: center;
  gap: 5px;
}

.avatar {
  margin-right: 5px;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  object-fit: cover;
}
</style> 