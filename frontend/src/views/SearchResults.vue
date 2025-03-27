<template>
  <div class="search-results-container">
    <div class="search-header">
      <h1>搜索结果: "{{ searchKeyword }}"</h1>
      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索文章、标签或作者..."
          prefix-icon="el-icon-search"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </div>
    </div>

    <!-- 搜索结果计数和排序 -->
    <div class="results-info">
      <div class="result-count" v-if="!loading">
        找到 <span class="highlight">{{ totalBlogs }}</span> 条结果
      </div>
      
      <div class="sort-control">
        <span class="sort-label">排序:</span>
        <el-select 
          v-model="sortBy" 
          size="small" 
          @change="handleSortChange" 
          class="sort-select"
        >
          <el-option label="相关度" value="relevance" />
          <el-option label="最新发布" value="createdAt" />
          <el-option label="最多阅读" value="views" />
        </el-select>
      </div>
    </div>

    <!-- 结果列表 -->
    <div class="results-list">
      <el-skeleton :rows="5" animated v-if="loading" />
      
      <div v-else-if="blogs.length === 0" class="empty-results">
        <el-empty description="没有找到与搜索词相关的内容" />
        <div class="search-tips">
          <h3>搜索建议:</h3>
          <ul>
            <li>请检查您的拼写</li>
            <li>尝试使用不同的关键词</li>
            <li>使用更通用的关键词</li>
            <li>减少关键词数量</li>
          </ul>
        </div>
      </div>
      
      <div v-else class="blog-list">
        <div v-for="blog in blogs" :key="blog.id" class="blog-item">
          <div class="blog-card">
            <div class="blog-image" v-if="blog.coverImage">
              <router-link :to="`/blog/${blog.id}`">
                <img :src="getImageUrl(blog.coverImage)" :alt="blog.title" />
              </router-link>
            </div>
            
            <div class="blog-content">
              <h2 class="blog-title">
                <router-link :to="`/blog/${blog.id}`">{{ blog.title }}</router-link>
              </h2>
              
              <div class="blog-meta">
                <span class="meta-item author" v-if="blog.author">
                  <i class="el-icon-user"></i>
                  {{ blog.author.username }}
                </span>
                <span class="meta-item date">
                  <i class="el-icon-date"></i>
                  {{ formatDate(blog.createdAt) }}
                </span>
                <span class="meta-item category" v-if="blog.category">
                  <i class="el-icon-folder"></i>
                  <router-link :to="`/category/${blog.category.id}`">
                    {{ blog.category.name }}
                  </router-link>
                </span>
                <span class="meta-item views">
                  <i class="el-icon-view"></i>
                  {{ blog.views || 0 }} 阅读
                </span>
              </div>
              
              <div class="blog-summary">{{ blog.summary }}</div>
              
              <div class="blog-tags" v-if="blog.tags && blog.tags.length > 0">
                <router-link 
                  v-for="tag in blog.tags" 
                  :key="tag.id" 
                  :to="`/tags/${tag.id}`"
                  class="tag-item"
                >
                  #{{ tag.name }}
                </router-link>
              </div>
              
              <div class="blog-actions">
                <router-link :to="`/blog/${blog.id}`" class="read-more">
                  阅读全文
                </router-link>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 分页器 -->
      <el-pagination
        v-if="totalBlogs > pageSize"
        layout="prev, pager, next"
        :total="totalBlogs"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handlePageChange"
        hide-on-single-page
        background
      />
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useBlogStore } from '../stores/blog'
import { ElMessage } from 'element-plus'

export default {
  name: 'SearchResults',
  setup() {
    const route = useRoute()
    const router = useRouter()
    const blogStore = useBlogStore()
    
    const searchKeyword = ref('')
    const currentPage = ref(1)
    const pageSize = ref(10)
    const sortBy = ref('relevance')
    const result = ref(null)
    
    const blogs = computed(() => blogStore.blogs)
    const totalBlogs = computed(() => blogStore.totalItems || result?.total || 0)
    const loading = computed(() => blogStore.loading)
    
    // 获取URL中的搜索关键词
    const updateSearchKeyword = () => {
      if (route.query.q) {
        searchKeyword.value = route.query.q
      }
      
      if (route.query.page) {
        currentPage.value = parseInt(route.query.page) || 1
      }
      
      if (route.query.sort) {
        sortBy.value = route.query.sort
      }
    }
    
    // 监听路由变化
    watch(() => route.query, () => {
      updateSearchKeyword()
      performSearch()
    })
    
    // 执行搜索
    const performSearch = async () => {
      if (!searchKeyword.value) return
      
      try {
        console.log('执行搜索，关键词:', searchKeyword.value)
        console.log('当前页:', currentPage.value)
        console.log('排序方式:', sortBy.value)
        
        // 确保参数格式匹配blog.js中的searchBlogs方法
        const result = await blogStore.searchBlogs(searchKeyword.value, {
          page: currentPage.value,
          size: pageSize.value,
          sortBy: sortBy.value,
          order: 'desc' // 默认降序排列
        })
        
        console.log('搜索完成，结果:', result)
        
        // 显示搜索结果统计
        if (blogs.value.length === 0) {
          console.log('未找到结果')
        } else {
          console.log(`找到 ${totalBlogs.value} 条结果`)
        }
      } catch (error) {
        console.error('搜索失败:', error)
        ElMessage.error('搜索失败，请稍后重试')
      }
    }
    
    // 处理搜索提交
    const handleSearch = () => {
      if (!searchKeyword.value.trim()) {
        ElMessage.warning('请输入搜索关键词')
        return
      }
      
      currentPage.value = 1
      
      // 更新URL参数并重新搜索
      router.push({
        path: '/search',
        query: {
          q: searchKeyword.value,
          page: 1,
          sort: sortBy.value
        }
      })
    }
    
    // 处理排序变化
    const handleSortChange = async () => {
      console.log('更改排序方式:', sortBy.value)
      
      // 如果选择的是"相关度"排序，需要向后端请求新的排序结果
      if (sortBy.value === 'relevance') {
        try {
          // 更新URL参数并重新执行搜索
          router.push({
            path: '/search',
            query: {
              q: searchKeyword.value,
              page: currentPage.value,
              sort: sortBy.value
            }
          })
          
          // 不需要调用performSearch，因为watch会自动触发
        } catch (error) {
          console.error('排序请求失败:', error)
          ElMessage.error('更改排序方式失败，请稍后重试')
        }
      } else {
        // 对于其他排序方式，可以在客户端进行排序
        try {
          // 应用客户端排序
          blogStore.blogsSortBy(sortBy.value)
          
          // 更新URL参数
          router.push({
            path: '/search',
            query: {
              q: searchKeyword.value,
              page: currentPage.value,
              sort: sortBy.value
            }
          })
          
          ElMessage.success(`已按${getSortLabel(sortBy.value)}排序`)
        } catch (error) {
          console.error('客户端排序失败:', error)
          ElMessage.error('排序失败，请稍后重试')
        }
      }
    }
    
    // 获取排序方式的显示标签
    const getSortLabel = (sortValue) => {
      switch (sortValue) {
        case 'createdAt': return '最新发布'
        case 'views': return '阅读量'
        case 'likes': return '点赞量'
        case 'relevance': return '相关度'
        default: return sortValue
      }
    }
    
    // 处理页码变化
    const handlePageChange = (page) => {
      currentPage.value = page
      
      // 更新URL参数
      router.push({
        path: '/search',
        query: {
          q: searchKeyword.value,
          page: page,
          sort: sortBy.value
        }
      })
    }
    
    // 格式化日期
    const formatDate = (dateArray) => {
      if (!dateArray) return '未知日期'
      
      try {
        const date = new Date(dateArray[0], dateArray[1] - 1, dateArray[2], dateArray[3], dateArray[4], dateArray[5])
        
        if (isNaN(date.getTime())) {
          return '无效日期'
        }
        
        return date.toLocaleDateString('zh-CN', {
          year: 'numeric',
          month: 'long',
          day: 'numeric'
        })
      } catch (error) {
        console.error('日期格式化错误:', error)
        return '未知日期'
      }
    }
    
    // 处理图片URL
    const getImageUrl = (url) => {
      if (!url) return ''
      
      if (url.startsWith('http')) {
        return url
      }
      
      if (!url.startsWith('/')) {
        url = '/' + url
      }
      
      if (url.startsWith('/uploads')) {
        url = '/api' + url
      }
      
      return `http://localhost:8080${url}`
    }
    
    onMounted(() => {
      updateSearchKeyword()
      performSearch()
    })
    
    return {
      searchKeyword,
      blogs,
      totalBlogs,
      loading,
      currentPage,
      pageSize,
      sortBy,
      handleSearch,
      handleSortChange,
      handlePageChange,
      formatDate,
      getImageUrl,
      getSortLabel
    }
  }
}
</script>

<style scoped>
.search-results-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.search-header {
  margin-bottom: 30px;
}

.search-header h1 {
  font-size: 24px;
  margin-bottom: 15px;
  color: #333;
}

.search-box {
  max-width: 600px;
}

.results-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.result-count {
  font-size: 16px;
  color: #606266;
}

.highlight {
  color: #409EFF;
  font-weight: bold;
}

.sort-control {
  display: flex;
  align-items: center;
}

.sort-label {
  margin-right: 10px;
  color: #606266;
}

.sort-select {
  width: 120px;
}

.results-list {
  margin-top: 20px;
}

.blog-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.blog-card {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  display: flex;
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
}

.blog-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px 0 rgba(0, 0, 0, 0.15);
}

.blog-image {
  width: 280px;
  flex-shrink: 0;
}

.blog-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.blog-content {
  flex-grow: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.blog-title {
  margin-top: 0;
  margin-bottom: 10px;
  font-size: 20px;
}

.blog-title a {
  color: #333;
  text-decoration: none;
}

.blog-title a:hover {
  color: #409EFF;
}

.blog-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 10px;
  font-size: 14px;
  color: #909399;
}

.meta-item {
  display: flex;
  align-items: center;
}

.meta-item i {
  margin-right: 5px;
}

.meta-item a {
  color: #909399;
  text-decoration: none;
}

.meta-item a:hover {
  color: #409EFF;
}

.blog-summary {
  margin-bottom: 15px;
  line-height: 1.6;
  color: #606266;
  flex-grow: 1;
}

.blog-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 15px;
}

.tag-item {
  color: #409EFF;
  text-decoration: none;
  font-size: 14px;
}

.tag-item:hover {
  text-decoration: underline;
}

.blog-actions {
  display: flex;
  justify-content: flex-end;
}

.read-more {
  display: inline-block;
  padding: 8px 15px;
  background-color: #409EFF;
  color: white;
  text-decoration: none;
  border-radius: 4px;
  font-size: 14px;
  transition: background-color 0.3s;
}

.read-more:hover {
  background-color: #66b1ff;
}

.empty-results {
  padding: 30px 0;
  text-align: center;
}

.search-tips {
  max-width: 500px;
  margin: 20px auto 0;
  text-align: left;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.search-tips h3 {
  font-size: 16px;
  margin-top: 0;
  margin-bottom: 10px;
  color: #333;
}

.search-tips ul {
  padding-left: 20px;
  margin: 0;
}

.search-tips li {
  margin-bottom: 5px;
  color: #606266;
}

.el-pagination {
  margin-top: 30px;
  text-align: center;
}

@media (max-width: 768px) {
  .blog-card {
    flex-direction: column;
  }
  
  .blog-image {
    width: 100%;
    height: 200px;
  }
  
  .results-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .blog-meta {
    font-size: 12px;
    gap: 10px;
  }
}
</style> 