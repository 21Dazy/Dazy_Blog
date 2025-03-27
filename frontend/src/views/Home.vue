<template>
  <div class="home-container">
    <!-- 顶部横幅 -->
    <div class="hero-banner">
      <div class="banner-content">
        <h1 class="banner-title">探索博客世界</h1>
        <p class="banner-description">分享知识，连接思想，发现精彩</p>
        
        <!-- 搜索框 -->
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
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <el-row :gutter="20">
        <!-- 左侧内容区 -->
        <el-col :xs="24" :sm="24" :md="16" :lg="18">
          <!-- 筛选与排序 -->
          <div class="filter-bar">
            <div class="filter-group">
              <span class="filter-label">排序:</span>
              <el-select 
                v-model="sortBy" 
                size="large" 
                @change="handleSortChange" 
                class="sort-select" 
                popper-class="sort-select-dropdown"
                placeholder="选择排序方式"
              >
                <el-option label="最新发布" value="createdAt">
                  <div class="sort-option">
                    <el-icon><Calendar /></el-icon>
                    <span>最新发布</span>
                  </div>
                </el-option>
                <el-option label="最多阅读" value="views">
                  <div class="sort-option">
                    <el-icon><View /></el-icon>
                    <span>最多阅读</span>
                  </div>
                </el-option>
                <el-option label="最多点赞" value="likes">
                  <div class="sort-option">
                    <el-icon><Star /></el-icon>
                    <span>最多点赞</span>
                  </div>
                </el-option>
              </el-select>
            </div>
            
            <div v-if="currentCategory || currentTag || searchKeyword" class="active-filters">
              <el-tag v-if="currentCategory" closable @close="clearCategoryFilter">
                分类: {{ currentCategory.name }}
              </el-tag>
              <el-tag v-if="currentTag" type="success" closable @close="clearTagFilter">
                标签: {{ currentTag.name }}
              </el-tag>
              <el-tag v-if="searchKeyword" type="info" closable @close="clearSearchFilter">
                搜索: {{ searchKeyword }}
              </el-tag>
            </div>
          </div>

          <!-- 博客列表 -->
          <div class="blog-list-container">
            <h2>{{ getListTitle() }}</h2>
            
            <el-skeleton :rows="3" animated v-if="loading" />
            
            <div v-else-if="blogs.length === 0" class="empty-blogs">
              <el-empty description="暂无博客" />
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
        </el-col>
        
        <!-- 右侧边栏 -->
        <el-col :xs="24" :sm="24" :md="8" :lg="6">
          <div class="sidebar-widgets">
            <!-- 作者信息 -->
            <div class="widget author-info" v-if="currentUser">
              <div class="author-avatar">
                <img :src="currentUser.avatar ? getImageUrl(currentUser.avatar) : 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" alt="头像" />
              </div>
              <h3 class="author-name">{{ currentUser.username }}</h3>
              <p class="author-bio">{{ currentUser.bio || '这个人很懒，什么都没有留下' }}</p>
              <div class="author-stats">
                <div class="stat">
                  <div class="stat-number">{{ blogs.length }}</div>
                  <div class="stat-label">文章</div>
                </div>
                <div class="stat">
                  <div class="stat-number">{{ getTotalViews() }}</div>
                  <div class="stat-label">阅读</div>
                </div>
              </div>
            </div>
            
            <!-- 分类列表 -->
            <div class="widget">
              <h3>博客分类</h3>
              <el-skeleton :rows="3" animated v-if="categoriesLoading" />
              <ul v-else-if="categories && categories.length > 0" class="category-list">
                <li v-for="category in categories" :key="category.id">
                  <router-link :to="'/category/' + category.id">
                    {{ category.name }}
                    <span class="category-count">{{}}</span>
                  </router-link>
                </li>
              </ul>
              <div v-else class="empty-categories">
                <el-empty description="暂无分类" />
              </div>
            </div>
            
            <!-- 标签云 -->
            <div class="widget">
              <h3>热门标签</h3>
              <el-skeleton :rows="2" animated v-if="tagsLoading" />
              <div v-else-if="tags && tags.length > 0" class="tag-cloud">
                <router-link 
                  v-for="tag in tags.slice(0, 20)" 
                  :key="tag.id" 
                  :to="'/tags/' + tag.id"
                  class="tag-item"
                  :style="getTagStyle(tag)"
                >
                  {{ tag.name || '未命名标签' }}
                  <small v-if="tag.count">({{ tag.count }})</small>
                </router-link>
              </div>
              <div v-else class="empty-tags">
                <el-empty description="暂无标签" />
              </div>
            </div>
            
            <!-- 最新文章 -->
            <div class="widget">
              <h3>最新文章</h3>
              <el-skeleton :rows="3" animated v-if="loading" />
              <ul v-else-if="recentBlogs && recentBlogs.length > 0" class="recent-posts">
                <li v-for="blog in recentBlogs" :key="blog.id">
                  <router-link :to="'/blog/' + blog.id">
                    {{ blog.title }}
                  </router-link>
                  <span class="post-date">{{ formatDate(blog.createdAt) }}</span>
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
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useBlogStore } from '../stores/blog'
import { useCategoryStore } from '../stores/category'
import { useTagStore } from '../stores/tag'
import { useUserStore } from '../stores/user'
import { useRoute, useRouter } from 'vue-router'
import BlogCard from '../components/blog/BlogCard.vue'
import { ElMessage } from 'element-plus'
import { Calendar, View, Star } from '@element-plus/icons-vue'

export default {
  name: 'Home',
  components: {
    BlogCard
  },
  setup() {
    const blogStore = useBlogStore()
    const categoryStore = useCategoryStore()
    const tagStore = useTagStore()
    const userStore = useUserStore()
    const route = useRoute()
    const router = useRouter()
    
    const currentPage = ref(1)
    const pageSize = ref(10)
    const searchKeyword = ref('')
    const sortBy = ref('createdAt')
    const currentCategory = ref(null)
    const currentTag = ref(null)
    const tagsLoading = computed(() => tagStore.loading)
    
    const blogs = computed(() => blogStore.blogs)
    const totalBlogs = computed(() => blogStore.totalBlogs || 0)
    const loading = computed(() => blogStore.loading)
    const categories = computed(() => categoryStore.categories)
    const categoriesLoading = computed(() => categoryStore.loading)
    const recentBlogs = computed(() => blogStore.recentBlogs)
    const currentUser = computed(() => userStore.currentUser)
    const tags = computed(() => tagStore.tags)
    
    // 监听URL参数变化
    watch(() => route.query, (newQuery) => {
      if (newQuery.search) {
        searchKeyword.value = newQuery.search
      }
      
      if (newQuery.category) {
        fetchCategoryInfo(newQuery.category)
        blogStore.setCategory && blogStore.setCategory(newQuery.category)
      }
      
      if (newQuery.tag) {
        fetchTagInfo(newQuery.tag)
        blogStore.setTag && blogStore.setTag(newQuery.tag)
      }
      
      if (newQuery.page) {
        currentPage.value = parseInt(newQuery.page) || 1
        blogStore.goToPage && blogStore.goToPage(currentPage.value)
      }
      
      if (newQuery.sort) {
        sortBy.value = newQuery.sort
        blogStore.setSorting(newQuery.sort)
      }
    }, { immediate: true })
    
    
    onMounted(async () => {
      try {
        // 获取分类、标签和博客列表
        await Promise.all([
          categoryStore.fetchCategories(),
          tagStore.fetchTags()
        ])
        
        // 检查URL是否有搜索参数
        if (route.query.search) {
          searchKeyword.value = route.query.search
          await handleSearch()
        } else if (route.query.category) {
          // 处理分类筛选
          fetchCategoryInfo(route.query.category)
          await blogStore.fetchBlogsByCategory({
            categoryId: route.query.category,
            page: currentPage.value,
            size: pageSize.value,
            sortBy: sortBy.value
          })
        } else if (route.query.tag) {
          // 处理标签筛选
          fetchTagInfo(route.query.tag)
          await blogStore.fetchBlogsByTag({
            tagId: route.query.tag,
            page: currentPage.value,
            size: pageSize.value,
            sortBy: sortBy.value
          })
        } else {
          // 没有筛选条件，获取全部博客
          await fetchBlogs()
        }
        
        // 打印标签数据，确认加载成功
        console.log('标签数据加载情况:', {
          tagsInStore: tagStore.tags,
          tagsComputed: tags.value,
          tagsCount: tags.value.length
        })
        
        // 确保在数据加载后应用排序
        console.log("初始化排序:", sortBy.value);
        let sortOption = "";
        if(sortBy.value === "createdAt" || sortBy.value === "最新发布"){
          sortOption = "最新发布";
        }
        else if(sortBy.value === "views" || sortBy.value === "最多阅读"){
          sortOption = "最多阅读";
        }
        else if(sortBy.value === "likes" || sortBy.value === "最多点赞"){
          sortOption = "最多点赞";
        }
        
        if(sortOption) {
          blogStore.blogsSortBy(sortOption);
        }
      } catch (error) {
        console.error('加载数据失败:', error)
        ElMessage.error('加载数据失败，请稍后重试')
      }
    })
    
    // 获取分类信息
    const fetchCategoryInfo = async (categoryId) => {
      if (!categoryId) {
        currentCategory.value = null
        return
      }
      
      try {
        const category = categories.value.find(c => c.id == categoryId)
        if (category) {
          currentCategory.value = category
        } else {
          const response = await categoryStore.fetchCategoryById(categoryId)
          currentCategory.value = response
        }
      } catch (error) {
        console.error('获取分类信息失败:', error)
        currentCategory.value = null
      }
    }
    
    // 获取标签信息
    const fetchTagInfo = async (tagId) => {
      if (!tagId) {
        currentTag.value = null
        return
      }
      
      try {
        const tag = tags.value.find(t => t.id == tagId)
        if (tag) {
          currentTag.value = tag
        } else {
          const tagData = await tagStore.fetchTagById(tagId)
          currentTag.value = tagData
        }
      } catch (error) {
        console.error('获取标签信息失败:', error)
        currentTag.value = null
      }
    }
    
    // 获取博客列表
    const fetchBlogs = async () => {
      try {
        await blogStore.fetchBlogs({
          page: currentPage.value - 1,
          size: pageSize.value,
          sortBy: sortBy.value
        })
      } catch (error) {
        console.error('获取博客列表失败:', error)
      }
    }
    
    // 处理搜索
    const handleSearch = async () => {
      if (!searchKeyword.value.trim()) {
        ElMessage.warning('请输入搜索关键词')
        return
      }
      
      // 跳转到搜索结果页面
      router.push({
        path: '/search',
        query: {
          q: searchKeyword.value
        }
      })
    }
    
    // 处理排序变化
    const handleSortChange = () => {
      // 设置回第一页
      currentPage.value = 1
      
      console.log("排序变化:", sortBy.value);
      
      // 不再进行路由跳转，直接应用排序
      // 使用更新后的 blogsSortBy 方法进行客户端排序
      let sortOption = "";
      if (sortBy.value === "createdAt" || sortBy.value === "最新发布") {
        sortOption = "最新发布";
      } else if (sortBy.value === "views" || sortBy.value === "最多阅读") {
        sortOption = "最多阅读";
      } else if (sortBy.value === "likes" || sortBy.value === "最多点赞") {
        sortOption = "最多点赞";
      }
      
      // 总是应用排序，即使是同一个排序选项
      if(sortOption) {
        blogStore.blogsSortBy(sortOption);
      }
      
      // 仍然更新后端排序状态，以便将来的数据请求使用正确的排序
      blogStore.setSorting(sortBy.value);
    }
    
    // 处理分页变化
    const handlePageChange = (page) => {
      currentPage.value = page
      
      // 更新URL参数
      const query = { ...route.query, page }
      router.push({ path: route.path, query })
      
      blogStore.goToPage(page)
    }
    
    // 清除分类筛选
    const clearCategoryFilter = () => {
      currentCategory.value = null
      
      // 更新URL参数
      const query = { ...route.query }
      delete query.category
      router.push({ path: route.path, query })
      
      blogStore.setCategory(null)
    }
    
    // 清除标签筛选
    const clearTagFilter = () => {
      currentTag.value = null
      
      // 更新URL参数
      const query = { ...route.query }
      delete query.tag
      router.push({ path: route.path, query })
      
      blogStore.setTag(null)
    }
    
    // 清除搜索筛选
    const clearSearchFilter = async () => {
      searchKeyword.value = ''
      
      // 更新URL参数
      const query = { ...route.query }
      delete query.search
      router.push({ path: route.path, query })
      
      // 设置搜索关键词为空
      blogStore.setSearch && blogStore.setSearch('')
      
      // 重新加载所有博客
      try {
        await fetchBlogs()
        ElMessage.success('已清除搜索筛选')
      } catch (error) {
        console.error('重新加载博客失败:', error)
        ElMessage.error('重新加载数据失败，请稍后重试')
      }
    }
    
    // 获取列表标题
    const getListTitle = () => {
      if (currentCategory.value) {
        return `分类: ${currentCategory.value.name}`
      }
      
      if (currentTag.value) {
        return `标签: ${currentTag.value.name}`
      }
      
      if (searchKeyword.value) {
        return `搜索: ${searchKeyword.value}`
      }
      
      return '全部博客'
    }
    
    // 计算总阅读量
    const getTotalViews = () => {
      return blogs.value.reduce((total, blog) => total + (blog.views || 0), 0)
    }
    
    // 获取标签样式，根据标签关联的博客数量设置不同的样式
    const getTagStyle = (tag) => {
      const count = tag.count || 0
      const maxFontSize = 18
      const minFontSize = 12
      
      // 根据标签数量计算字体大小
      let fontSize = Math.max(Math.min(Math.floor(count / 2) + minFontSize, maxFontSize), minFontSize)
      
      // 随机颜色
      const colors = [
        '#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399',
        '#8E44AD', '#16A085', '#2C3E50', '#F39C12', '#7F8C8D'
      ]
      const colorIndex = tag.id % colors.length
      
      return {
        fontSize: `${fontSize}px`,
        color: colors[colorIndex],
        fontWeight: count > 10 ? 'bold' : 'normal'
      }
    }
    
    // 格式化日期
    const formatDate = (dateArray) => {
      if (!dateArray) return '未知日期'
      
      try {
        // 尝试直接解析日期字符串
        const date = new Date(dateArray[0], dateArray[1] - 1, dateArray[2], dateArray[3], dateArray[4], dateArray[5])
  
        // 检查日期是否有效
        if (isNaN(date.getTime())) {
          console.error('无效的日期值:', dateArray)
          return '无效日期'
        }
        
        return date.toLocaleDateString('zh-CN', {
          year: 'numeric',
          month: 'long',
          day: 'numeric',
          hour: '2-digit',
          minute: '2-digit'
        })
      } catch (error) {
        console.error('日期格式化错误:', error, '日期值:', dateArray)
        return '无效日期'
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
      blogs,
      totalBlogs,
      loading,
      categories,
      categoriesLoading,
      recentBlogs,
      currentPage,
      pageSize,
      searchKeyword,
      sortBy,
      currentCategory,
      currentTag,
      tags,
      tagsLoading,
      currentUser,
      handleSearch,
      handleSortChange,
      handlePageChange,
      clearCategoryFilter,
      clearTagFilter,
      clearSearchFilter,
      getListTitle,
      getTotalViews,
      getTagStyle,
      formatDate,
      getImageUrl
    }
  }
}
</script>

<style scoped>
.home-container {
  padding-bottom: 40px;
}

/* 顶部横幅 */
.hero-banner {
  background: linear-gradient(135deg, #6a11cb 0%, #2575fc 100%);
  color: white;
  padding: 60px 20px;
  margin-bottom: 30px;
  border-radius: 0 0 10px 10px;
  text-align: center;
}

.banner-content {
  max-width: 800px;
  margin: 0 auto;
}

.banner-title {
  font-size: 2.5rem;
  margin: 0 0 10px;
  font-weight: 700;
}

.banner-description {
  font-size: 1.2rem;
  margin-bottom: 25px;
  opacity: 0.9;
}

.search-box {
  max-width: 600px;
  margin: 0 auto;
}

.search-box .el-input {
  font-size: 16px;
}

.search-box .el-input__inner {
  height: 50px;
  border-radius: 25px 0 0 25px;
  padding-left: 20px;
}

.search-box .el-input-group__append button {
  height: 50px;
  padding: 0 20px;
  border-radius: 0 25px 25px 0;
}

/* 主内容区域 */
.main-content {
  padding: 0 20px;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-group {
  /* 边框线 */
  border: 1px solid #eaeaea;
  border-radius: 8px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  background-color: #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.filter-group:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #dcdfe6;
}

.filter-label {
  margin-right: 10px;
  margin-left: 0;
  font-weight: 500;
  color: #606266;
  white-space: nowrap;
}

/* 排序选择框样式 */
.sort-select {
  min-width: 120px;
}

/* 选项内部的样式 */
.sort-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-option i {
  font-size: 16px;
  color: #409EFF;
}

.sort-option span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 全局样式，修改下拉框样式需要加 :deep() */
:deep(.el-select .el-input__wrapper) {
  padding: 0 10px;
  box-shadow: none !important;
  border: none;
  transition: all 0.3s ease;
}

:deep(.el-select .el-input__wrapper:hover) {
  background-color: #f5f7fa;
}

:deep(.el-select .el-select__tags) {
  flex-wrap: nowrap;
  overflow: visible;
}

/* 添加过渡动画 */
:deep(.el-select-dropdown__item) {
  padding: 8px 12px;
  transition: all 0.2s ease;
}

:deep(.el-select-dropdown__item.selected) {
  font-weight: 600;
  color: #409EFF;
  background-color: rgba(64, 158, 255, 0.1);
}

:deep(.el-select-dropdown__item:hover) {
  background-color: #f5f7fa;
}

.active-filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.active-filters .el-tag {
  margin-right: 0;
}

/* 博客列表 */
.blog-list-container {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

.blog-list-container h2 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 1.5rem;
  color: #333;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
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

.empty-blogs {
  padding: 30px 0;
}

.el-pagination {
  margin-top: 30px;
  text-align: center;
  grid-column: 1 / -1;
}

/* 侧边栏部件 */
.sidebar-widgets {
  position: sticky;
  top: 20px;
}

.widget {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

.widget h3 {
  margin-top: 0;
  margin-bottom: 15px;
  font-size: 1.2rem;
  color: #333;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

/* 作者信息 */
.author-info {
  text-align: center;
}

.author-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  margin: 0 auto 15px;
  border: 3px solid #f2f3f5;
}

.author-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.author-name {
  margin: 0 0 10px;
  border: none;
  padding: 0;
}

.author-bio {
  color: #606266;
  margin-bottom: 15px;
  font-size: 14px;
}

.author-stats {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.stat {
  text-align: center;
}

.stat-number {
  font-size: 1.4rem;
  font-weight: 600;
  color: #409EFF;
}

.stat-label {
  font-size: 0.9rem;
  color: #909399;
}

/* 分类列表 */
.category-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.category-list li {
  padding: 8px 0;
  border-bottom: 1px dashed #eee;
}

.category-list li:last-child {
  border-bottom: none;
}

.category-list a {
  color: #303133;
  text-decoration: none;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.category-list a:hover {
  color: #409EFF;
}

.category-count {
  background-color: #f2f3f5;
  color: #909399;
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 12px;
}

/* 标签云 */
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.tag-item {
  display: inline-block;
  padding: 4px 8px;
  text-decoration: none;
  border-radius: 4px;
  transition: all 0.3s ease;
  margin-bottom: 8px;
}

.tag-item:hover {
  background: rgba(64, 158, 255, 0.1);
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.tag-item small {
  opacity: 0.8;
  font-size: 0.8em;
  margin-left: 2px;
}

/* 最新文章列表 */
.recent-posts {
  list-style: none;
  padding: 0;
  margin: 0;
}

.recent-posts li {
  padding: 8px 0;
  border-bottom: 1px dashed #eee;
}

.recent-posts li:last-child {
  border-bottom: none;
}

.recent-posts a {
  color: #303133;
  text-decoration: none;
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 3px;
}

.recent-posts a:hover {
  color: #409EFF;
}

.post-date {
  font-size: 12px;
  color: #909399;
}

.empty-categories,
.empty-tags,
.empty-recent {
  padding: 10px 0;
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
  
  .hero-banner {
    padding: 40px 15px;
  }
  
  .banner-title {
    font-size: 2rem;
  }
  
  .filter-bar {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .filter-group {
    width: 100%;
    justify-content: space-between;
  }
  
  .sort-select {
    min-width: 150px;
    flex-grow: 1;
  }
  
  .sidebar-widgets {
    margin-top: 20px;
  }
  
  .tag-cloud {
    gap: 6px;
  }
  
  .tag-item {
    padding: 2px 6px;
    font-size: 12px !important;
  }
}
</style> 