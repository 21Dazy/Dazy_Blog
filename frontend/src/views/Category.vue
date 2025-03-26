<template>
  <div class="category-container">
    <div class="category-header">
      <h2 v-if="category">{{ category.name }}</h2>
      <h2 v-else>分类</h2>
      <p v-if="category">{{ category.description }}</p>
    </div>
    
    <el-skeleton :rows="3" animated v-if="loading" />
    
    <div v-else-if="blogs.length === 0" class="empty-blogs">
      <el-empty description="该分类下暂无博客" />
    </div>
    
    <div v-else class="blog-grid">
      <div class="blog-grid-item" v-for="blog in blogs" :key="blog.id">
        <blog-card :blog="blog" />
      </div>
      
      <el-pagination
        v-if="totalBlogs > 0"
        layout="prev, pager, next"
        :total="totalBlogs"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useBlogStore } from '@/stores/blog'
import { useCategoryStore } from '@/stores/category'
import { useRoute } from 'vue-router'
import BlogCard from '@/components/blog/BlogCard.vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'Category',
  components: {
    BlogCard
  },
  setup() {
    const blogStore = useBlogStore()
    const categoryStore = useCategoryStore()
    const route = useRoute()
    const loading = ref(true)
    const category = ref(null)
    const currentPage = ref(1)
    const pageSize = ref(10)
    
    const blogs = computed(() => blogStore.blogs)
    const totalBlogs = computed(() => blogStore.totalBlogs)
    
    const fetchCategoryData = async () => {
      const categoryId = route.params.id
      
      try {
        loading.value = true
        
        
        // 获取分类信息
        const categoryData = await categoryStore.fetchCategoryById(categoryId)
        category.value = categoryData
        
        // 获取该分类下的博客
        await blogStore.fetchBlogsByCategory({
          page: currentPage.value,
          size: pageSize.value,
          categoryId: categoryId
        })
      } catch (error) {
        console.error('获取分类数据失败:', error)
        ElMessage.error('获取分类数据失败')
      } finally {
        loading.value = false
      }
    }
    
    onMounted(fetchCategoryData)
    
    // 监听路由参数变化，重新获取数据
    watch(() => route.params.id, () => {
      currentPage.value = 1
      fetchCategoryData()
    })
    
    const handlePageChange = async (page) => {
      currentPage.value = page
      try {
        loading.value = true
        await blogStore.fetchBlogs({
          page: currentPage.value,
          size: pageSize.value,
          categoryId: route.params.id
        })
      } catch (error) {
        console.error('获取博客列表失败:', error)
        ElMessage.error('获取博客列表失败')
      } finally {
        loading.value = false
      }
    }
    
    return {
      category,
      blogs,
      totalBlogs,
      loading,
      currentPage,
      pageSize,
      handlePageChange
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
  margin: 0;
}

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

.el-pagination {
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
}
</style> 