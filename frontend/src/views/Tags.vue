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
        <h2>
          标签: <span class="highlight">{{ selectedTagName }}</span>
        </h2>
        
        <!-- 添加排序控制 -->
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
            <el-button
              :icon="sortDirection === 'desc' ? 'ArrowDown' : 'ArrowUp'"
              size="small"
              class="direction-button"
              @click="toggleSortDirection"
              circle
            />
          </div>
        </div>
      </div>
      
      <el-skeleton :rows="3" animated v-if="blogsLoading" />
      
      <div v-else-if="blogs.length === 0" class="empty-blogs">
        <el-empty description="该标签下暂无博客" />
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
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useBlogStore } from '@/stores/blog'
import { useTagStore } from '@/stores/tag'
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
    const tagStore = useTagStore()
    const route = useRoute()
    const router = useRouter()
    const loading = ref(true)
    const blogsLoading = computed(() => blogStore.loading)
    const tags = computed(() => tagStore.tags)
    const blogs = computed(() => blogStore.blogs)
    
    // 分页和排序状态
    const totalItems = ref(0)
    const totalPages = ref(1)
    const currentPage = ref(1)
    const pageSize = ref(10)
    const sortBy = ref('createdAt')
    const sortDirection = ref('desc')
    
    const selectedTagId = computed(() => route.params.id)
    const selectedTagName = computed(() => {
      const tag = tags.value.find(t => t.id == selectedTagId.value)
      return tag ? tag.name : ''
    })
    
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
      }).catch(error => {
        console.error('更新URL参数失败:', error)
      })
    }
    
    onMounted(async () => {
      try {
        console.log('Tags页面：开始加载标签数据...')
        await tagStore.fetchTags()
        console.log('Tags页面：标签数据加载完成', tags.value)
        
        // 从URL获取初始参数
        initFromUrl()
        
        // 如果URL中有标签ID，则获取该标签下的博客
        if (selectedTagId.value) {
          await fetchTagBlogs()
        }
      } catch (error) {
        console.error('Tags页面：加载标签失败', error)
        ElMessage.error('加载标签失败，请稍后重试')
      } finally {
        loading.value = false
      }
    })
    
    // 监听路由参数变化
    watch(() => route.params.id, () => {
      if (selectedTagId.value) {
        currentPage.value = 1
        fetchTagBlogs()
      } else {
        totalItems.value = 0
        totalPages.value = 1
      }
    })
    
    // 监听URL查询参数变化
    watch(() => route.query, (newQuery) => {
      let shouldRefetch = false
      
      if (newQuery.page && parseInt(newQuery.page) !== currentPage.value) {
        currentPage.value = parseInt(newQuery.page) || 1
        shouldRefetch = true
      }
      
      if (newQuery.sort && newQuery.sort !== sortBy.value) {
        sortBy.value = newQuery.sort
        shouldRefetch = true
      }
      
      if (newQuery.direction && newQuery.direction !== sortDirection.value) {
        sortDirection.value = newQuery.direction
        shouldRefetch = true
      }
      
      // 只有当参数真正变化且标签ID存在时重新加载
      if (shouldRefetch && selectedTagId.value) {
        fetchTagBlogs()
      }
    }, { deep: true })
    
    const fetchTagBlogs = async () => {
      if (!selectedTagId.value) return;
      
      try {
        const result = await blogStore.fetchBlogsByTag({
          tagId: selectedTagId.value,
          page: currentPage.value,
          size: pageSize.value,
          sortBy: sortBy.value,
          order: sortDirection.value
        })
        
        // 更新分页状态
        totalItems.value = blogStore.totalItems || result?.totalItems || 0
        totalPages.value = blogStore.totalPages || result?.totalPages || Math.ceil(totalItems.value / pageSize.value) || 1
        
        console.log('标签博客加载完成，总数:', totalItems.value, '总页数:', totalPages.value)
        
        // 更新URL参数
        updateUrlParams()
      } catch (error) {
        console.error('获取标签博客失败:', error)
        ElMessage.error('获取标签博客失败')
      }
    }
    
    const handlePageChange = (page) => {
      console.log('页码变化为:', page)
      currentPage.value = page
      
      // 更新URL参数
      updateUrlParams()
      
      // 加载新页的数据
      fetchTagBlogs()
    }
    
    // 处理排序变化
    const handleSortChange = () => {
      console.log('排序变化为:', sortBy.value)
      
      // 排序变化时回到第一页
      currentPage.value = 1
      
      // 更新URL参数
      updateUrlParams()
      
      // 加载排序后的数据
      fetchTagBlogs()
      
      ElMessage.success(`已按${getSortLabel(sortBy.value)}排序`)
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
    
    const toggleSortDirection = async () => {
      sortDirection.value = sortDirection.value === 'desc' ? 'asc' : 'desc'
      currentPage.value = 1
      updateUrlParams()
      await fetchTagBlogs()
    }
    
    return {
      tags,
      blogs,
      totalItems,
      totalPages,
      currentPage,
      pageSize,
      sortBy,
      sortDirection,
      selectedTagId,
      selectedTagName,
      loading,
      blogsLoading,
      handlePageChange,
      handleSortChange,
      navigateToTag,
      getRandomType,
      getRandomEffect,
      getRandomSize,
      toggleSortDirection
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
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
}

.tag-blogs-header h2 {
  margin: 0;
  color: #333;
}

.highlight {
  color: #409EFF;
}

/* 筛选栏样式 */
.filter-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 0;
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

.direction-button {
  margin-left: 10px;
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

.pagination-container {
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
  
  .tag-blogs-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .tag-blogs-header h2 {
    margin-bottom: 15px;
  }
  
  .filter-bar {
    width: 100%;
    justify-content: flex-start;
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
</style> 