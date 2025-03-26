<template>
  <div class="tags-container">
    <div class="tags-header">
      <h2>标签云</h2>
      <p>探索不同标签下的博客内容</p>
    </div>
    
    <el-skeleton :rows="3" animated v-if="loading" />
    
    <div v-else-if="tags.length === 0" class="empty-tags">
      <el-empty description="暂无标签" />
    </div>
    
    <div v-else class="tag-cloud">
      <el-tag
        v-for="tag in tags"
        :key="tag.id"
        :type="getRandomType()"
        :effect="getRandomEffect()"
        :size="getRandomSize(tag)"
        @click="navigateToTag(tag.id)"
        class="tag-item"
      >
        {{ tag.name }} ({{ tag.count || 0 }})
      </el-tag>
    </div>
    
    <div v-if="selectedTagId" class="tag-blogs">
      <div class="tag-blogs-header">
        <h3>{{ selectedTagName }} 相关博客</h3>
      </div>
      
      <el-skeleton :rows="3" animated v-if="blogsLoading" />
      
      <div v-else-if="blogs.length === 0" class="empty-blogs">
        <el-empty description="该标签下暂无博客" />
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
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useBlogStore } from '@/stores/blog'
import { useRoute, useRouter } from 'vue-router'
import BlogCard from '@/components/blog/BlogCard.vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'Tags',
  components: {
    BlogCard
  },
  setup() {
    const blogStore = useBlogStore()
    const route = useRoute()
    const router = useRouter()
    const loading = ref(true)
    const blogsLoading = computed(() => blogStore.loading)
    const tags = ref([])
    const blogs = computed(() => blogStore.blogs)
    const totalBlogs = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(10)
    
    const selectedTagId = computed(() => route.params.id)
    const selectedTagName = computed(() => {
      const tag = tags.value.find(t => t.id === selectedTagId.value)
      return tag ? tag.name : ''
    })
    
    onMounted(async () => {
      try {
        // 获取所有标签
        await fetchTags()
        
        // 如果URL中有标签ID，则获取该标签下的博客
        if (selectedTagId.value) {
          await fetchTagBlogs()
        }
      } catch (error) {
        console.error('获取标签数据失败:', error)
        ElMessage.error('获取标签数据失败')
      } finally {
        loading.value = false
      }
    })
    
    // 监听路由参数变化，重新获取数据
    watch(() => route.params.id, () => {
      if (selectedTagId.value) {
        currentPage.value = 1
        fetchTagBlogs()
      } else {
        totalBlogs.value = 0
      }
    })
    
    const fetchTags = async () => {
      try {
        // 假设有一个获取所有标签的API
        // const response = await axios.get('/api/tags')
        // tags.value = response.data
        
        // 模拟数据
        tags.value = [
          { id: '1', name: 'JavaScript', count: 15 },
          { id: '2', name: 'Vue', count: 10 },
          { id: '3', name: 'React', count: 8 },
          { id: '4', name: 'Angular', count: 5 },
          { id: '5', name: 'Node.js', count: 12 },
          { id: '6', name: 'TypeScript', count: 7 },
          { id: '7', name: 'CSS', count: 9 },
          { id: '8', name: 'HTML', count: 6 },
          { id: '9', name: 'Spring Boot', count: 11 },
          { id: '10', name: 'Java', count: 14 },
          { id: '11', name: 'Python', count: 13 },
          { id: '12', name: 'Docker', count: 4 }
        ]
      } catch (error) {
        console.error('获取标签失败:', error)
        ElMessage.error('获取标签失败')
      }
    }
    
    const fetchTagBlogs = async () => {
      try {
        // 获取标签下的博客
        const response = await blogStore.fetchBlogsByTag({
          tagId: selectedTagId.value,
          page: currentPage.value,
          size: pageSize.value
        })
        
        totalBlogs.value = response.totalElements
      } catch (error) {
        console.error('获取标签博客失败:', error)
        ElMessage.error('获取标签博客失败')
      }
    }
    
    const handlePageChange = (page) => {
      currentPage.value = page
      fetchTagBlogs()
    }
    
    const navigateToTag = (tagId) => {
      router.push(`/tags/${tagId}`)
    }
    
    // 随机生成标签的样式，使标签云更加生动
    const getRandomType = () => {
      const types = ['', 'success', 'info', 'warning', 'danger']
      return types[Math.floor(Math.random() * types.length)]
    }
    
    const getRandomEffect = () => {
      const effects = ['light', 'dark', 'plain']
      return effects[Math.floor(Math.random() * effects.length)]
    }
    
    const getRandomSize = (tag) => {
      // 根据博客数量决定标签大小
      const count = tag.count || 0
      if (count > 10) return 'large'
      if (count > 5) return 'default'
      return 'small'
    }
    
    return {
      tags,
      blogs,
      totalBlogs,
      loading,
      blogsLoading,
      currentPage,
      pageSize,
      selectedTagId,
      selectedTagName,
      handlePageChange,
      navigateToTag,
      getRandomType,
      getRandomEffect,
      getRandomSize
    }
  }
}
</script>

<style scoped>
.tags-container {
  padding: 20px;
}

.tags-header {
  background-color: #fff;
  border-radius: 4px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  text-align: center;
}

.tags-header h2 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #333;
}

.tags-header p {
  color: #666;
  margin: 0;
}

.tag-cloud {
  background-color: #fff;
  border-radius: 4px;
  padding: 30px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  text-align: center;
}

.tag-item {
  margin: 10px;
  cursor: pointer;
  transition: transform 0.3s;
}

.tag-item:hover {
  transform: scale(1.1);
}

.empty-tags {
  background-color: #fff;
  border-radius: 4px;
  padding: 40px 0;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.tag-blogs {
  margin-top: 30px;
}

.tag-blogs-header {
  background-color: #fff;
  border-radius: 4px;
  padding: 15px 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.tag-blogs-header h3 {
  margin: 0;
  color: #333;
}

.empty-blogs {
  background-color: #fff;
  border-radius: 4px;
  padding: 40px 0;
  margin-top: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
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