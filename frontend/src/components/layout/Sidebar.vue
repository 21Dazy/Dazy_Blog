<template>
  <div class="sidebar-container">
    <el-menu
      :default-active="activeIndex"
      class="sidebar-menu"
      :router="true"
      :collapse="isCollapse"
    >
      <el-menu-item index="/">
        <el-icon><House /></el-icon>
        <span>首页</span>
      </el-menu-item>
      
      <el-sub-menu index="categories">
        <template #title>
          <el-icon><Folder /></el-icon>
          <span>博客分类</span>
        </template>
        <el-menu-item v-for="category in categories" :key="category.id" :index="'/category/' + category.id">
          {{ category.name }}
        </el-menu-item>
      </el-sub-menu>
      
      <el-menu-item index="/tags">
        <el-icon><PriceTag /></el-icon>
        <span>标签云</span>
      </el-menu-item>
      
      <el-menu-item index="/archives">
        <el-icon><Calendar /></el-icon>
        <span>归档</span>
      </el-menu-item>
      
      <el-menu-item v-if="isAuthenticated" index="/write">
        <el-icon><Edit /></el-icon>
        <span>写博客</span>
      </el-menu-item>
      
      <el-menu-item v-if="isAuthenticated" index="/profile">
        <el-icon><User /></el-icon>
        <span>个人中心</span>
      </el-menu-item>
      
      <el-menu-item index="/about">
        <el-icon><InfoFilled /></el-icon>
        <span>关于</span>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useCategoryStore } from '@/stores/category'
import {
  House,
  Folder,
  PriceTag,
  Calendar,
  Edit,
  User,
  InfoFilled
} from '@element-plus/icons-vue'

export default {
  name: 'Sidebar',
  components: {
    House,
    Folder,
    PriceTag,
    Calendar,
    Edit,
    User,
    InfoFilled
  },
  setup() {
    const route = useRoute()
    const userStore = useUserStore()
    const categoryStore = useCategoryStore()
    
    const isCollapse = ref(false)
    const categories = computed(() => categoryStore.allCategories)
    const activeIndex = computed(() => route.path)
    const isAuthenticated = computed(() => userStore.isAuthenticated)
    
    onMounted(async () => {
      try {
        await categoryStore.fetchCategories()
      } catch (error) {
        console.error('获取分类失败:', error)
      }
    })
    
    return {
      isCollapse,
      categories,
      activeIndex,
      isAuthenticated
    }
  }
}
</script>

<style scoped>
.sidebar-container {
  height: 100%;
}

.sidebar-menu {
  height: 100%;
  border-right: none;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 200px;
}
</style> 