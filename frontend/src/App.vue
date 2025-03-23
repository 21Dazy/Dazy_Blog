<template>
  <div class="app-container">
    <el-container>
      <el-header>
        <Header />
      </el-header>
      <el-container>
        <el-main>
          <router-view />
        </el-main>
      </el-container>
      <el-footer>
        <Footer />
      </el-footer>
    </el-container>
  </div>
</template>

<script>
import Header from '@/components/layout/Header.vue'
import Footer from '@/components/layout/Footer.vue'
import { useUserStore } from '@/stores/user'
import { onMounted } from 'vue'

export default {
  name: 'App',
  components: {
    Header,
    Footer
  },
  setup() {
    const userStore = useUserStore()
    
    onMounted(async () => {
      if (userStore.token && !userStore.user) {
        try {
          await userStore.fetchCurrentUser()
        } catch (error) {
          console.error('获取用户信息失败:', error)
        }
      }
    })
    
    return {}
  }
}
</script>

<style>
body {
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.el-header {
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 0;
  height: 60px;
}

.el-main {
  flex: 1;
  background-color: #f5f7fa;
  padding: 20px;
}

.el-footer {
  background-color: #fff;
  border-top: 1px solid #eee;
  padding: 20px 0;
}
</style> 