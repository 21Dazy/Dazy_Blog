<template>
  <div class="category-container">
    <div class="category-header">
      <h2 v-if="category">{{ category.name }}</h2>
      <h2 v-else>分类</h2>
      <p v-if="category">{{ category.description }}</p>
      
      <!-- 排序控制 -->
      <div class="filter-bar">
        <div class="filter-group">
          <span class="filter-label">排序:</span>
          <el-select 
            v-model="sortBy" 
            size="small" 
            @change="handleSortChange" 
            class="sort-select"
          >
            <el-option label="最新发布" value="createdAt" />
            <el-option label="最多阅读" value="views" />
            <el-option label="最多点赞" value="likes" />
          </el-select>
          
          <!-- 添加升降序切换按钮 -->
          <el-tooltip
            :content="sortDirection === 'desc' ? '当前：降序排列' : '当前：升序排列'"
            placement="top"
          >
            <el-button
              class="direction-button"
              @click="toggleSortDirection"
              round
              type="primary"
              text
            >
              <div class="sort-direction-indicator">
                <transition name="flip">
                  <el-icon v-if="sortDirection === 'desc'" key="desc"><ArrowDown /></el-icon>
                  <el-icon v-else key="asc"><ArrowUp /></el-icon>
                </transition>
              </div>
            </el-button>
          </el-tooltip>
        </div>
      </div>
    </div>
    
    <el-skeleton :rows="3" animated v-if="loading" />
    
    <div v-else-if="blogs.length === 0" class="empty-blogs">
      <el-empty description="该分类下暂无博客" />
    </div>
    
    <div v-else class="blog-grid">
      <div class="blog-grid-item" v-for="blog in blogs" :key="blog.id">
        <blog-card :blog="blog" />
      </div>
      
      <div class="pagination-container" v-if="totalItems > pageSize">
        <el-pagination
          layout="prev, pager, next"
          :total="totalItems"
          :page-size="pageSize"
          :current-page="currentPage"
          @current-change="handlePageChange"
          background
          hide-on-single-page
        />
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useBlogStore } from '@/stores/blog'
import { useCategoryStore } from '@/stores/category'
import { useRoute, useRouter } from 'vue-router'
import BlogCard from '@/components/blog/BlogCard.vue'
import { ElMessage } from 'element-plus'
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue'

export default {
  name: 'Category',
  components: {
    BlogCard,
    ArrowDown,
    ArrowUp
  },
  setup() {
    const blogStore = useBlogStore()
    const categoryStore = useCategoryStore()
    const route = useRoute()
    const router = useRouter()
    const loading = ref(true)
    const category = ref(null)
    
    // 分页和排序状态
    const currentPage = ref(1)
    const pageSize = ref(10)
    const totalItems = ref(0)
    const totalPages = ref(1)
    const sortBy = ref('createdAt')
    const sortDirection = ref('desc')
    
    const blogs = computed(() => blogStore.blogs)
    
    // 初始化：从URL参数获取页码和排序
    const initFromUrl = () => {
      const query = route.query
      
      // 设置页码
      if (query.page) {
        currentPage.value = parseInt(query.page) || 1
      }
      
      // 设置排序
      if (query.sort) {
        sortBy.value = query.sort
      }
      
      if (query.direction) {
        sortDirection.value = query.direction
      }
      
      console.log('从URL初始化参数:', {
        page: currentPage.value,
        sort: sortBy.value,
        direction: sortDirection.value
      })
    }
    
    // 更新URL参数
    const updateUrlParams = () => {
      const query = {}
      
      // 只有当页码不是1时才添加页码参数
      if (currentPage.value > 1) {
        query.page = currentPage.value
      }
      
      // 只有当排序不是默认排序时才添加排序参数
      if (sortBy.value !== 'createdAt') {
        query.sort = sortBy.value
      }
      
      if (sortDirection.value !== 'desc') {
        query.direction = sortDirection.value
      }
      
      // 更新URL，不触发导航
      router.replace({ 
        params: route.params,
        query 
      }, { shallow: true }).catch(error => {
        console.error('更新URL参数失败:', error)
      })
    }
    
    // 加载分类数据
    const fetchCategoryData = async () => {
      const categoryId = route.params.id
      
      if (!categoryId) {
        ElMessage.error('分类ID无效')
        return
      }
      
      try {
        loading.value = true
        
        // 获取分类信息
        const categoryData = await categoryStore.fetchCategoryById(categoryId)
        category.value = categoryData
        
        // 获取该分类下的博客
        const result = await blogStore.fetchBlogsByCategory({
          categoryId: categoryId,
          page: currentPage.value,
          size: pageSize.value,
          sortBy: sortBy.value,
          order: sortDirection.value
        })
        
        // 更新分页状态
        totalItems.value = blogStore.totalItems || result?.totalItems || 0
        totalPages.value = blogStore.totalPages || result?.totalPages || Math.ceil(totalItems.value / pageSize.value) || 1
        
        console.log('分类博客加载完成，总数:', totalItems.value, '总页数:', totalPages.value)
        
        // 更新URL参数
        updateUrlParams()
      } catch (error) {
        console.error('获取分类数据失败:', error)
        ElMessage.error('获取分类数据失败')
      } finally {
        loading.value = false
      }
    }
    
    // 处理页码变化
    const handlePageChange = async (page) => {
      console.log('页码变化为:', page)
      currentPage.value = page
      
      // 更新URL参数
      updateUrlParams()
      
      // 加载新页的数据
      try {
        loading.value = true
        
        const result = await blogStore.fetchBlogsByCategory({
          categoryId: route.params.id,
          page: currentPage.value,
          size: pageSize.value,
          sortBy: sortBy.value,
          order: sortDirection.value
        })
        
        // 更新分页状态
        totalItems.value = blogStore.totalItems || result?.totalItems || 0
        totalPages.value = blogStore.totalPages || result?.totalPages || Math.ceil(totalItems.value / pageSize.value) || 1
      } catch (error) {
        console.error('获取博客列表失败:', error)
        ElMessage.error('获取博客列表失败')
      } finally {
        loading.value = false
      }
    }
    
    // 处理排序变化
    const handleSortChange = async () => {
      console.log('排序变化为:', sortBy.value)
      
      // 排序变化时回到第一页
      currentPage.value = 1
      
      // 更新URL参数
      updateUrlParams()
      
      // 加载排序后的数据
      try {
        loading.value = true
        
        const result = await blogStore.fetchBlogsByCategory({
          categoryId: route.params.id,
          page: currentPage.value,
          size: pageSize.value,
          sortBy: sortBy.value,
          order: sortDirection.value
        })
        
        // 更新分页状态
        totalItems.value = blogStore.totalItems || result?.totalItems || 0
        totalPages.value = blogStore.totalPages || result?.totalPages || Math.ceil(totalItems.value / pageSize.value) || 1
        
        ElMessage.success(`已按${getSortLabel(sortBy.value)}排序`)
      } catch (error) {
        console.error('排序博客失败:', error)
        ElMessage.error('排序博客失败')
      } finally {
        loading.value = false
      }
    }
    
    // 获取排序方式的显示标签
    const getSortLabel = (sortValue) => {
      switch (sortValue) {
        case 'createdAt': return '最新发布'
        case 'views': return '阅读量'
        case 'likes': return '点赞量'
        default: return sortValue
      }
    }
    
    // 切换排序方向
    const toggleSortDirection = async () => {
      sortDirection.value = sortDirection.value === 'desc' ? 'asc' : 'desc'
      
      // 排序变化时回到第一页
      currentPage.value = 1
      
      // 更新URL参数
      updateUrlParams()
      
      // 加载排序后的数据
      try {
        loading.value = true
        
        const result = await blogStore.fetchBlogsByCategory({
          categoryId: route.params.id,
          page: currentPage.value,
          size: pageSize.value,
          sortBy: sortBy.value,
          order: sortDirection.value
        })
        
        // 更新分页状态
        totalItems.value = blogStore.totalItems || result?.totalItems || 0
        totalPages.value = blogStore.totalPages || result?.totalPages || Math.ceil(totalItems.value / pageSize.value) || 1
        
        ElMessage.success(`已按${getSortLabel(sortBy.value)}排序`)
      } catch (error) {
        console.error('排序博客失败:', error)
        ElMessage.error('排序博客失败')
      } finally {
        loading.value = false
      }
    }
    
    // 监听路由参数变化
    watch(() => route.params.id, () => {
      // 重置页码和加载新分类
      currentPage.value = 1
      fetchCategoryData()
    })
    
    // 监听URL查询参数变化
    watch(() => route.query, (newQuery) => {
      if (newQuery.page && parseInt(newQuery.page) !== currentPage.value) {
        currentPage.value = parseInt(newQuery.page) || 1
      }
      
      if (newQuery.sort && newQuery.sort !== sortBy.value) {
        sortBy.value = newQuery.sort
      }
      
      if (newQuery.direction && newQuery.direction !== sortDirection.value) {
        sortDirection.value = newQuery.direction
      }
      
      // 只有当参数真正变化且分类ID存在时重新加载
      if (route.params.id) {
        fetchCategoryData()
      }
    }, { deep: true })
    
    onMounted(() => {
      // 从URL获取初始参数
      initFromUrl()
      
      // 加载分类数据
      fetchCategoryData()
    })
    
    return {
      category,
      blogs,
      loading,
      currentPage,
      pageSize,
      totalItems,
      totalPages,
      sortBy,
      sortDirection,
      handlePageChange,
      handleSortChange,
      getSortLabel,
      toggleSortDirection
    }
  }
}
</script>

<style scoped>
.category-container {
  padding: 20px;
}

.category-header {
  background-color: #fff;
  border-radius: 4px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.category-header h2 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #333;
}

.category-header p {
  color: #666;
  margin-bottom: 15px;
}

/* 筛选栏样式 */
.filter-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 15px;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-group {
  border: 1px solid #eaeaea;
  border-radius: 8px;
  padding: 6px 10px;
  display: flex;
  align-items: center;
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.03);
  transition: all 0.3s ease;
}

.filter-group:hover {
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.08);
  border-color: #dcdfe6;
}

.filter-label {
  margin-right: 10px;
  font-weight: 500;
  color: #606266;
  white-space: nowrap;
}

.sort-select {
  min-width: 110px;
}

/* 博客列表样式 */
.blog-list {
  margin-top: 20px;
}

.blog-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.blog-grid-item {
  display: flex;
  height: 100%;
}

.pagination-container {
  margin-top: 30px;
  text-align: center;
  grid-column: 1 / -1;
}

.empty-blogs {
  background-color: #fff;
  border-radius: 4px;
  padding: 40px 0;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

/* 博客网格布局的响应式调整 */
@media (min-width: 1441px) {
  .blog-grid {
    grid-template-columns: repeat(4, 1fr); /* 大屏幕显示4列 */
  }
}

@media (min-width: 1201px) and (max-width: 1440px) {
  .blog-grid {
    grid-template-columns: repeat(3, 1fr); /* 中大屏幕显示3列 */
  }
}

@media (min-width: 769px) and (max-width: 1200px) {
  .blog-grid {
    grid-template-columns: repeat(2, 1fr); /* 中等屏幕显示2列 */
  }
}

@media (max-width: 768px) {
  .blog-grid {
    grid-template-columns: 1fr; /* 小屏幕显示1列 */
    gap: 15px;
  }
  
  .filter-bar {
    justify-content: flex-start;
    margin-top: 20px;
  }
  
  .filter-group {
    width: 100%;
    justify-content: space-between;
  }
  
  .sort-select {
    min-width: 150px;
    flex-grow: 1;
  }
}

/* 排序方向按钮样式 */
.direction-button {
  margin-left: 6px;
  position: relative;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #eaeaea;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.direction-button:hover {
  background-color: #ecf5ff;
  border-color: #409EFF;
  color: #409EFF;
}

.sort-direction-indicator {
  position: relative;
  width: 24px;
  height: 24px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 排序方向切换的动画效果 */
.flip-enter-active,
.flip-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.flip-enter-from,
.flip-leave-to {
  transform: rotateX(90deg);
  opacity: 0;
}
</style> 