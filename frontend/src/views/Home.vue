<template>
  <div class="home-container">
    <el-row :gutter="20">
      <el-col :span="18">
        <div class="blog-list-container">
          <h2>最新博客</h2>
          
          <el-skeleton :rows="3" animated v-if="loading" />
          
          <div v-else-if="blogs && blogs.length === 0" class="empty-blogs">
            <el-empty description="暂无博客" />
          </div>
          
          <div v-else class="blog-list">
            <blog-card 
              v-for="blog in blogs" 
              :key="blog.id" 
              :blog="blog"
            />
            
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
      </el-col>
      
      <el-col :span="6">
        <div class="sidebar-widgets">
          <div class="widget">
            <h3>热门分类</h3>
            <el-skeleton :rows="3" animated v-if="categoriesLoading" />
            <ul v-else-if="categories && categories.length > 0" class="category-list">
              <li v-for="category in categories" :key="category.id">
                <router-link :to="'/category/' + category.id">
                  {{ category.name }}
                </router-link>
              </li>
            </ul>
            <div v-else class="empty-categories">
              <el-empty description="暂无分类" />
            </div>
          </div>
          
          <div class="widget">
            <h3>最新文章</h3>
            <el-skeleton :rows="3" animated v-if="loading" />
            <ul v-else-if="recentBlogs && recentBlogs.length > 0" class="recent-posts">
              <li v-for="blog in recentBlogs" :key="blog.id">
                <router-link :to="'/blog/' + blog.id">
                  {{ blog.title }}
                </router-link>
              </li>
            </ul>
            <div v-else class="empty-recent">
              <el-empty description="暂无最新文章" />
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useBlogStore } from '../stores/blog'
import { useCategoryStore } from '../stores/category'
import BlogCard from '../components/blog/BlogCard.vue'

export default {
  name: 'Home',
  components: {
    BlogCard
  },
  setup() {
    const blogStore = useBlogStore()
    const categoryStore = useCategoryStore()
    const currentPage = ref(1)
    const pageSize = ref(10)
    
    const blogs = computed(() => blogStore.allBlogs)
    const totalBlogs = computed(() => blogStore.totalBlogs || 0)
    const loading = computed(() => blogStore.loading || categoryStore.loading)
    const categories = computed(() => categoryStore.allCategories)
    const categoriesLoading = computed(() => categoryStore.loading)
    
    const recentBlogs = computed(() => blogStore.recentBlogs)
    
    onMounted(async () => {
      try {
        await Promise.all([
          blogStore.fetchBlogs(),
          categoryStore.fetchCategories()
        ])
      } catch (error) {
        console.error('加载数据失败:', error)
      }
    })
    
    const handlePageChange = async (page) => {
      currentPage.value = page
      try {
        await blogStore.fetchBlogs()
      } catch (error) {
        console.error('加载数据失败:', error)
      }
    }
    
    return {
      blogs,
      totalBlogs,
      loading,
      categories,
      categoriesLoading,
      recentBlogs,
      currentPage,
      pageSize,
      handlePageChange
    }
  }
}
</script>

<style scoped>
.home-container {
  padding: 20px;
}

.blog-list-container {
  background-color: #fff;
  border-radius: 4px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.blog-list {
  margin-top: 20px;
}

.empty-blogs, .empty-categories, .empty-recent {
  padding: 20px 0;
}

.sidebar-widgets {
  margin-bottom: 20px;
}

.widget {
  background-color: #fff;
  border-radius: 4px;
  padding: 15px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.widget h3 {
  margin-top: 0;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.category-list, .recent-posts {
  list-style: none;
  padding: 0;
  margin: 0;
}

.category-list li, .recent-posts li {
  padding: 8px 0;
  border-bottom: 1px dashed #eee;
}

.category-list li:last-child, .recent-posts li:last-child {
  border-bottom: none;
}

.category-list a, .recent-posts a {
  color: #333;
  text-decoration: none;
}

.category-list a:hover, .recent-posts a:hover {
  color: #409EFF;
}

.el-pagination {
  margin-top: 20px;
  text-align: center;
}
</style> 